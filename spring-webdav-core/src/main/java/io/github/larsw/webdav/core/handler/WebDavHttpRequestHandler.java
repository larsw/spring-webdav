package io.github.larsw.webdav.core.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central {@link HttpRequestHandler} that dispatches all WebDAV requests to the
 * appropriate {@link WebDavMethodHandler} based on the HTTP method name.
 *
 * <p>Registered with Spring MVC via {@link io.github.larsw.webdav.core.web.WebDavHandlerMapping}.
 */
public class WebDavHttpRequestHandler implements HttpRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(WebDavHttpRequestHandler.class);

    private final Map<String, WebDavMethodHandler> handlers;

    public WebDavHttpRequestHandler(List<WebDavMethodHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(
                        h -> h.getMethod().toUpperCase(),
                        Function.identity(),
                        (a, b) -> { log.warn("Duplicate WebDavMethodHandler for method {}; using {}", a.getMethod(), b.getClass()); return b; }
                ));
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();
        WebDavMethodHandler handler = handlers.get(method);

        if (handler == null) {
            log.debug("No WebDavMethodHandler registered for method: {}", method);
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed: " + method);
            return;
        }

        try {
            handler.handle(request, response);
        } catch (io.github.larsw.webdav.core.spi.WebDavException ex) {
            log.debug("WebDavException for {} {}: {} {}", method, request.getRequestURI(),
                    ex.getStatusCode(), ex.getMessage());
            if (!response.isCommitted()) {
                response.sendError(ex.getStatusCode(), ex.getMessage());
            }
        }
    }
}

