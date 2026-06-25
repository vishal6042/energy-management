package com.dems.orchestrator.sql;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Enforces the "SQL whitelist": only a single read-only SELECT is allowed. This is
 * the application-level guard; the connection pool is also read-only as defense in depth.
 */
@Component
public class SqlValidator {

    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|REVOKE|MERGE|COPY|CALL|DO|VACUUM|EXEC|EXECUTE)\\b",
            Pattern.CASE_INSENSITIVE);

    public static class InvalidSqlException extends RuntimeException {
        public InvalidSqlException(String message) {
            super(message);
        }
    }

    /** Returns a sanitized, row-limited SELECT or throws {@link InvalidSqlException}. */
    public String validate(String rawSql, int maxRows) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new InvalidSqlException("Empty SQL.");
        }
        String sql = rawSql.strip();
        // Drop a single trailing semicolon; reject any internal ones (no statement chaining).
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).strip();
        }
        if (sql.contains(";")) {
            throw new InvalidSqlException("Multiple statements are not allowed.");
        }
        String upper = sql.toUpperCase();
        if (!(upper.startsWith("SELECT") || upper.startsWith("WITH"))) {
            throw new InvalidSqlException("Only SELECT queries are allowed.");
        }
        if (FORBIDDEN.matcher(sql).find()) {
            throw new InvalidSqlException("Query contains a forbidden (write/DDL) keyword.");
        }
        // Cap rows if the model didn't add a LIMIT.
        if (!Pattern.compile("\\bLIMIT\\b", Pattern.CASE_INSENSITIVE).matcher(sql).find()) {
            sql = sql + " LIMIT " + maxRows;
        }
        return sql;
    }
}
