package io.github.larsw.webdav.core.handler;

import io.github.larsw.webdav.core.spi.WebDavResource;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Optional;

/** Handles GET and HEAD (RFC 4918 / HTTP 1.1) — retrieves resource content. */
public class GetMethodHandler implements WebDavMethodHandler {

    private static final DateTimeFormatter RFC1123 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                    .withZone(ZoneOffset.UTC)
                    .withLocale(java.util.Locale.US);

    private static final int BUFFER_SIZE = 8192;

    private final WebDavResourceStore store;
    private final String davPrefix;
    private final boolean headOnly;

    public GetMethodHandler(WebDavResourceStore store, String davPrefix, boolean headOnly) {
        this.store = store;
        this.davPrefix = davPrefix;
        this.headOnly = headOnly;
    }

    @Override
    public String getMethod() { return headOnly ? "HEAD" : "GET"; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);
        Optional<WebDavResource> opt = store.getResource(path);

        if (opt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        WebDavResource resource = opt.get();

        if (resource.isCollection()) {
            // Collections: return 200 with empty body (some clients probe with GET)
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("DAV", "1, 2");
            return;
        }

        // ETag check (If-None-Match)
        String etag = "\"" + resource.getETag() + "\"";
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        if (resource.getContentType() != null) {
            response.setContentType(resource.getContentType());
        }
        if (resource.getContentLength() >= 0) {
            response.setHeader("Content-Length", String.valueOf(resource.getContentLength()));
        }
        response.setHeader("ETag", etag);
        response.setHeader("Last-Modified", RFC1123.format(resource.getLastModified()));
        response.setHeader("DAV", "1, 2");

        if (!headOnly) {
            try (InputStream in = store.getContent(path);
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}

