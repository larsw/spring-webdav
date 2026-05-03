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

package io.github.larsw.webdav.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Spring WebDAV framework.
 *
 * <pre>{@code
 * spring:
 *   webdav:
 *     prefix: /dav
 * }</pre>
 */
@ConfigurationProperties(prefix = "spring.webdav")
public class WebDavProperties {

    /**
     * URL prefix under which all WebDAV endpoints are served.
     * Defaults to root ({@code ""}) which makes the entire server a WebDAV endpoint.
     */
    private String prefix = "";

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
}

