package com.evoting.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies Spring context loads without errors.
 * Uses test profile to avoid requiring a real Supabase DB connection.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "evoting.jwt.secret=test-secret-key-must-be-at-least-32-characters-long-for-hmac-sha512",
    "evoting.cors.allowed-origins=http://localhost:8080",
    "spring.security.user.name=test",
    "spring.security.user.password=test"
})
class EVotingApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the Spring application context initializes correctly
    }
}
