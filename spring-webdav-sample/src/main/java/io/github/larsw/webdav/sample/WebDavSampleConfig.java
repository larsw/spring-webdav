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

package io.github.larsw.webdav.sample;

import io.github.larsw.webdav.filesystem.NioFileSystemResourceStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * Optional explicit WebDAV configuration for the sample application.
 *
 * <p>This class is here for illustration purposes. In most cases you do NOT need it —
 * simply setting {@code spring.webdav.*} properties in {@code application.yaml} is enough.
 *
 * <p>Uncomment the {@code @Bean} below to override the auto-configured
 * {@link io.github.larsw.webdav.core.spi.WebDavResourceStore} programmatically.
 */
@Configuration(proxyBeanMethods = false)
public class WebDavSampleConfig {

    /**
     * Example: provide a custom WebDavConfigurer to override just the prefix
     * while keeping autoconfiguration for everything else.
     *
     * <p>Comment out (or remove) this bean to fall back to the
     * {@code spring.webdav.prefix} property in application.yaml.
     */
    // @Bean
    // public WebDavConfigurer webDavConfigurer() {
    //     return new WebDavConfigurer() {
    //         @Override
    //         public String getDavPrefix() { return "/dav"; }
    //     };
    // }

    /**
     * Example: replace the autoconfigured filesystem store by injecting a custom root.
     * The autoconfiguration detects {@code spring.webdav.filesystem.root} automatically,
     * so this bean is only needed when the root must be resolved at runtime from a
     * non-standard property or other logic.
     *
     * Activate with: {@code -Dwebdav.custom.root=/my/path}
     */
    @Bean
    @ConditionalOnProperty("webdav.custom.root")
    public NioFileSystemResourceStore customNioFileSystemResourceStore(
            @Value("${webdav.custom.root}") String root) {
        return new NioFileSystemResourceStore(Paths.get(root));
    }
}

