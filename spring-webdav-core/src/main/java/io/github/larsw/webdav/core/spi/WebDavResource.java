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
import java.time.Instant;
import java.util.Map;

/**
 * Immutable value object representing a single WebDAV resource (file or collection).
 * Implementations are returned by {@link WebDavResourceStore}.
 */
public interface WebDavResource {

    /** Full server-relative path, e.g. {@code /documents/report.pdf}. */
    String getPath();

    /** Just the name segment, e.g. {@code report.pdf}. */
    String getName();

    /** {@code true} for directories/collections, {@code false} for files. */
    boolean isCollection();

    Instant getCreationDate();

    Instant getLastModified();

    /** Content length in bytes; {@code -1} for collections. */
    long getContentLength();

    /** MIME type; {@code null} for collections. */
    String getContentType();

    /** ETag value without surrounding quotes. */
    String getETag();

    /**
     * Dead (custom) properties stored for this resource.
     * Live WebDAV properties are derived from the fields above.
     */
    Map<QName, String> getDeadProperties();
}

