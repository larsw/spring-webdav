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

package io.github.larsw.webdav.filesystem;

import io.github.larsw.webdav.core.spi.WebDavResource;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * {@link WebDavResource} backed by a {@link java.nio.file.Path}.
 */
public class NioFileSystemResource implements WebDavResource {

    private final Path file;
    private final String davPath;
    private final BasicFileAttributes attrs;

    public NioFileSystemResource(Path file, String davPath) {
        this.file = file;
        this.davPath = davPath;
        try {
            this.attrs = Files.readAttributes(file, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file attributes: " + file, e);
        }
    }

    @Override public String getPath() { return davPath; }
    @Override public String getName() { return file.getFileName() != null ? file.getFileName().toString() : "/"; }
    @Override public boolean isCollection() { return attrs.isDirectory(); }
    @Override public Instant getCreationDate() { return attrs.creationTime().toInstant(); }
    @Override public Instant getLastModified() { return attrs.lastModifiedTime().toInstant(); }
    @Override public long getContentLength() { return isCollection() ? -1 : attrs.size(); }

    @Override
    public String getContentType() {
        if (isCollection()) return null;
        try { return Files.probeContentType(file); }
        catch (IOException e) { return "application/octet-stream"; }
    }

    @Override
    public String getETag() {
        // Simple ETag: hex of last-modified millis + size
        long lm = attrs.lastModifiedTime().toMillis();
        long sz = isCollection() ? 0 : attrs.size();
        return HexFormat.of().toHexDigits(lm) + HexFormat.of().toHexDigits(sz);
    }

    @Override
    public Map<QName, String> getDeadProperties() { return Map.of(); }
}

