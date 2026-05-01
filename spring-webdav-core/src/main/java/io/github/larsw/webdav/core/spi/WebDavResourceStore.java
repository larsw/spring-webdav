package io.github.larsw.webdav.core.spi;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Primary plugin point for the Spring WebDAV framework.
 *
 * <p>Implement this interface to back WebDAV with any storage backend
 * (filesystem, S3, database, …) and register it as a Spring bean.
 * The autoconfiguration will pick it up via {@code @ConditionalOnMissingBean}.
 */
public interface WebDavResourceStore {

    /**
     * Returns the resource at the given server-relative path, or empty if it does not exist.
     */
    Optional<WebDavResource> getResource(String path);

    /**
     * Lists the direct children of the collection at {@code path}.
     * Returns an empty list for non-collection resources or missing paths.
     */
    List<WebDavResource> listChildren(String path);

    /**
     * Creates a new collection (directory) at {@code path}.
     *
     * @throws WebDavException with status 409 Conflict if the parent does not exist
     * @throws WebDavException with status 405 Method Not Allowed if a resource already exists there
     */
    void createCollection(String path) throws WebDavException;

    /**
     * Deletes the resource or collection (recursively) at {@code path}.
     *
     * @throws WebDavException with status 404 if the resource does not exist
     * @throws WebDavException with status 423 Locked if the resource is locked
     */
    void delete(String path) throws WebDavException;

    /**
     * Copies a resource or collection.
     *
     * @param sourcePath source server-relative path
     * @param destPath   destination server-relative path
     * @param overwrite  whether to overwrite an existing destination
     * @param recursive  whether to copy collections recursively (Depth: Infinity)
     * @throws WebDavException with status 412 Precondition Failed if overwrite is false and dest exists
     */
    void copy(String sourcePath, String destPath, boolean overwrite, boolean recursive) throws WebDavException;

    /**
     * Moves (renames) a resource or collection.
     *
     * @param sourcePath source server-relative path
     * @param destPath   destination server-relative path
     * @param overwrite  whether to overwrite an existing destination
     * @throws WebDavException with status 412 Precondition Failed if overwrite is false and dest exists
     */
    void move(String sourcePath, String destPath, boolean overwrite) throws WebDavException;

    /**
     * Opens an input stream to the content of the resource at {@code path}.
     *
     * @throws WebDavException with status 404 if the resource does not exist
     * @throws WebDavException with status 409 if the path is a collection
     */
    InputStream getContent(String path) throws WebDavException;

    /**
     * Stores content for the resource at {@code path}, creating it if necessary.
     *
     * @param path          server-relative path
     * @param content       body stream (the caller is responsible for closing it)
     * @param contentType   MIME type of the content, may be {@code null}
     * @param contentLength byte length of the content, or {@code -1} if unknown
     * @throws WebDavException with status 409 if the parent collection does not exist
     */
    void putContent(String path, InputStream content, String contentType, long contentLength)
            throws WebDavException;
}

