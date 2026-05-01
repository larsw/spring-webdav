package io.github.larsw.webdav.core.handler;

import io.github.larsw.webdav.core.spi.WebDavLockManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static io.github.larsw.webdav.core.xml.DavNamespaces.METHOD_UNLOCK;

/** Handles UNLOCK (RFC 4918, Section 9.11). */
public class UnlockMethodHandler implements WebDavMethodHandler {

    private final WebDavLockManager lockManager;
    private final String davPrefix;

    public UnlockMethodHandler(WebDavLockManager lockManager, String davPrefix) {
        this.lockManager = lockManager;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return METHOD_UNLOCK; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);
        String lockToken = DavPathUtils.extractLockToken(request.getHeader("Lock-Token"));

        if (lockToken == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Lock-Token header");
            return;
        }

        boolean removed = lockManager.unlock(path, lockToken);
        response.setStatus(removed
                ? HttpServletResponse.SC_NO_CONTENT   // 204 — success
                : HttpServletResponse.SC_CONFLICT);    // 409 — lock not held
    }
}

