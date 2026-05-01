package io.github.larsw.webdav.core.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles a single WebDAV HTTP method (PROPFIND, LOCK, PUT, etc.).
 *
 * <p>Beans implementing this interface are auto-discovered by
 * {@link WebDavHttpRequestHandler} and dispatched to by method name.
 * Override any built-in handler by declaring a bean with the same method name.
 */
public interface WebDavMethodHandler {

    /** Returns the HTTP method name this handler handles (e.g. {@code "PROPFIND"}). */
    String getMethod();

    void handle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;
}

