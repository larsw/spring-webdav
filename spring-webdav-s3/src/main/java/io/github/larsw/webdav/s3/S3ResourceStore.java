package io.github.larsw.webdav.s3;

import io.github.larsw.webdav.core.spi.WebDavException;
import io.github.larsw.webdav.core.spi.WebDavResource;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link WebDavResourceStore} backed by an AWS S3 bucket.
 *
 * <p>S3 is a flat key-value store. This implementation follows the de-facto convention of:
 * <ul>
 *   <li>Regular objects → WebDAV files (e.g. key {@code docs/report.pdf})</li>
 *   <li>Zero-byte objects ending with {@code /} → WebDAV collections (e.g. key {@code docs/})</li>
 *   <li>Common prefixes from list responses → virtual collections</li>
 * </ul>
 *
 * <p>The root collection ({@code /}) maps to the bucket root.
 */
public class S3ResourceStore implements WebDavResourceStore {

    private final S3Client s3;
    private final String bucket;

    public S3ResourceStore(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    // ---- Helpers -----------------------------------------------------------

    /** Converts a DAV path ({@code /docs/file.txt}) to an S3 key ({@code docs/file.txt}). */
    private String toKey(String davPath) {
        String key = davPath.startsWith("/") ? davPath.substring(1) : davPath;
        return key;
    }

    /** Converts a DAV path to the S3 collection key (always ends with {@code /}). */
    private String toCollectionKey(String davPath) {
        String key = toKey(davPath);
        return key.isEmpty() ? "" : (key.endsWith("/") ? key : key + "/");
    }

    private String toDavPath(String key) {
        return "/" + (key.endsWith("/") ? key.substring(0, key.length() - 1) : key);
    }

    // ---- Interface ---------------------------------------------------------

    @Override
    public Optional<WebDavResource> getResource(String path) {
        if ("/".equals(path) || path.isEmpty()) {
            return Optional.of(S3WebDavResource.collection("/"));
        }

        // Try collection first (key ending with /)
        String collKey = toCollectionKey(path);
        try {
            HeadObjectResponse head = s3.headObject(r -> r.bucket(bucket).key(collKey));
            return Optional.of(S3WebDavResource.collection(normalizePath(path)));
        } catch (NoSuchKeyException ignored) {}

        // Try regular object
        String fileKey = toKey(path);
        try {
            HeadObjectResponse head = s3.headObject(r -> r.bucket(bucket).key(fileKey));
            return Optional.of(new S3WebDavResource(head, normalizePath(path)));
        } catch (NoSuchKeyException ignored) {}

        // Check if it's a virtual prefix (has children)
        String prefix = collKey;
        ListObjectsV2Response probe = s3.listObjectsV2(r -> r.bucket(bucket)
                .prefix(prefix).maxKeys(1));
        if (!probe.contents().isEmpty() || !probe.commonPrefixes().isEmpty()) {
            return Optional.of(S3WebDavResource.collection(normalizePath(path)));
        }

        return Optional.empty();
    }

    @Override
    public List<WebDavResource> listChildren(String path) {
        String prefix = toCollectionKey(path);
        List<WebDavResource> result = new ArrayList<>();

        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .delimiter("/")
                .build();

        ListObjectsV2Response resp;
        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder builder = req.toBuilder();
            if (continuationToken != null) builder.continuationToken(continuationToken);
            resp = s3.listObjectsV2(builder.build());

            // Sub-collections (common prefixes)
            for (CommonPrefix cp : resp.commonPrefixes()) {
                result.add(S3WebDavResource.collection(toDavPath(cp.prefix())));
            }
            // Files
            for (S3Object obj : resp.contents()) {
                if (obj.key().equals(prefix)) continue; // skip the "folder" marker itself
                result.add(new S3WebDavResource(obj, toDavPath(obj.key())));
            }

            continuationToken = resp.nextContinuationToken();
        } while (Boolean.TRUE.equals(resp.isTruncated()));

        return result;
    }

    @Override
    public void createCollection(String path) throws WebDavException {
        if (getResource(path).isPresent())
            throw new WebDavException(405, "Resource already exists: " + path);
        String collKey = toCollectionKey(path);
        s3.putObject(r -> r.bucket(bucket).key(collKey).contentLength(0L),
                RequestBody.empty());
    }

    @Override
    public void delete(String path) throws WebDavException {
        if (getResource(path).isEmpty())
            throw new WebDavException(404, "Not found: " + path);

        String prefix = toCollectionKey(path);
        // List all objects under the prefix and delete them (recursive)
        ListObjectsV2Response resp;
        String token = null;
        do {
            String finalToken = token;
            resp = s3.listObjectsV2(r -> {
                r.bucket(bucket).prefix(prefix);
                if (finalToken != null) r.continuationToken(finalToken);
            });
            for (S3Object obj : resp.contents()) {
                s3.deleteObject(r -> r.bucket(bucket).key(obj.key()));
            }
            token = resp.nextContinuationToken();
        } while (Boolean.TRUE.equals(resp.isTruncated()));

        // Also try to delete the file key
        String fileKey = toKey(path);
        try { s3.deleteObject(r -> r.bucket(bucket).key(fileKey)); }
        catch (Exception ignored) {}
    }

    @Override
    public void copy(String src, String dest, boolean overwrite, boolean recursive)
            throws WebDavException {
        if (getResource(src).isEmpty()) throw new WebDavException(404, "Not found: " + src);
        if (getResource(dest).isPresent() && !overwrite)
            throw new WebDavException(412, "Destination exists and Overwrite is false");

        String srcKey = toKey(src);
        String destKey = toKey(dest);

        // Single file copy
        try {
            s3.copyObject(r -> r.sourceBucket(bucket).sourceKey(srcKey)
                    .destinationBucket(bucket).destinationKey(destKey));
            return;
        } catch (NoSuchKeyException ignored) {}

        // Collection copy
        if (recursive) {
            String srcPrefix = toCollectionKey(src);
            String destPrefix = toCollectionKey(dest);
            listAllKeys(srcPrefix).forEach(key -> {
                String newKey = destPrefix + key.substring(srcPrefix.length());
                s3.copyObject(r -> r.sourceBucket(bucket).sourceKey(key)
                        .destinationBucket(bucket).destinationKey(newKey));
            });
        }
    }

    @Override
    public void move(String src, String dest, boolean overwrite) throws WebDavException {
        copy(src, dest, overwrite, true);
        delete(src);
    }

    @Override
    public InputStream getContent(String path) throws WebDavException {
        String key = toKey(path);
        try {
            return s3.getObject(r -> r.bucket(bucket).key(key));
        } catch (NoSuchKeyException e) {
            throw new WebDavException(404, "Not found: " + path);
        }
    }

    @Override
    public void putContent(String path, InputStream content, String contentType, long contentLength)
            throws WebDavException {
        String key = toKey(path);
        RequestBody body = contentLength >= 0
                ? RequestBody.fromInputStream(content, contentLength)
                : RequestBody.fromBytes(readAllBytes(content, path));

        PutObjectRequest.Builder req = PutObjectRequest.builder().bucket(bucket).key(key);
        if (contentType != null) req.contentType(contentType);
        s3.putObject(req.build(), body);
    }

    // ---- Private helpers ---------------------------------------------------

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }

    private List<String> listAllKeys(String prefix) {
        List<String> keys = new ArrayList<>();
        String token = null;
        ListObjectsV2Response resp;
        do {
            String finalToken = token;
            resp = s3.listObjectsV2(r -> {
                r.bucket(bucket).prefix(prefix);
                if (finalToken != null) r.continuationToken(finalToken);
            });
            resp.contents().forEach(o -> keys.add(o.key()));
            token = resp.nextContinuationToken();
        } while (Boolean.TRUE.equals(resp.isTruncated()));
        return keys;
    }

    private byte[] readAllBytes(InputStream in, String path) throws WebDavException {
        try { return in.readAllBytes(); }
        catch (java.io.IOException e) { throw new WebDavException(500, "Cannot read content for: " + path, e); }
    }
}

