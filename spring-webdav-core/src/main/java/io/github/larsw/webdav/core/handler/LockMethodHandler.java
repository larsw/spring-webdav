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

import io.github.larsw.webdav.core.model.LockInfo;
import io.github.larsw.webdav.core.model.LockResult;
import io.github.larsw.webdav.core.spi.WebDavException;
import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import io.github.larsw.webdav.core.xml.DavXmlUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static io.github.larsw.webdav.core.xml.DavNamespaces.METHOD_LOCK;

/**
 * Handles LOCK (RFC 4918, Section 9.10).
 *
 * <p>Supports both "lock acquisition" (body present) and "lock refresh" (no body, If header present).
 * If the resource does not exist, it is created as a zero-byte file (required by RFC 4918).
 */
public class LockMethodHandler implements WebDavMethodHandler {

    private final WebDavResourceStore store;
    private final WebDavLockManager lockManager;
    private final String davPrefix;

    public LockMethodHandler(WebDavResourceStore store, WebDavLockManager lockManager, String davPrefix) {
        this.store = store;
        this.lockManager = lockManager;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return METHOD_LOCK; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);
        int depth = DavPathUtils.parseDepth(request.getHeader("Depth"), false);
        long timeout = parseTimeout(request.getHeader("Timeout"));

        String ifHeader = request.getHeader("If");
        boolean isRefresh = (request.getContentLength() == 0 || request.getContentLength() == -1)
                && ifHeader != null;

        LockResult result;
        boolean resourceCreated = false;

        if (isRefresh) {
            String token = DavPathUtils.extractLockToken(ifHeader);
            result = lockManager.refreshLock(path, token, timeout);
        } else {
            LockInfo lockInfo = DavXmlUtils.parseLockInfo(request.getInputStream());
            lockInfo.setPath(path);
            lockInfo.setDepth(depth);
            lockInfo.setTimeoutSeconds(timeout);

            resourceCreated = store.getResource(path).isEmpty();
            if (resourceCreated) {
                // Create a zero-byte placeholder (RFC 4918 §9.10.4)
                store.putContent(path, InputStream.nullInputStream(), null, 0);
            }
            result = lockManager.lock(path, lockInfo);
        }

        response.setStatus(resourceCreated ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_OK);

        LockInfo granted = result.getLockInfo();
        response.setContentType("application/xml; charset=UTF-8");
        response.setHeader("Lock-Token", "<" + granted.getLockToken() + ">");
        response.setHeader("DAV", "1, 2");

        DavXmlUtils.writeLockResponse(response.getOutputStream(), granted);
    }

    private long parseTimeout(String timeoutHeader) {
        if (timeoutHeader == null) return 600;
        for (String part : timeoutHeader.split(",")) {
            part = part.trim();
            if (part.startsWith("Second-")) {
                try { return Long.parseLong(part.substring(7).trim()); }
                catch (NumberFormatException ignored) {}
            } else if ("Infinite".equalsIgnoreCase(part)) {
                return -1;
            }
        }
        return 600;
    }
}

