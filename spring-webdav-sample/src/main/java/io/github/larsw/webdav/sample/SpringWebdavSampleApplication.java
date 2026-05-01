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

