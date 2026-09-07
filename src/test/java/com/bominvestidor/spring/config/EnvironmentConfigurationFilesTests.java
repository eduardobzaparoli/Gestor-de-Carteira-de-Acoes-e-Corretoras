package com.bominvestidor.spring.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertEquals("false", properties.getProperty("spring.flyway.enabled"));
        assertEquals("true", properties.getProperty("spring.h2.console.enabled"));

        String profile = Files.readString(
                PROJECT_ROOT.resolve("src/main/resources/application-h2.properties"),
                StandardCharsets.UTF_8);
        assertFalse(profile.contains("${DB_"));
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
        assertEquals("troque-por-seu-token", properties.getProperty("BRAPI_TOKEN"));
        assertEquals(7, properties.size());
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
}
