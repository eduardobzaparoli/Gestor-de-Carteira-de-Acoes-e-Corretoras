package com.bominvestidor.spring.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

class UserStatusMigrationTest {

    @Test
    void createsTheCompleteSchemaInAnEmptyDatabase() throws Exception {
        String databaseUrl = databaseUrl("empty-schema");

        MigrateResult firstMigration = migrate(databaseUrl);
        assertEquals(1, firstMigration.migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            assertEquals(5, queryCount(statement, """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'PUBLIC'
                      AND table_name IN ('USERS', 'BROKERAGES', 'PORTFOLIOS',
                                         'PORTFOLIO_TRANSACTIONS', 'PORTFOLIO_INCOME_EVENTS')
                    """));
            assertEquals(1, queryCount(statement, """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'PUBLIC' AND table_name = 'USERS' AND column_name = 'STATUS'
                    """));
            assertEquals(1, queryCount(statement, """
                    SELECT COUNT(*)
                    FROM "flyway_schema_history"
                    WHERE "version" = '1'
                      AND "description" = 'initial schema'
                      AND "success" = TRUE
                      AND "checksum" IS NOT NULL
                    """));

            statement.execute("""
                    INSERT INTO users (id, name, email, password_hash, role, status, created_at, updated_at)
                    VALUES (RANDOM_UUID(), 'First', 'unique@example.com', 'hash', 'INVESTOR', 'ACTIVE',
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
            assertThrows(java.sql.SQLException.class, () -> statement.execute("""
                    INSERT INTO users (id, name, email, password_hash, role, status, created_at, updated_at)
                    VALUES (RANDOM_UUID(), 'Second', 'unique@example.com', 'hash', 'INVESTOR', 'ACTIVE',
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """));
            assertThrows(java.sql.SQLException.class, () -> statement.execute("""
                    INSERT INTO brokerages (id, owner_id, nickname, nickname_key, cnpj, legal_name,
                                            registration_status, cvm_participant_category, cep, street,
                                            neighborhood, number, city, state, created_at, updated_at)
                    VALUES (RANDOM_UUID(), RANDOM_UUID(), 'Broker', 'broker', '12345678000100', 'Broker Ltd',
                            'ACTIVE', 'BROKER', '01001000', 'Street', 'District', '1', 'City', 'SP',
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """));
        }

        MigrateResult secondMigration = migrate(databaseUrl);
        assertEquals(0, secondMigration.migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            assertEquals(1, queryCount(statement, "SELECT COUNT(*) FROM users"));
            assertEquals(2, queryCount(statement, "SELECT COUNT(*) FROM \"flyway_schema_history\""));
            assertEquals(1, queryCount(statement, """
                    SELECT COUNT(*) FROM "flyway_schema_history"
                    WHERE "type" = 'TABLE' AND "success" = TRUE
                    """));
        }
    }

    @Test
    void addsActiveStatusOnceToAnExistingUsersTable() throws Exception {
        String databaseUrl = databaseUrl("legacy-user-status");

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL)");
            statement.execute("INSERT INTO users (id, email) VALUES (RANDOM_UUID(), 'existing@example.com')");
        }

        MigrateResult firstMigration = migrate(databaseUrl);
        MigrateResult secondMigration = migrate(databaseUrl);
        assertEquals(1, firstMigration.migrationsExecuted);
        assertEquals(0, secondMigration.migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT status FROM users")) {
            resultSet.next();
            assertEquals("ACTIVE", resultSet.getString("status"));
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            assertEquals(1, queryCount(statement, """
                    SELECT COUNT(*) FROM "flyway_schema_history"
                    WHERE "type" = 'BASELINE' AND "version" = '0' AND "success" = TRUE
                    """));
            assertEquals(1, queryCount(statement, """
                    SELECT COUNT(*) FROM "flyway_schema_history"
                    WHERE "version" = '1' AND "description" = 'add user status'
                      AND "success" = TRUE AND "checksum" IS NOT NULL
                    """));
        }
    }

    private String databaseUrl(String name) {
        return "jdbc:h2:mem:" + name + "-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    }

    private MigrateResult migrate(String databaseUrl) {
        return Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private long queryCount(Statement statement, String query) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(query)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
