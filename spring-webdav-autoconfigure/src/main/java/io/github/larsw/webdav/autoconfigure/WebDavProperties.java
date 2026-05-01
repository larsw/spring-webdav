package io.github.larsw.webdav.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Spring WebDAV framework.
 *
 * <pre>{@code
 * spring:
 *   webdav:
 *     prefix: /dav
 * }</pre>
 */
@ConfigurationProperties(prefix = "spring.webdav")
public class WebDavProperties {

    /**
     * URL prefix under which all WebDAV endpoints are served.
     * Defaults to root ({@code ""}) which makes the entire server a WebDAV endpoint.
     */
    private String prefix = "";

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
}

