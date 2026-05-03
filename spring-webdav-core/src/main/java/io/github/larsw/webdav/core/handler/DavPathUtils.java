/*
 * Copyright 2026 Lars Wilhelmsen <lars@lars-backwards.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.larsw.webdav.core.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * Shared path extraction utilities for WebDAV method handlers.
 */
public final class DavPathUtils {

    /**
     * Extracts the server-relative WebDAV resource path from the request,
     * stripping the servlet context path and the configured DAV mapping prefix.
     *
     * <p>Example: context={@code ""}, prefix={@code "/dav"}, requestURI={@code "/dav/docs/file.txt"}
     * → returns {@code "/docs/file.txt"}.
     */
    public static String extractDavPath(HttpServletRequest request, String davPrefix) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // Strip context path
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }

        // URL-decode
        uri = UriUtils.decode(uri, StandardCharsets.UTF_8);

        // Strip DAV prefix
        if (davPrefix != null && !davPrefix.isEmpty() && uri.startsWith(davPrefix)) {
            uri = uri.substring(davPrefix.length());
        }

        if (uri.isEmpty()) uri = "/";
        return uri;
    }

    /**
     * Extracts the server-relative path from a full {@code Destination} header URL,
     * stripping the scheme, host, context path and DAV prefix.
     */
    public static String extractDestinationPath(HttpServletRequest request,
                                                 String destinationHeader,
                                                 String davPrefix) {
        if (destinationHeader == null || destinationHeader.isBlank()) return null;
        try {
            URI uri = new URI(destinationHeader);
            String path = uri.getPath();
            if (path == null) return null;

            path = UriUtils.decode(path, StandardCharsets.UTF_8);

            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            if (davPrefix != null && !davPrefix.isEmpty() && path.startsWith(davPrefix)) {
                path = path.substring(davPrefix.length());
            }
            if (path.isEmpty()) path = "/";
            return path;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Extracts the first lock token from an {@code If} or {@code Lock-Token} header.
     * Handles both {@code (<urn:uuid:…>)} and {@code Lock-Token: <urn:uuid:…>} formats.
     */
    public static String extractLockToken(String headerValue) {
        if (headerValue == null) return null;
        int start = headerValue.indexOf('<');
        int end = headerValue.indexOf('>');
        if (start >= 0 && end > start) {
            return headerValue.substring(start + 1, end);
        }
        return null;
    }

    /**
     * Returns the Depth header value as an integer:
     * 0, 1, or -1 for Infinity (also the default when the header is absent and
     * {@code defaultInfinity=true}).
     */
    public static int parseDepth(String depthHeader, boolean defaultInfinity) {
        if (depthHeader == null) return defaultInfinity ? -1 : 0;
        return switch (depthHeader.trim()) {
            case "0" -> 0;
            case "1" -> 1;
            case "infinity", "Infinity" -> -1;
            default -> defaultInfinity ? -1 : 0;
        };
    }

    private DavPathUtils() {}
}

