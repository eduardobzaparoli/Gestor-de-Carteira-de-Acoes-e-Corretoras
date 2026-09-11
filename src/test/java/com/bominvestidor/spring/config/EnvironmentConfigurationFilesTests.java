package com.bominvestidor.spring.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class EnvironmentConfigurationFilesTests {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void configuresDevWithExternalizedPostgresCredentials() throws IOException {
        Properties properties = loadProperties("src/main/resources/application-dev.properties");

        assertEquals("${DB_URL:jdbc:postgresql://localhost:5432/bominvestidor}",
                properties.getProperty("spring.datasource.url"));
        assertEquals("${DB_USERNAME:postgres}", properties.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertEquals("org.postgresql.Driver", properties.getProperty("spring.datasource.driver-class-name"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.flyway.enabled"));
        assertEquals("false", properties.getProperty("spring.h2.console.enabled"));
    }

    @Test
    void keepsH2ProfileSelfContained() throws IOException {
        Properties properties = loadProperties("src/main/resources/application-h2.properties");

        assertTrue(properties.getProperty("spring.datasource.url").startsWith("jdbc:h2:mem:"));
        assertEquals("sa", properties.getProperty("spring.datasource.username"));
        assertEquals("", properties.getProperty("spring.datasource.password"));
        assertEquals("org.h2.Driver", properties.getProperty("spring.datasource.driver-class-name"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.flyway.enabled"));
        assertEquals("true", properties.getProperty("spring.h2.console.enabled"));

        String profile = Files.readString(
                PROJECT_ROOT.resolve("src/main/resources/application-h2.properties"),
                StandardCharsets.UTF_8);
        assertFalse(profile.contains("${DB_"));
    }

    @Test
    void configuresTestsToUseTheSharedFlywayMigrations() throws IOException {
        Properties defaults = loadProperties("src/main/resources/application.properties");
        Properties properties = loadProperties("src/test/resources/application-test.properties");

        assertEquals("classpath:db/migration", defaults.getProperty("spring.flyway.locations"));
        assertTrue(properties.getProperty("spring.datasource.url").contains("MODE=PostgreSQL"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.flyway.enabled"));
    }

    @Test
    void preservesPublishedMigrationFiles() throws IOException, NoSuchAlgorithmException {
        assertEquals("dbee139f755c91ee82168bb94250444707b697c3ce9a6c364498e476c8258909",
                sha256("src/main/resources/db/migration/B1__initial_schema.sql"));
        assertEquals("237d1933efd9073b1d5ccec96e460f0786c452249198e121d7f979d668c482db",
                sha256("src/main/resources/db/migration/V1__add_user_status.sql"));
    }

    @Test
    void publishesOnlyFictitiousValuesInEnvironmentExample() throws IOException {
        Properties properties = loadProperties(".env.example");

        assertEquals("dev", properties.getProperty("SPRING_PROFILES_ACTIVE"));
        assertEquals("jdbc:postgresql://localhost:5432/bominvestidor", properties.getProperty("DB_URL"));
        assertEquals("postgres", properties.getProperty("DB_USERNAME"));
        assertEquals("troque-esta-senha", properties.getProperty("DB_PASSWORD"));

        String jwtSecret = properties.getProperty("JWT_SECRET");
        assertTrue(jwtSecret.startsWith("troque-por-"));
        assertTrue(jwtSecret.length() >= 32);
        assertEquals("troque-por-sua-chave", properties.getProperty("ALPHA_VANTAGE_API_KEY"));
        assertEquals("troque-por-sua-chave-twelve-data", properties.getProperty("TWELVE_DATA_API_KEY"));
        assertEquals("troque-por-seu-token", properties.getProperty("BRAPI_TOKEN"));
        assertEquals("http://localhost:5173", properties.getProperty("CORS_ALLOWED_ORIGINS"));
        assertEquals(9, properties.size());
    }

    @Test
    void externalizesIndependentMarketProviderConfiguration() throws IOException {
        Properties properties = loadProperties("src/main/resources/application.properties");

        assertEquals("${TWELVE_DATA_BASE_URL:https://api.twelvedata.com}",
                properties.getProperty("app.integrations.twelve-data-base-url"));
        assertEquals("${TWELVE_DATA_API_KEY:}",
                properties.getProperty("app.integrations.twelve-data-api-key"));
        assertEquals("${ALPHA_VANTAGE_BASE_URL:https://www.alphavantage.co}",
                properties.getProperty("app.integrations.alpha-vantage-base-url"));
        assertEquals("${ALPHA_VANTAGE_API_KEY:}",
                properties.getProperty("app.integrations.alpha-vantage-api-key"));
    }

    @Test
    void ignoresLocalEnvironmentFilesButKeepsExampleVersionable() throws IOException {
        List<String> lines = Files.readAllLines(PROJECT_ROOT.resolve(".gitignore"), StandardCharsets.UTF_8);

        assertTrue(lines.contains(".env"));
        assertTrue(lines.contains(".env.*"));
        assertTrue(lines.contains("!.env.example"));
        assertTrue(Files.isRegularFile(PROJECT_ROOT.resolve(".env.example")));
    }

    private Properties loadProperties(String relativePath) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(PROJECT_ROOT.resolve(relativePath), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private String sha256(String relativePath) throws IOException, NoSuchAlgorithmException {
        byte[] content = Files.readAllBytes(PROJECT_ROOT.resolve(relativePath));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
