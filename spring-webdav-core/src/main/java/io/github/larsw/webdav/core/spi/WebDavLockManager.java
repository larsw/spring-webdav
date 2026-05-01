package io.github.larsw.webdav.core.spi;

import io.github.larsw.webdav.core.model.LockInfo;
import io.github.larsw.webdav.core.model.LockResult;

import java.util.Optional;

/**
 * SPI for managing WebDAV lock state (RFC 4918, Section 6).
 *
 * <p>The default implementation ({@code InMemoryLockManager}) stores locks in memory.
 * Replace this bean for persistent lock storage (e.g. JDBC, Redis).
 */
public interface WebDavLockManager {

    /**
     * Acquires a lock on {@code path}.
     *
     * @return a {@link LockResult} containing the granted lock token and active lock details
     * @throws WebDavException with status 423 Locked if the resource is already exclusively locked
     */
    LockResult lock(String path, LockInfo request) throws WebDavException;

    /**
     * Releases the lock identified by {@code lockToken} on {@code path}.
     *
     * @return {@code true} if the lock was found and removed
     */
    boolean unlock(String path, String lockToken);

    /**
     * Returns the active lock on {@code path}, if any.
     */
    Optional<LockInfo> getActiveLock(String path);

    /**
     * Returns {@code true} if {@code path} or any of its ancestors currently hold an active lock.
     */
    boolean isLocked(String path);

    /**
     * Refreshes (extends) the timeout on an existing lock.
     *
     * @throws WebDavException with status 412 if the lock token does not match
     */
    LockResult refreshLock(String path, String lockToken, long timeoutSeconds) throws WebDavException;
}

