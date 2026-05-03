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

import io.github.larsw.webdav.core.spi.WebDavException;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static io.github.larsw.webdav.core.xml.DavNamespaces.METHOD_MKCOL;

/** Handles MKCOL (RFC 4918, Section 9.3) — creates a new collection. */
public class MkColMethodHandler implements WebDavMethodHandler {

    private final WebDavResourceStore store;
    private final String davPrefix;

    public MkColMethodHandler(WebDavResourceStore store, String davPrefix) {
        this.store = store;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return METHOD_MKCOL; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // RFC 4918: MKCOL with a body MUST return 415 Unsupported Media Type
        if (request.getContentLength() > 0
                || (request.getContentLength() == -1
                && request.getInputStream().available() > 0)) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "MKCOL request body not supported");
            return;
        }

        String path = DavPathUtils.extractDavPath(request, davPrefix);
        store.createCollection(path);
        response.setStatus(HttpServletResponse.SC_CREATED);
    }
}

