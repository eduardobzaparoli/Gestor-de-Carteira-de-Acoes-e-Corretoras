package com.bominvestidor.spring.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.security.jwt.secret=test-only-secret-key-with-at-least-32-bytes",
        "app.security.jwt.expiration=PT15M"
})
@ActiveProfiles("h2")
class H2ProfileIntegrationTests {

    @Test
    void startsWithoutPostgresEnvironmentVariables() {
    }
}
