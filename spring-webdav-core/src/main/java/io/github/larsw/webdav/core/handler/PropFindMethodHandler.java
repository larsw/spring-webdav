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
import io.github.larsw.webdav.core.model.PropFindRequest;
import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import io.github.larsw.webdav.core.spi.WebDavResource;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import io.github.larsw.webdav.core.xml.DavXmlUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

import static io.github.larsw.webdav.core.xml.DavNamespaces.*;

/**
 * Handles PROPFIND (RFC 4918, Section 9.1).
 *
 * <p>Supports {@code Depth: 0}, {@code Depth: 1}, and {@code Depth: Infinity}
 * with {@code allprop}, {@code propname}, and {@code prop} request types.
 */
public class PropFindMethodHandler implements WebDavMethodHandler {

    private static final int SC_MULTI_STATUS = 207;

    private final WebDavResourceStore store;
    private final WebDavPropertyStore propertyStore;
    private final WebDavLockManager lockManager;
    private final String davPrefix;

    public PropFindMethodHandler(WebDavResourceStore store,
                                  WebDavPropertyStore propertyStore,
                                  WebDavLockManager lockManager,
                                  String davPrefix) {
        this.store = store;
        this.propertyStore = propertyStore;
        this.lockManager = lockManager;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return METHOD_PROPFIND; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);
        int depth = DavPathUtils.parseDepth(request.getHeader("Depth"), true);

        Optional<WebDavResource> resourceOpt = store.getResource(path);
        if (resourceOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        PropFindRequest propFind = DavXmlUtils.parsePropFind(request.getInputStream());

        List<WebDavResource> resources = collectResources(resourceOpt.get(), depth);

        // Build dead-property and lock maps
        Map<String, Map<javax.xml.namespace.QName, String>> deadProps = new HashMap<>();
        Map<String, LockInfo> locks = new HashMap<>();
        for (WebDavResource r : resources) {
            deadProps.put(r.getPath(), propertyStore.getProperties(r.getPath()));
            lockManager.getActiveLock(r.getPath()).ifPresent(l -> locks.put(r.getPath(), l));
        }

        response.setStatus(SC_MULTI_STATUS);
        response.setContentType("application/xml; charset=UTF-8");
        response.setHeader("DAV", "1, 2");

        DavXmlUtils.writeMultiStatus(response.getOutputStream(), resources, propFind, deadProps, locks, davPrefix);
    }

    private List<WebDavResource> collectResources(WebDavResource root, int depth) {
        List<WebDavResource> result = new ArrayList<>();
        result.add(root);
        if (depth != 0 && root.isCollection()) {
            List<WebDavResource> children = store.listChildren(root.getPath());
            if (depth == 1) {
                result.addAll(children);
            } else {
                // Infinity
                for (WebDavResource child : children) {
                    result.addAll(collectResources(child, depth));
                }
            }
        }
        return result;
    }
}

