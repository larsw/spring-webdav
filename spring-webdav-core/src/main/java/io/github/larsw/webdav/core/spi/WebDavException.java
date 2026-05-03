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

package io.github.larsw.webdav.core.spi;

/**
 * Unchecked exception thrown by WebDAV store operations.
 * Carries an HTTP status code that is forwarded directly to the client.
 */
public class WebDavException extends RuntimeException {

    private final int statusCode;

    public WebDavException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public WebDavException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** The HTTP status code to return to the client (e.g. 404, 409, 412, 423). */
    public int getStatusCode() {
        return statusCode;
    }
}

