package io.github.larsw.webdav.core.model;

/**
 * Returned by {@link io.github.larsw.webdav.core.spi.WebDavLockManager#lock} and
 * {@link io.github.larsw.webdav.core.spi.WebDavLockManager#refreshLock}.
 */
public class LockResult {

    private final LockInfo lockInfo;

    /** {@code true} if the resource was newly created during the lock operation (PUT+LOCK). */
    private final boolean resourceCreated;

    public LockResult(LockInfo lockInfo, boolean resourceCreated) {
        this.lockInfo = lockInfo;
        this.resourceCreated = resourceCreated;
    }

    public LockInfo getLockInfo() { return lockInfo; }
    public boolean isResourceCreated() { return resourceCreated; }
}

