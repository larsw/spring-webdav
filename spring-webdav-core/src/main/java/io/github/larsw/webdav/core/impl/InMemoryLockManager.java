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

package io.github.larsw.webdav.core.impl;

import io.github.larsw.webdav.core.model.LockInfo;
import io.github.larsw.webdav.core.model.LockResult;
import io.github.larsw.webdav.core.model.LockScope;
import io.github.larsw.webdav.core.spi.WebDavException;
import io.github.larsw.webdav.core.spi.WebDavLockManager;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory implementation of {@link WebDavLockManager}.
 *
 * <p>Lock state is lost on restart. Suitable for development and single-instance deployments.
 * Replace with a persistent implementation (e.g. JDBC) for production clustered use.
 */
public class InMemoryLockManager implements WebDavLockManager {

    /** path → active LockInfo */
    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    @Override
    public LockResult lock(String path, LockInfo request) throws WebDavException {
        // Purge expired locks lazily
        locks.entrySet().removeIf(e -> e.getValue().isExpired());

        LockInfo existing = locks.get(path);
        if (existing != null && !existing.isExpired()) {
            // Shared locks can co-exist; exclusive locks cannot be double-acquired
            if (existing.getScope() == LockScope.EXCLUSIVE
                    || request.getScope() == LockScope.EXCLUSIVE) {
                throw new WebDavException(423, "Resource is already locked: " + path);
            }
        }

        LockInfo granted = new LockInfo(
                path,
                request.getOwner(),
                request.getType(),
                request.getScope(),
                request.getDepth(),
                request.getTimeoutSeconds()
        );
        granted.setLockToken("urn:uuid:" + UUID.randomUUID());
        Instant expiry = request.getTimeoutSeconds() == -1
                ? null
                : Instant.now().plusSeconds(request.getTimeoutSeconds());
        granted.setExpiresAt(expiry);

        locks.put(path, granted);
        return new LockResult(granted, false);
    }

    @Override
    public boolean unlock(String path, String lockToken) {
        LockInfo existing = locks.get(path);
        if (existing != null && lockToken.equals(existing.getLockToken())) {
            locks.remove(path);
            return true;
        }
        return false;
    }

    @Override
    public Optional<LockInfo> getActiveLock(String path) {
        LockInfo info = locks.get(path);
        if (info == null || info.isExpired()) {
            locks.remove(path);
            return Optional.empty();
        }
        return Optional.of(info);
    }

    @Override
    public boolean isLocked(String path) {
        // Check the exact path and all ancestor paths (for depth-infinity locks)
        String check = path;
        while (!check.isEmpty()) {
            if (getActiveLock(check).isPresent()) return true;
            int slash = check.lastIndexOf('/');
            if (slash <= 0) break;
            check = check.substring(0, slash);
        }
        return false;
    }

    @Override
    public LockResult refreshLock(String path, String lockToken, long timeoutSeconds)
            throws WebDavException {
        LockInfo existing = locks.get(path);
        if (existing == null || !lockToken.equals(existing.getLockToken())) {
            throw new WebDavException(412, "Lock token does not match: " + path);
        }
        existing.setTimeoutSeconds(timeoutSeconds);
        existing.setExpiresAt(timeoutSeconds == -1
                ? null
                : Instant.now().plusSeconds(timeoutSeconds));
        return new LockResult(existing, false);
    }
}

