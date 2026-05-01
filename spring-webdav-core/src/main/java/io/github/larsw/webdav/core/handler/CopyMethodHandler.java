package io.github.larsw.webdav.core.handler;

import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static io.github.larsw.webdav.core.xml.DavNamespaces.METHOD_COPY;

/** Handles COPY (RFC 4918, Section 9.8). */
public class CopyMethodHandler implements WebDavMethodHandler {

    private final WebDavResourceStore store;
    private final WebDavPropertyStore propertyStore;
    private final String davPrefix;

    public CopyMethodHandler(WebDavResourceStore store, WebDavPropertyStore propertyStore,
                              String davPrefix) {
        this.store = store;
        this.propertyStore = propertyStore;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return METHOD_COPY; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String src = DavPathUtils.extractDavPath(request, davPrefix);
        String dest = DavPathUtils.extractDestinationPath(
                request, request.getHeader("Destination"), davPrefix);

        if (dest == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Destination header");
            return;
        }
        if (store.getResource(src).isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean overwrite = !"F".equalsIgnoreCase(request.getHeader("Overwrite"));
        int depth = DavPathUtils.parseDepth(request.getHeader("Depth"), true);
        boolean destExisted = store.getResource(dest).isPresent();

        store.copy(src, dest, overwrite, depth != 0);
        // Copy dead properties too
        propertyStore.getProperties(src).forEach((qn, val) ->
                propertyStore.setProperties(dest, java.util.Map.of(qn, val)));

        response.setStatus(destExisted ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_CREATED);
    }
}

