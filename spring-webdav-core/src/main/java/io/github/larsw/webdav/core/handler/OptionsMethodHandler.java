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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles OPTIONS — advertises WebDAV Class 1+2 compliance plus all supported methods.
 * The {@code MS-Author-Via: DAV} header is required by the Windows WebDAV Mini-Redirector.
 */
public class OptionsMethodHandler implements WebDavMethodHandler {

    private static final String ALLOWED_METHODS =
            "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, COPY, MOVE, PROPFIND, PROPPATCH, LOCK, UNLOCK";

    @Override
    public String getMethod() { return "OPTIONS"; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("DAV", "1, 2");
        response.setHeader("MS-Author-Via", "DAV");
        response.setHeader("Allow", ALLOWED_METHODS);
        response.setHeader("Content-Length", "0");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}

