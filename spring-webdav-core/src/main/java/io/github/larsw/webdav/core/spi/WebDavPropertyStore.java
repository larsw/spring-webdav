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

package io.github.larsw.webdav.core.spi;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Set;

/**
 * SPI for storing and retrieving WebDAV dead properties (RFC 4918, Section 4).
 *
 * <p>Dead properties are arbitrary XML properties set by clients via PROPPATCH.
 * Live properties (displayname, getlastmodified, etc.) are derived from {@link WebDavResource}.
 */
public interface WebDavPropertyStore {

    /**
     * Returns all dead properties stored for {@code path}.
     */
    Map<QName, String> getProperties(String path);

    /**
     * Stores or updates dead properties for {@code path}.
     */
    void setProperties(String path, Map<QName, String> properties);

    /**
     * Removes the specified dead properties from {@code path}.
     */
    void removeProperties(String path, Set<QName> propertyNames);

    /**
     * Called when a resource is deleted so that its dead properties can be purged.
     */
    void onDelete(String path);

    /**
     * Called when a resource is moved so that dead properties follow the resource.
     */
    void onMove(String sourcePath, String destPath);
}

