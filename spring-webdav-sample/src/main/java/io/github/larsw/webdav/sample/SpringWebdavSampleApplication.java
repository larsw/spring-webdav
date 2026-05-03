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

package io.github.larsw.webdav.sample;

import io.github.larsw.webdav.core.annotation.EnableWebDav;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Spring Boot application that exposes a local directory over WebDAV.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 *   ./mvnw spring-boot:run -pl spring-webdav-sample
 * }</pre>
 *
 * The server starts on port {@code 8080} and serves
 * {@code ~/webdav-root/} at {@code http://localhost:8080/dav/}.
 *
 * <h2>Connect from Windows</h2>
 * <ol>
 *   <li>Open "Map network drive"</li>
 *   <li>Enter {@code \\localhost@8080\DavWWWRoot\dav}</li>
 *   <li>Use any credentials (no authentication is configured in this sample)</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * See {@code application.yaml}. Key properties:
 * <pre>{@code
 * spring:
 *   webdav:
 *     prefix: /dav               # URL prefix for all WebDAV endpoints
 *     filesystem:
 *       root: /path/to/your/dir  # Directory to serve
 * }</pre>
 */
@SpringBootApplication
@EnableWebDav
public class SpringWebdavSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringWebdavSampleApplication.class, args);
    }
}

