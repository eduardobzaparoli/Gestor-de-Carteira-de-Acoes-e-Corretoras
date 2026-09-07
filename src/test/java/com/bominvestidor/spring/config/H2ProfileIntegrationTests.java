package com.bominvestidor.spring.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.security.jwt.secret=test-only-secret-key-with-at-least-32-bytes",
        "app.security.jwt.expiration=PT15M"
})
@ActiveProfiles("h2")
class H2ProfileIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startsWithTheCompleteFlywaySchemaWithoutPostgresEnvironmentVariables() {
        assertEquals(5, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'PUBLIC'
                  AND table_name IN ('USERS', 'BROKERAGES', 'PORTFOLIOS',
                                     'PORTFOLIO_TRANSACTIONS', 'PORTFOLIO_INCOME_EVENTS')
                """, Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE LOWER(table_name) = 'flyway_schema_history'
                """, Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM "flyway_schema_history"
                WHERE "version" = '1' AND "success" = TRUE
                """, Integer.class));
    }
}
