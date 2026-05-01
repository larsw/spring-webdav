package io.github.larsw.webdav.core.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles OPTIONS — advertises WebDAV Class 1+2 compliance plus all supported methods.
 * The {@code MS-Author-Via: DAV} header is required by the Windows WebDAV Mini-Redirector.
 */
public class OptionsMethodHandler implements WebDavMethodHandler {

    private static final String ALLOWED_METHODS =
            "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, COPY, MOVE, PROPFIND, PROPPATCH, LOCK, UNLOCK";

    @Override
    public String getMethod() { return "OPTIONS"; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("DAV", "1, 2");
        response.setHeader("MS-Author-Via", "DAV");
        response.setHeader("Allow", ALLOWED_METHODS);
        response.setHeader("Content-Length", "0");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}

