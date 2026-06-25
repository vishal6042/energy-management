package com.dems.orchestrator.sql;

import com.dems.orchestrator.agent.Card;
import com.dems.orchestrator.assistant.dto.ChatMessage;
import com.dems.orchestrator.client.OllamaClient;
import com.dems.orchestrator.config.OrchestratorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Text-to-SQL data agent: the LLM writes a PostgreSQL SELECT from the schema, the
 * query is validated (SELECT-only) and executed against a read-only connection,
 * and the LLM then synthesizes a concise answer from the rows.
 */
@Service
public class SqlAgent {

    private static final Logger log = LoggerFactory.getLogger(SqlAgent.class);

    public record SqlResult(String answer, Card card) {}

    private final OllamaClient ollama;
    private final SchemaProvider schemaProvider;
    private final SqlValidator validator;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final int maxRows;

    public SqlAgent(OllamaClient ollama, SchemaProvider schemaProvider, SqlValidator validator,
                    ObjectMapper mapper, DataSource dataSource, OrchestratorProperties props) {
        this.ollama = ollama;
        this.schemaProvider = schemaProvider;
        this.validator = validator;
        this.mapper = mapper;
        this.maxRows = props.sql().maxRows();
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setMaxRows(props.sql().maxRows());
        this.jdbc.setQueryTimeout(props.sql().timeoutSeconds());
    }

    public SqlResult run(List<ChatMessage> history) {
        String rawSql = generateSql(history);
        String sql;
        try {
            sql = validator.validate(rawSql, maxRows);
        } catch (SqlValidator.InvalidSqlException e) {
            log.warn("Rejected generated SQL [{}]: {}", rawSql, e.getMessage());
            return new SqlResult(
                    "I couldn't run that as a safe read-only query. Could you rephrase the question?",
                    null);
        }

        List<Map<String, Object>> rows;
        try {
            rows = jdbc.query(sql, new ColumnMapRowMapper());
        } catch (Exception e) {
            log.warn("SQL execution failed [{}]: {}", sql, e.getMessage());
            return new SqlResult("That query couldn't be executed against the database.", null);
        }

        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
        Card card = new Card("query_result", Map.of("sql", sql, "columns", columns, "rows", rows));
        String answer = synthesize(lastUser(history), sql, rows);
        return new SqlResult(answer, card);
    }

    private String generateSql(List<ChatMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", """
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

                """ + schemaProvider.schema()));
        // Include recent turns so references like "them"/"that device" resolve.
        for (ChatMessage m : recent(history)) {
            messages.add(Map.of("role", m.role(), "content", m.content() == null ? "" : m.content()));
        }
        String sql = ollama.chat(messages, List.of()).content();
        return stripFences(sql);
    }

    private String synthesize(String question, String sql, List<Map<String, Object>> rows) {
        String rowsJson = toJson(rows.size() > 50 ? rows.subList(0, 50) : rows);
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content",
                        "Answer the user's question concisely and clearly using ONLY the query result "
                                + "below. Do not mention SQL or databases. Use Markdown when helpful.\n"
                                + "Question: " + question + "\nResult JSON: " + rowsJson),
                Map.of("role", "user", "content", question));
        return ollama.chat(messages, List.of()).content();
    }

    private List<ChatMessage> recent(List<ChatMessage> history) {
        int n = history.size();
        return n > 6 ? history.subList(n - 6, n) : history;
    }

    private String lastUser(List<ChatMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).role())) {
                return history.get(i).content();
            }
        }
        return "";
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
