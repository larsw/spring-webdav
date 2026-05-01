package io.github.larsw.webdav.core.web;

import io.github.larsw.webdav.core.handler.WebDavHttpRequestHandler;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Map;

/**
 * Registers the {@link WebDavHttpRequestHandler} at the configured DAV path prefix.
 *
 * <p>Mapped with the highest priority ({@link Ordered#HIGHEST_PRECEDENCE}) so that WebDAV
 * paths take precedence over any application-level mappings.
 */
public class WebDavHandlerMapping extends SimpleUrlHandlerMapping {

    public WebDavHandlerMapping(WebDavHttpRequestHandler handler, String davPrefix) {
        String pattern = davPrefix.endsWith("/")
                ? davPrefix + "**"
                : davPrefix + "/**";
        // Also handle the root of the prefix itself
        setUrlMap(Map.of(
                pattern, handler,
                davPrefix, handler
        ));
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}

