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

package io.github.larsw.webdav.s3;

import io.github.larsw.webdav.core.spi.WebDavResource;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.Map;

/**
 * {@link WebDavResource} backed by an S3 object metadata entry.
 *
 * <p>Collections are represented as synthetic "folder" keys ending with {@code /}
 * (common-prefix entries returned by the list-objects-v2 response).
 */
public class S3WebDavResource implements WebDavResource {

    private final String davPath;
    private final boolean collection;
    private final long size;
    private final Instant lastModified;
    private final String contentType;
    private final String eTag;

    /** Construct from an S3Object listing entry (regular file). */
    public S3WebDavResource(S3Object s3Object, String davPath) {
        this.davPath = davPath;
        this.collection = false;
        this.size = s3Object.size();
        this.lastModified = s3Object.lastModified();
        this.contentType = null;   // not available in list response
        this.eTag = s3Object.eTag() != null ? s3Object.eTag().replace("\"", "") : "";
    }

    /** Construct from a HeadObject response (single file, full metadata). */
    public S3WebDavResource(HeadObjectResponse head, String davPath) {
        this.davPath = davPath;
        this.collection = false;
        this.size = head.contentLength() != null ? head.contentLength() : -1;
        this.lastModified = head.lastModified();
        this.contentType = head.contentType();
        this.eTag = head.eTag() != null ? head.eTag().replace("\"", "") : "";
    }

    /** Construct a synthetic collection from a common-prefix. */
    public static S3WebDavResource collection(String davPath) {
        return new S3WebDavResource(davPath);
    }

    private S3WebDavResource(String davPath) {
        this.davPath = davPath;
        this.collection = true;
        this.size = -1;
        this.lastModified = Instant.EPOCH;
        this.contentType = null;
        this.eTag = "";
    }

    @Override public String getPath() { return davPath; }
    @Override public String getName() {
        String p = davPath.endsWith("/") ? davPath.substring(0, davPath.length() - 1) : davPath;
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }
    @Override public boolean isCollection() { return collection; }
    @Override public Instant getCreationDate() { return lastModified; }  // S3 has no creation date
    @Override public Instant getLastModified() { return lastModified; }
    @Override public long getContentLength() { return size; }
    @Override public String getContentType() { return contentType; }
    @Override public String getETag() { return eTag; }
    @Override public Map<QName, String> getDeadProperties() { return Map.of(); }
}

