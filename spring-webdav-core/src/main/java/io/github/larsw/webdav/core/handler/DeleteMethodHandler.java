package io.github.larsw.webdav.core.handler;

import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Handles DELETE (RFC 4918, Section 9.6) — removes a resource or collection recursively. */
public class DeleteMethodHandler implements WebDavMethodHandler {

    private final WebDavResourceStore store;
    private final WebDavLockManager lockManager;
    private final WebDavPropertyStore propertyStore;
    private final String davPrefix;

    public DeleteMethodHandler(WebDavResourceStore store, WebDavLockManager lockManager,
                                WebDavPropertyStore propertyStore, String davPrefix) {
        this.store = store;
        this.lockManager = lockManager;
        this.propertyStore = propertyStore;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return "DELETE"; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);

        if (store.getResource(path).isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (lockManager.isLocked(path)) {
            String token = DavPathUtils.extractLockToken(request.getHeader("If"));
            if (token == null || lockManager.getActiveLock(path)
                    .filter(l -> token.equals(l.getLockToken())).isEmpty()) {
                response.sendError(423, "Locked");
                return;
            }
        }

        store.delete(path);
        propertyStore.onDelete(path);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}

