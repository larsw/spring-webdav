package io.github.larsw.webdav.postgresql;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used exclusively by the Testcontainers integration tests.
 *
 * <p>{@code @SpringBootApplication} triggers Spring Boot's auto-configuration scan, which
 * picks up both {@code WebDavAutoConfiguration} (via {@code spring-webdav-autoconfigure})
 * and {@code PostgresqlWebDavAutoConfiguration} (this module) when
 * {@code spring.webdav.postgresql.enabled=true} is set in the test properties.
 */
@SpringBootApplication
class PostgresqlWebDavTestApplication {
    // no main() needed — only used as the application class for @SpringBootTest
}

