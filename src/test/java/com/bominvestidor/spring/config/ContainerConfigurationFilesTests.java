package com.bominvestidor.spring.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContainerConfigurationFilesTests {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void composesHealthyServicesWithAnInternalPersistentDatabase() throws IOException {
        String compose = read("compose.yaml");
        String database = compose.substring(compose.indexOf("  database:"), compose.indexOf("  backend:"));

        assertTrue(compose.contains("  database:"));
        assertTrue(compose.contains("  backend:"));
        assertTrue(compose.contains("  frontend:"));
        assertTrue(compose.contains("postgres-data:/var/lib/postgresql/data"));
        assertTrue(database.contains("pg_isready"));
        assertFalse(database.contains("\n    ports:"));
        assertTrue(compose.contains("DB_PASSWORD: ${DB_PASSWORD:?"));
        assertTrue(compose.contains("JWT_SECRET: ${JWT_SECRET:?"));
        assertTrue(compose.contains("DB_URL: jdbc:postgresql://database:5432/"));
        assertTrue(compose.contains("condition: service_healthy"));
        assertTrue(compose.contains("VITE_API_BASE_URL: /"));
    }

    @Test
    void buildsBackendAndFrontendInSeparateNonPrivilegedRuntimeStages() throws IOException {
        String backend = read("Dockerfile");
        String frontend = read("frontend/Dockerfile");
        String nginx = read("frontend/nginx.conf");

        assertTrue(backend.contains("FROM maven:3.9.16-eclipse-temurin-17 AS build"));
        assertTrue(backend.contains("FROM eclipse-temurin:17-jre-jammy AS runtime"));
        assertTrue(backend.contains("USER app"));
        assertTrue(backend.contains("/actuator/health"));

        assertTrue(frontend.contains("FROM node:22.23.2-alpine AS build"));
        assertTrue(frontend.contains("FROM nginxinc/nginx-unprivileged:1-alpine AS runtime"));
        assertTrue(frontend.contains("ARG VITE_API_BASE_URL=/"));
        assertTrue(frontend.contains("HEALTHCHECK"));

        assertTrue(nginx.contains("resolver 127.0.0.11"));
        assertTrue(nginx.contains("set $backend_upstream http://backend:8080;"));
        assertTrue(nginx.contains("proxy_pass $backend_upstream$request_uri;"));
        assertTrue(nginx.contains("try_files $uri $uri/ /index.html;"));
    }

    @Test
    void excludesSecretsAndLocalArtifactsFromBuildContexts() throws IOException {
        String backendIgnore = read(".dockerignore");
        String frontendIgnore = read("frontend/.dockerignore");

        assertTrue(backendIgnore.contains(".env\n"));
        assertTrue(backendIgnore.contains(".env.*"));
        assertTrue(backendIgnore.contains("frontend"));
        assertTrue(backendIgnore.contains("graphify-out"));
        assertTrue(backendIgnore.contains("target"));

        assertTrue(frontendIgnore.contains(".env\n"));
        assertTrue(frontendIgnore.contains(".env.*"));
        assertTrue(frontendIgnore.contains("node_modules"));
        assertTrue(frontendIgnore.contains("dist"));
        assertTrue(frontendIgnore.contains("test-results"));
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }
}
