/*
 * Copyright 2026 Lars Wilhelmsen <lars@lars-backwards.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

