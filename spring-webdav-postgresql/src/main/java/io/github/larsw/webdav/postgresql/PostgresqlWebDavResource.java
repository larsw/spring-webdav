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

package io.github.larsw.webdav.postgresql;

import io.github.larsw.webdav.core.spi.WebDavResource;

import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.Map;

/**
 * Immutable {@link WebDavResource} representing either a synthetic collection or a
 * database-row-backed file inside the PostgreSQL WebDAV store.
 */
public final class PostgresqlWebDavResource implements WebDavResource {

    private final String path;
    private final boolean collection;
    private final long contentLength;
    private final String contentType;
    private final String eTag;
    private final Instant lastModified;

    // ---- Factory methods ----------------------------------------------------

    /** Creates a synthetic collection resource at the given DAV path. */
    public static PostgresqlWebDavResource collection(String path) {
        return new PostgresqlWebDavResource(path, true, -1L, null, "", Instant.EPOCH);
    }

    /**
     * Creates a file resource.
     *
     * @param path          server-relative DAV path
     * @param contentLength byte length of the generated content
     * @param contentType   MIME type (e.g. {@code text/csv; charset=UTF-8})
     * @param eTag          hex-encoded MD5 of the generated content
     * @param lastModified  best-effort last-modified timestamp (may be {@link Instant#EPOCH})
     */
    public static PostgresqlWebDavResource file(String path,
                                                long contentLength,
                                                String contentType,
                                                String eTag,
                                                Instant lastModified) {
        return new PostgresqlWebDavResource(path, false, contentLength, contentType, eTag, lastModified);
    }

    private PostgresqlWebDavResource(String path, boolean collection,
                                     long contentLength, String contentType,
                                     String eTag, Instant lastModified) {
        this.path          = path;
        this.collection    = collection;
        this.contentLength = contentLength;
        this.contentType   = contentType;
        this.eTag          = eTag;
        this.lastModified  = lastModified;
    }

    // ---- WebDavResource -----------------------------------------------------

    @Override
    public String getPath() { return path; }

    @Override
    public String getName() {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }

    @Override public boolean isCollection()        { return collection; }
    @Override public Instant getCreationDate()     { return lastModified; }
    @Override public Instant getLastModified()     { return lastModified; }
    @Override public long getContentLength()       { return contentLength; }
    @Override public String getContentType()       { return contentType; }
    @Override public String getETag()              { return eTag; }
    @Override public Map<QName, String> getDeadProperties() { return Map.of(); }
}

