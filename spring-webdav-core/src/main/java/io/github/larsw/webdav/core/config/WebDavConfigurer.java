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

package io.github.larsw.webdav.core.config;

import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;

/**
 * Callback interface for customising the Spring WebDAV framework configuration.
 *
 * <p>Declare a bean implementing this interface in your {@code @Configuration} class
 * to override individual defaults without replacing the entire auto-configuration.
 * Follows the same pattern as Spring MVC's {@code WebMvcConfigurer}.
 *
 * <pre>{@code
 * @Configuration
 * public class MyWebDavConfig implements WebDavConfigurer {
 *
 *     @Override
 *     public String getDavPrefix() { return "/files"; }
 *
 *     @Override
 *     public WebDavResourceStore resourceStore() { return new MyCustomStore(); }
 * }
 * }</pre>
 */
public interface WebDavConfigurer {

    /**
     * The URL prefix under which all WebDAV endpoints are served.
     * Defaults to {@code ""} (root — the entire servlet handles WebDAV).
     */
    default String getDavPrefix() { return ""; }

    /**
     * Override to supply a custom {@link WebDavResourceStore}.
     * Return {@code null} to let auto-configuration choose the default.
     */
    default WebDavResourceStore resourceStore() { return null; }

    /**
     * Override to supply a custom {@link WebDavLockManager}.
     * Return {@code null} to use the built-in {@code InMemoryLockManager}.
     */
    default WebDavLockManager lockManager() { return null; }

    /**
     * Override to supply a custom {@link WebDavPropertyStore}.
     * Return {@code null} to use the built-in {@code DefaultWebDavPropertyStore}.
     */
    default WebDavPropertyStore propertyStore() { return null; }
}

