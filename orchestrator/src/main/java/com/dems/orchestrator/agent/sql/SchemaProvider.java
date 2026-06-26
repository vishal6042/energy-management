package com.dems.orchestrator.agent.sql;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/** Loads the DB schema description (schema.sql) once, to feed the SQL-generation prompt. */
@Component
public class SchemaProvider {

    private final String schema;

    public SchemaProvider() {
        this.schema = load();
    }

    public String schema() {
        return schema;
    }

    private String load() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource("schema.sql").getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load schema.sql", e);
        }
    }
}
