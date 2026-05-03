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

package io.github.larsw.webdav.core.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles a single WebDAV HTTP method (PROPFIND, LOCK, PUT, etc.).
 *
 * <p>Beans implementing this interface are auto-discovered by
 * {@link WebDavHttpRequestHandler} and dispatched to by method name.
 * Override any built-in handler by declaring a bean with the same method name.
 */
public interface WebDavMethodHandler {

    /** Returns the HTTP method name this handler handles (e.g. {@code "PROPFIND"}). */
    String getMethod();

    void handle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException;
}

