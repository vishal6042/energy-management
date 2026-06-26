package com.dems.orchestrator.agent.sql;

import com.dems.orchestrator.agent.Agent;
import com.dems.orchestrator.agent.AgentRequest;
import com.dems.orchestrator.agent.AgentResult;
import com.dems.orchestrator.agent.SpringAiMessages;
import com.dems.orchestrator.assistant.dto.Card;
import com.dems.orchestrator.assistant.dto.ChatMessage;
import com.dems.orchestrator.config.OrchestratorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Text-to-SQL data agent: the LLM writes a PostgreSQL SELECT from the schema, the query is validated
 * (SELECT-only) and executed against a read-only connection, then the LLM synthesizes an answer.
 */
@Component
public class SqlAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SqlAgent.class);

    private static final String SQL_SYSTEM = """
            You translate the user's question into exactly ONE read-only PostgreSQL SELECT
            for the schema below. Output ONLY the SQL — no markdown, no explanation, no semicolon.
            Use the exact table/column names. Enum values are UPPERCASE. Join saving_records to
            devices on saving_records.device_id = devices.id when filtering by device name.

            Dates: the period column is TEXT ('YYYY-MM-DD' for DAILY, 'YYYY-MM' for MONTHLY).
            Never assume today's date — use CURRENT_DATE and cast with period::date for date math.
            For a "last N days" request you MUST filter daily rows, e.g.:
              WHERE sr.granularity = 'DAILY' AND sr.period::date >= CURRENT_DATE - INTERVAL '<N-1> days'
            Example — last 7 days of usage per device:
              SELECT d.name, sr.period, sr.usage_kwh, sr.usage_cost
              FROM saving_records sr JOIN devices d ON d.id = sr.device_id
              WHERE sr.granularity = 'DAILY' AND sr.period::date >= CURRENT_DATE - INTERVAL '6 days'
              ORDER BY sr.period DESC
            For months use sr.granularity = 'MONTHLY'.

            """;

    private final ChatClient chatClient;
    private final SchemaProvider schemaProvider;
    private final SqlValidator validator;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final int maxRows;

    public SqlAgent(ChatClient chatClient, SchemaProvider schemaProvider, SqlValidator validator,
                    ObjectMapper mapper, DataSource dataSource, OrchestratorProperties props) {
        this.chatClient = chatClient;
        this.schemaProvider = schemaProvider;
        this.validator = validator;
        this.mapper = mapper;
        this.maxRows = props.sql().maxRows();
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setMaxRows(props.sql().maxRows());
        this.jdbc.setQueryTimeout(props.sql().timeoutSeconds());
    }

    @Override
    public String id() {
        return "sql";
    }

    @Override
    public String displayName() {
        return "SQL Agent";
    }

    @Override
    public String routingDescription() {
        return "questions answerable from the database: device lists/counts/status, energy usage, "
                + "savings, costs, history, comparisons, analytics.";
    }

    @Override
    public AgentResult handle(AgentRequest request) {
        String rawSql = generateSql(request.history());
        String sql;
        try {
            sql = validator.validate(rawSql, maxRows);
        } catch (SqlValidator.InvalidSqlException e) {
            log.warn("Rejected generated SQL [{}]: {}", rawSql, e.getMessage());
            return AgentResult.message(
                    "I couldn't run that as a safe read-only query. Could you rephrase the question?");
        }

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.query(sql, new ColumnMapRowMapper());
        } catch (Exception e) {
            log.warn("SQL execution failed [{}]: {}", sql, e.getMessage());
            return AgentResult.message("That query couldn't be executed against the database.");
        }

        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
        Card card = new Card("query_result", Map.of("sql", sql, "columns", columns, "rows", rows));
        String answer = synthesize(request.lastUser(), rows);
        return AgentResult.message(answer, List.of(), List.of(card));
    }

    private String generateSql(List<ChatMessage> history) {
        String sql = chatClient.prompt()
                .system(SQL_SYSTEM + schemaProvider.schema())
                .messages(SpringAiMessages.from(recent(history)))
                .call()
                .content();
        return stripFences(sql);
    }

    private String synthesize(String question, List<Map<String, Object>> rows) {
        String rowsJson = toJson(rows.size() > 50 ? rows.subList(0, 50) : rows);
        return chatClient.prompt()
                .system("Answer the user's question concisely and clearly using ONLY the query result "
                        + "below. Do not mention SQL or databases. Use Markdown when helpful.\n"
                        + "Result JSON: " + rowsJson)
                .user(question)
                .call()
                .content();
    }

    private List<ChatMessage> recent(List<ChatMessage> history) {
        int n = history.size();
        return n > 6 ? history.subList(n - 6, n) : history;
    }

    private String stripFences(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        if (t.startsWith("```")) {
            t = t.replaceAll("(?s)```[a-zA-Z]*\\s*", "").replace("```", "").strip();
        }
        return t;
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (RuntimeException e) {
            return String.valueOf(o);
        }
    }
}
