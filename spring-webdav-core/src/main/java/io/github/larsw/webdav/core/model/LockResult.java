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

