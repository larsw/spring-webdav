package io.github.larsw.webdav.core.model;

import java.time.Instant;

/**
 * Describes the lock state of a resource.
 * Used both as the input to {@link io.github.larsw.webdav.core.spi.WebDavLockManager#lock} and
 * as the stored representation of an active lock.
 */
public class LockInfo {

    /** Server-relative path of the locked resource. */
    private String path;

    /** Opaque lock token (urn:uuid:…), populated after a lock is granted. */
    private String lockToken;

    /** Raw owner value from the LOCK request XML (may be a mailto: href or free text). */
    private String owner;

    private LockType type = LockType.WRITE;
    private LockScope scope = LockScope.EXCLUSIVE;

    /**
     * Lock depth: {@code 0} for resource only, {@code -1} for {@code Depth: Infinity}.
     */
    private int depth = 0;

    /** Requested timeout in seconds; {@code -1} means infinite. */
    private long timeoutSeconds = 600;

    /** Absolute expiry time, set by the lock manager when the lock is granted. */
    private Instant expiresAt;

    public LockInfo() {}

    public LockInfo(String path, String owner, LockType type, LockScope scope, int depth, long timeoutSeconds) {
        this.path = path;
        this.owner = owner;
        this.type = type;
        this.scope = scope;
        this.depth = depth;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getLockToken() { return lockToken; }
    public void setLockToken(String lockToken) { this.lockToken = lockToken; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public LockType getType() { return type; }
    public void setType(LockType type) { this.type = type; }

    public LockScope getScope() { return scope; }
    public void setScope(LockScope scope) { this.scope = scope; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}

