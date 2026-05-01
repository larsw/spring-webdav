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

