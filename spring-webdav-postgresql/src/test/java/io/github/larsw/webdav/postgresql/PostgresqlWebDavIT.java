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

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end Testcontainers integration test for the PostgreSQL WebDAV store.
 *
 * <p>Spins up a real PostgreSQL container, seeds three tables (including a real {@code JSONB}
 * column), then exercises the full WebDAV HTTP stack via Spring MockMvc.
 *
 * <h2>Virtual layout under /dav/</h2>
 * <pre>
 * /dav/
 * ├── reports/                         ← CSV, hierarchical region path column
 * │   ├── europe/2024/Q1.csv  Q2.csv
 * │   ├── europe/2025/Q1.csv
 * │   ├── americas/2024/Q1.csv
 * │   └── summary.csv                  ← null pathColumn row → mapping root
 * ├── settings/                        ← JSON + JSONB column (PGobject handling)
 * │   ├── notifications/email.json  slack.json
 * │   └── infrastructure/database.json
 * └── products/                        ← flat JSON, no pathColumn
 *     ├── P001.json
 *     └── P002.json
 * </pre>
 */
@SpringBootTest(
        classes = PostgresqlWebDavTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.webdav.prefix=/dav",
                "spring.webdav.postgresql.enabled=true",

                // Mapping 0 — CSV with hierarchical pathColumn
                "spring.webdav.postgresql.mappings[0].name=reports",
                "spring.webdav.postgresql.mappings[0].table=monthly_reports",
                "spring.webdav.postgresql.mappings[0].path-column=region",
                "spring.webdav.postgresql.mappings[0].name-column=report_name",
                "spring.webdav.postgresql.mappings[0].format=CSV",

                // Mapping 1 — JSON with JSONB column
                "spring.webdav.postgresql.mappings[1].name=settings",
                "spring.webdav.postgresql.mappings[1].table=app_settings",
                "spring.webdav.postgresql.mappings[1].path-column=category",
                "spring.webdav.postgresql.mappings[1].name-column=setting_key",
                "spring.webdav.postgresql.mappings[1].format=JSON",
                "spring.webdav.postgresql.mappings[1].json-column=setting_value",

                // Mapping 2 — flat JSON, all columns as JSON object (no pathColumn)
                "spring.webdav.postgresql.mappings[2].name=products",
                "spring.webdav.postgresql.mappings[2].table=products",
                "spring.webdav.postgresql.mappings[2].name-column=product_id",
                "spring.webdav.postgresql.mappings[2].format=JSON"
        }
)
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PostgreSQL WebDAV — integration tests (Testcontainers)")
class PostgresqlWebDavIT {

    // ---- Container ----------------------------------------------------------
    // Note: PostgreSQLContainer is not generic in Testcontainers 2.x

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    // ---- Spring beans -------------------------------------------------------

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    // ---- Schema + seed data -------------------------------------------------

    @BeforeAll
    void createSchemaAndSeed() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS monthly_reports (
                    id          SERIAL PRIMARY KEY,
                    region      TEXT,
                    report_name TEXT NOT NULL,
                    revenue     NUMERIC(12,2),
                    units       INT,
                    updated_at  TIMESTAMPTZ DEFAULT now()
                )""");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    id            SERIAL PRIMARY KEY,
                    category      TEXT NOT NULL,
                    setting_key   TEXT NOT NULL,
                    setting_value JSONB,
                    created_at    TIMESTAMPTZ DEFAULT now()
                )""");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    product_id   TEXT PRIMARY KEY,
                    product_name TEXT,
                    price        NUMERIC(10,2),
                    in_stock     BOOLEAN
                )""");

        // monthly_reports
        jdbcTemplate.update("INSERT INTO monthly_reports(region,report_name,revenue,units) VALUES('europe/2024','Q1',150000,1200)");
        jdbcTemplate.update("INSERT INTO monthly_reports(region,report_name,revenue,units) VALUES('europe/2024','Q2',175000,1350)");
        jdbcTemplate.update("INSERT INTO monthly_reports(region,report_name,revenue,units) VALUES('europe/2025','Q1',200000,1500)");
        jdbcTemplate.update("INSERT INTO monthly_reports(region,report_name,revenue,units) VALUES('americas/2024','Q1',280000,2100)");
        jdbcTemplate.update("INSERT INTO monthly_reports(region,report_name,revenue,units) VALUES(null,'summary',805000,6150)");

        // app_settings with real JSONB — exercises PGobject extraction in PostgresqlResourceStore
        jdbcTemplate.update("INSERT INTO app_settings(category,setting_key,setting_value) VALUES('notifications','email','{\"enabled\":true,\"address\":\"admin@example.com\"}'::jsonb)");
        jdbcTemplate.update("INSERT INTO app_settings(category,setting_key,setting_value) VALUES('notifications','slack','{\"enabled\":false}'::jsonb)");
        jdbcTemplate.update("INSERT INTO app_settings(category,setting_key,setting_value) VALUES('infrastructure','database','{\"host\":\"db.example.com\",\"port\":5432}'::jsonb)");

        // products
        jdbcTemplate.update("INSERT INTO products VALUES('P001','Widget A',9.99,true)");
        jdbcTemplate.update("INSERT INTO products VALUES('P002','Gadget B',24.99,false)");
    }

    // =========================================================================
    // OPTIONS
    // =========================================================================

    @Nested
    @DisplayName("OPTIONS — capability advertisement")
    class OptionsTests {

        @Test
        @DisplayName("OPTIONS /dav/ returns 200 with DAV capability header")
        void optionsRoot() throws Exception {
            mockMvc.perform(options("/dav/"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("DAV", containsString("1")));
        }

        @Test
        @DisplayName("OPTIONS on a deep file path returns 200")
        void optionsFile() throws Exception {
            mockMvc.perform(options("/dav/reports/europe/2024/Q1.csv"))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // PROPFIND
    // =========================================================================

    @Nested
    @DisplayName("PROPFIND — DAV discovery")
    class PropFindTests {

        private static final String ALLPROP = """
                <?xml version="1.0" encoding="UTF-8"?>
                <D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>""";

        @Test
        @DisplayName("Depth:1 on root returns 207 listing all three mapping collections")
        void propfindRoot() throws Exception {
            propfind("/dav/", 1)
                    .andExpect(status().is(207))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(content().string(containsString("/dav/")))
                    .andExpect(content().string(containsString("reports")))
                    .andExpect(content().string(containsString("settings")))
                    .andExpect(content().string(containsString("products")));
        }

        @Test
        @DisplayName("Depth:0 on root returns only the root entry — no children")
        void propfindRootDepth0() throws Exception {
            propfind("/dav/", 0)
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("/dav/")))
                    .andExpect(content().string(not(containsString("reports"))));
        }

        @Test
        @DisplayName("Depth:1 on /dav/reports/ lists europe, americas + summary.csv")
        void propfindMappingRoot() throws Exception {
            propfind("/dav/reports/", 1)
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("europe")))
                    .andExpect(content().string(containsString("americas")))
                    .andExpect(content().string(containsString("summary.csv")));
        }

        @Test
        @DisplayName("Depth:1 on /dav/reports/europe/2024/ lists Q1.csv and Q2.csv")
        void propfindLeafFolder() throws Exception {
            propfind("/dav/reports/europe/2024/", 1)
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("Q1.csv")))
                    .andExpect(content().string(containsString("Q2.csv")));
        }

        @Test
        @DisplayName("Depth:0 on a CSV file returns 207 with text/csv in the contenttype property")
        void propfindCsvFile() throws Exception {
            propfind("/dav/reports/europe/2024/Q1.csv", 0)
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("Q1.csv")))
                    .andExpect(content().string(containsString("text/csv")));
        }

        @Test
        @DisplayName("Depth:0 on a JSON file includes application/json in the contenttype property")
        void propfindJsonFile() throws Exception {
            propfind("/dav/settings/notifications/email.json", 0)
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("email.json")))
                    .andExpect(content().string(containsString("application/json")));
        }

        @Test
        @DisplayName("PROPFIND on a non-existent path returns 404")
        void propfindNotFound() throws Exception {
            propfind("/dav/reports/europe/2024/Q9.csv", 0)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Depth:1 on /dav/settings/ lists notifications and infrastructure sub-collections")
        void propfindSettingsRoot() throws Exception {
            propfind("/dav/settings/", 1)
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("notifications")))
                    .andExpect(content().string(containsString("infrastructure")));
        }

        @Test
        @DisplayName("Depth:infinity traverses the entire /dav/reports/ sub-tree")
        void propfindInfinity() throws Exception {
            propfind("/dav/reports/", "infinity")
                    .andExpect(status().is(207))
                    .andExpect(content().string(containsString("2024")))
                    .andExpect(content().string(containsString("americas")))
                    .andExpect(content().string(containsString("Q1.csv")));
        }

        // ---- helpers --------------------------------------------------------

        private ResultActions propfind(String path, int depth) throws Exception {
            return propfind(path, String.valueOf(depth));
        }

        private ResultActions propfind(String path, String depth) throws Exception {
            return mockMvc.perform(
                    request("PROPFIND", URI.create(path))
                            .header("Depth", depth)
                            .contentType(MediaType.APPLICATION_XML)
                            .content(ALLPROP));
        }
    }

    // =========================================================================
    // GET — content retrieval
    // =========================================================================

    @Nested
    @DisplayName("GET — content retrieval")
    class GetTests {

        @Test
        @DisplayName("GET CSV file returns 200 with text/csv Content-Type")
        void getCsvContentType() throws Exception {
            mockMvc.perform(get("/dav/reports/europe/2024/Q1.csv"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"));
        }

        @Test
        @DisplayName("GET CSV body has exactly one header line and one data line (RFC 4180)")
        void getCsvBodyStructure() throws Exception {
            String body = getBodyAsString("/dav/reports/europe/2024/Q1.csv");
            String[] lines = body.split("\r\n");
            assertThat(lines).hasSize(2);
        }

        @Test
        @DisplayName("GET CSV header contains data columns but NOT pathColumn or nameColumn")
        void getCsvHeaderColumns() throws Exception {
            String[] lines = getBodyAsString("/dav/reports/europe/2024/Q1.csv").split("\r\n");
            assertThat(lines[0]).contains("revenue").contains("units");
            assertThat(lines[0]).doesNotContain("region").doesNotContain("report_name");
        }

        @Test
        @DisplayName("GET CSV data row contains the correct values for Q1/europe/2024")
        void getCsvDataValues() throws Exception {
            String[] lines = getBodyAsString("/dav/reports/europe/2024/Q1.csv").split("\r\n");
            assertThat(lines[1]).contains("150000");
            assertThat(lines[1]).contains("1200");
        }

        @Test
        @DisplayName("GET JSON file with JSONB column returns raw JSON (PGobject extraction)")
        void getJsonbColumn() throws Exception {
            String body = getBodyAsString("/dav/settings/notifications/email.json");
            assertThat(body).contains("\"enabled\"");
            assertThat(body).contains("admin@example.com");
            // metadata columns must not bleed into the output
            assertThat(body).doesNotContain("setting_key").doesNotContain("category");
        }

        @Test
        @DisplayName("GET JSON file returns application/json Content-Type")
        void getJsonContentType() throws Exception {
            mockMvc.perform(get("/dav/settings/notifications/email.json"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET flat JSON (no pathColumn) serialises non-metadata columns as a JSON object")
        void getFlatJson() throws Exception {
            String body = getBodyAsString("/dav/products/P001.json");
            assertThat(body).contains("Widget A");
            assertThat(body).doesNotContain("product_id"); // nameColumn excluded
        }

        @Test
        @DisplayName("GET root-level CSV file (null pathColumn row) returns 200")
        void getNullPathColumnFile() throws Exception {
            mockMvc.perform(get("/dav/reports/summary.csv"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"));
        }

        @Test
        @DisplayName("GET non-existent file returns 404")
        void getNotFound() throws Exception {
            mockMvc.perform(get("/dav/reports/europe/2024/Q9.csv"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET on a collection resource returns 200")
        void getCollection() throws Exception {
            mockMvc.perform(get("/dav/reports/"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ETag header is present and stable across repeated GETs of the same row")
        void eTagStability() throws Exception {
            String eTag1 = getHeader("/dav/reports/europe/2024/Q1.csv", "ETag");
            String eTag2 = getHeader("/dav/reports/europe/2024/Q1.csv", "ETag");
            assertThat(eTag1).isNotBlank().isEqualTo(eTag2);
        }

        @Test
        @DisplayName("Different rows produce different ETags")
        void differentRowsDifferentETags() throws Exception {
            String e1 = getHeader("/dav/reports/europe/2024/Q1.csv", "ETag");
            String e2 = getHeader("/dav/reports/europe/2024/Q2.csv", "ETag");
            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("Content-Length header matches the actual response body byte count")
        void contentLengthMatchesBody() throws Exception {
            var response = mockMvc.perform(get("/dav/reports/europe/2024/Q2.csv"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse();

            long declared = Long.parseLong(response.getHeader("Content-Length"));
            long actual   = response.getContentAsByteArray().length;
            assertThat(declared).isEqualTo(actual);
        }

        @Test
        @DisplayName("If-None-Match with the current ETag returns 304 Not Modified")
        void ifNoneMatch() throws Exception {
            String eTag = getHeader("/dav/reports/europe/2024/Q1.csv", "ETag");
            mockMvc.perform(get("/dav/reports/europe/2024/Q1.csv")
                            .header("If-None-Match", eTag))
                    .andExpect(status().isNotModified());
        }

        // ---- helpers --------------------------------------------------------

        private String getBodyAsString(String path) throws Exception {
            return mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }

        private String getHeader(String path, String header) throws Exception {
            return mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getHeader(header);
        }
    }

    // =========================================================================
    // HEAD — metadata without body
    // =========================================================================

    @Nested
    @DisplayName("HEAD — metadata without body")
    class HeadTests {

        @Test
        @DisplayName("HEAD CSV file returns 200 with ETag and Content-Length but no body bytes")
        void headCsvFile() throws Exception {
            var response = mockMvc.perform(head("/dav/reports/europe/2024/Q1.csv"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("ETag"))
                    .andExpect(header().exists("Content-Length"))
                    .andReturn().getResponse();

            assertThat(response.getContentAsByteArray()).isEmpty();
        }

        @Test
        @DisplayName("HEAD JSON file carries application/json Content-Type")
        void headJsonFile() throws Exception {
            mockMvc.perform(head("/dav/settings/notifications/email.json"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("application/json")));
        }
    }

    // =========================================================================
    // Write operations — all rejected (read-only store)
    // =========================================================================

    @Nested
    @DisplayName("Write operations — 405 Method Not Allowed")
    class WriteOperationsTests {

        @Test
        @DisplayName("PUT returns 405")
        void putRejected() throws Exception {
            mockMvc.perform(put("/dav/reports/europe/2024/new.csv")
                            .contentType("text/csv")
                            .content("id,val\r\n1,x\r\n"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE returns 405")
        void deleteRejected() throws Exception {
            mockMvc.perform(delete("/dav/reports/europe/2024/Q1.csv"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("MKCOL returns 405")
        void mkcolRejected() throws Exception {
            mockMvc.perform(request("MKCOL", URI.create("/dav/reports/new-folder")))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("COPY returns 405")
        void copyRejected() throws Exception {
            mockMvc.perform(request("COPY", URI.create("/dav/reports/europe/2024/Q1.csv"))
                            .header("Destination", "http://localhost/dav/reports/copy.csv"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("MOVE returns 405")
        void moveRejected() throws Exception {
            mockMvc.perform(request("MOVE", URI.create("/dav/reports/europe/2024/Q1.csv"))
                            .header("Destination", "http://localhost/dav/reports/moved.csv"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}

