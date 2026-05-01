package io.github.larsw.webdav.filesystem;

import io.github.larsw.webdav.core.spi.WebDavException;
import io.github.larsw.webdav.core.spi.WebDavResource;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link WebDavResourceStore} backed by the local filesystem using {@code java.nio.file}.
 *
 * <p>All DAV paths are resolved relative to a configured root directory.
 * Path traversal outside the root is rejected (status 403).
 */
public class NioFileSystemResourceStore implements WebDavResourceStore {

    private final Path root;

    public NioFileSystemResourceStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(this.root)) {
            try { Files.createDirectories(this.root); }
            catch (IOException e) { throw new IllegalStateException("Cannot create WebDAV root: " + root, e); }
        }
    }

    // ---- Store interface ---------------------------------------------------

    @Override
    public Optional<WebDavResource> getResource(String path) {
        Path resolved = resolve(path);
        return Files.exists(resolved)
                ? Optional.of(new NioFileSystemResource(resolved, normalizeDavPath(path)))
                : Optional.empty();
    }

    @Override
    public List<WebDavResource> listChildren(String path) {
        Path dir = resolve(path);
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .map(p -> (WebDavResource) new NioFileSystemResource(p, childDavPath(path, p.getFileName().toString())))
                    .toList();
        } catch (IOException e) {
            throw new WebDavException(500, "Cannot list directory: " + path, e);
        }
    }

    @Override
    public void createCollection(String path) throws WebDavException {
        Path dir = resolve(path);
        if (Files.exists(dir)) {
            throw new WebDavException(405, "Resource already exists: " + path);
        }
        Path parent = dir.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new WebDavException(409, "Parent collection does not exist: " + path);
        }
        try { Files.createDirectory(dir); }
        catch (IOException e) { throw new WebDavException(500, "Cannot create collection: " + path, e); }
    }

    @Override
    public void delete(String path) throws WebDavException {
        Path target = resolve(path);
        if (!Files.exists(target)) throw new WebDavException(404, "Not found: " + path);
        try { deleteRecursive(target); }
        catch (IOException e) { throw new WebDavException(500, "Cannot delete: " + path, e); }
    }

    @Override
    public void copy(String src, String dest, boolean overwrite, boolean recursive)
            throws WebDavException {
        Path srcPath = resolve(src);
        Path destPath = resolve(dest);
        if (!Files.exists(srcPath)) throw new WebDavException(404, "Not found: " + src);
        if (Files.exists(destPath) && !overwrite)
            throw new WebDavException(412, "Destination exists and Overwrite is false");
        try {
            if (Files.exists(destPath)) deleteRecursive(destPath);
            if (Files.isDirectory(srcPath) && recursive) {
                copyDirectoryRecursive(srcPath, destPath);
            } else {
                Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) { throw new WebDavException(500, "Cannot copy: " + src, e); }
    }

    @Override
    public void move(String src, String dest, boolean overwrite) throws WebDavException {
        Path srcPath = resolve(src);
        Path destPath = resolve(dest);
        if (!Files.exists(srcPath)) throw new WebDavException(404, "Not found: " + src);
        if (Files.exists(destPath) && !overwrite)
            throw new WebDavException(412, "Destination exists and Overwrite is false");
        try {
            if (Files.exists(destPath)) deleteRecursive(destPath);
            Files.move(srcPath, destPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            try { Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException e) { throw new WebDavException(500, "Cannot move: " + src, e); }
        } catch (IOException e) { throw new WebDavException(500, "Cannot move: " + src, e); }
    }

    @Override
    public InputStream getContent(String path) throws WebDavException {
        Path file = resolve(path);
        if (!Files.exists(file)) throw new WebDavException(404, "Not found: " + path);
        if (Files.isDirectory(file)) throw new WebDavException(409, "Path is a collection: " + path);
        try { return new BufferedInputStream(Files.newInputStream(file)); }
        catch (IOException e) { throw new WebDavException(500, "Cannot open: " + path, e); }
    }

    @Override
    public void putContent(String path, InputStream content, String contentType, long contentLength)
            throws WebDavException {
        Path file = resolve(path);
        Path parent = file.getParent();
        if (parent == null || !Files.isDirectory(parent))
            throw new WebDavException(409, "Parent collection does not exist: " + path);
        try { Files.copy(content, file, StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { throw new WebDavException(500, "Cannot write: " + path, e); }
    }

    // ---- Helpers -----------------------------------------------------------

    private Path resolve(String davPath) {
        String normalized = davPath == null ? "/" : davPath;
        // Strip leading slash for resolution
        String relative = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new WebDavException(403, "Path traversal detected: " + davPath);
        }
        return resolved;
    }

    private String normalizeDavPath(String path) {
        if (path == null || path.isEmpty()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }

    private String childDavPath(String parentPath, String childName) {
        String parent = normalizeDavPath(parentPath);
        return parent.endsWith("/") ? parent + childName : parent + "/" + childName;
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> children = Files.list(path)) {
                for (Path child : children.toList()) deleteRecursive(child);
            }
        }
        Files.deleteIfExists(path);
    }

    private void copyDirectoryRecursive(Path src, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (Stream<Path> children = Files.list(src)) {
            for (Path child : children.toList()) {
                Path childDest = dest.resolve(child.getFileName());
                if (Files.isDirectory(child)) {
                    copyDirectoryRecursive(child, childDest);
                } else {
                    Files.copy(child, childDest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}

