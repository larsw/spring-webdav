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

package io.github.larsw.webdav.postgresql.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import io.github.larsw.webdav.postgresql.PostgresqlResourceStore;
import io.github.larsw.webdav.postgresql.TableMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot auto-configuration for {@link PostgresqlResourceStore}.
 *
 * <h2>Activation conditions</h2>
 * <ol>
 *   <li>{@code spring.webdav.postgresql.enabled=true} is set</li>
 *   <li>{@link JdbcTemplate} is on the classpath (usually via
 *       {@code spring-boot-starter-jdbc})</li>
 *   <li>No other {@link WebDavResourceStore} bean is already registered</li>
 * </ol>
 *
 * <h2>Minimal configuration example</h2>
 * <pre>{@code
 * spring:
 *   webdav:
 *     postgresql:
 *       enabled: true
 *       mappings:
 *         - name: reports
 *           table: monthly_reports
 *           path-column: region        # e.g. "europe/2024"
 *           name-column: report_name   # e.g. "revenue"  → /reports/europe/2024/revenue.csv
 *           format: CSV
 *
 *         - name: configs
 *           table: app_settings
 *           name-column: setting_key
 *           format: JSON
 *           json-column: setting_value  # a jsonb column
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(prefix = "spring.webdav.postgresql", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PostgresqlWebDavAutoConfiguration.PostgresqlWebDavProperties.class)
public class PostgresqlWebDavAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebDavResourceStore.class)
    public PostgresqlResourceStore postgresqlResourceStore(
            JdbcTemplate jdbcTemplate,
            PostgresqlWebDavProperties props,
            ObjectProvider<ObjectMapper> objectMapperProvider) {

        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new PostgresqlResourceStore(jdbcTemplate, props.getMappings(), mapper);
    }

    // ---- Configuration properties -------------------------------------------

    /**
     * Top-level configuration properties bound to {@code spring.webdav.postgresql}.
     */
    @ConfigurationProperties(prefix = "spring.webdav.postgresql")
    public static class PostgresqlWebDavProperties {

        /**
         * Set to {@code true} to enable the PostgreSQL WebDAV resource store.
         */
        private boolean enabled = false;

        /**
         * One entry per virtual directory tree to expose over WebDAV.
         * Each entry maps a PostgreSQL table/query to a root collection name.
         */
        private List<TableMapping> mappings = new ArrayList<>();

        public boolean isEnabled()                       { return enabled; }
        public void setEnabled(boolean enabled)          { this.enabled = enabled; }

        public List<TableMapping> getMappings()          { return mappings; }
        public void setMappings(List<TableMapping> m)    { this.mappings = m; }
    }
}

