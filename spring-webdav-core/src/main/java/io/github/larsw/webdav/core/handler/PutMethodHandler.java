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

import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Handles PUT (RFC 4918 / HTTP 1.1) — creates or replaces a resource. */
public class PutMethodHandler implements WebDavMethodHandler {

    private final WebDavResourceStore store;
    private final WebDavLockManager lockManager;
    private final String davPrefix;

    public PutMethodHandler(WebDavResourceStore store, WebDavLockManager lockManager, String davPrefix) {
        this.store = store;
        this.lockManager = lockManager;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return "PUT"; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);

        // Check lock — if locked, the If header must supply the correct token
        if (lockManager.isLocked(path)) {
            String ifHeader = request.getHeader("If");
            String token = DavPathUtils.extractLockToken(ifHeader);
            if (token == null || lockManager.getActiveLock(path)
                    .filter(l -> token.equals(l.getLockToken())).isEmpty()) {
                response.sendError(423, "Locked");
                return;
            }
        }

        boolean existed = store.getResource(path).isPresent();

        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();

        store.putContent(path, request.getInputStream(), contentType, contentLength);

        response.setStatus(existed ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_CREATED);
    }
}

