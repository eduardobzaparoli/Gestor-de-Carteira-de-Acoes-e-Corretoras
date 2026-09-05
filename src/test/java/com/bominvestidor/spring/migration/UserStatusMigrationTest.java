package com.bominvestidor.spring.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class UserStatusMigrationTest {

    @Test
    void addsActiveStatusToAnExistingUsersTable() throws Exception {
        String databaseUrl = "jdbc:h2:mem:user-status-migration-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL)");
            statement.execute("INSERT INTO users (id, email) VALUES (RANDOM_UUID(), 'existing@example.com')");
        }

        Flyway.configure()
                .dataSource(databaseUrl, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT status FROM users")) {
            resultSet.next();
            assertEquals("ACTIVE", resultSet.getString("status"));
        }
    }
}
