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

package io.github.larsw.webdav.filesystem.autoconfigure;

import io.github.larsw.webdav.filesystem.NioFileSystemResourceStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Paths;

/**
 * Auto-configuration for the local-filesystem {@link NioFileSystemResourceStore}.
 *
 * <p>Activated when {@code spring.webdav.filesystem.root} is set in application properties
 * and no other {@link io.github.larsw.webdav.core.spi.WebDavResourceStore} bean is present.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.webdav.filesystem", name = "root")
@EnableConfigurationProperties(FileSystemWebDavAutoConfiguration.FileSystemProperties.class)
public class FileSystemWebDavAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NioFileSystemResourceStore nioFileSystemResourceStore(FileSystemProperties props) {
        return new NioFileSystemResourceStore(Paths.get(props.getRoot()));
    }

    @ConfigurationProperties(prefix = "spring.webdav.filesystem")
    public static class FileSystemProperties {
        /** Absolute path to the root directory served over WebDAV. */
        private String root = System.getProperty("user.home") + "/webdav";

        public String getRoot() { return root; }
        public void setRoot(String root) { this.root = root; }
    }
}

