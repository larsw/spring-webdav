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

