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

package io.github.larsw.webdav.s3.autoconfigure;

import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import io.github.larsw.webdav.s3.S3ResourceStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Auto-configuration for the S3-backed {@link S3ResourceStore}.
 *
 * <p>Activated when:
 * <ul>
 *   <li>{@code spring.webdav.s3.bucket} is set</li>
 *   <li>The AWS SDK v2 S3 client is on the classpath</li>
 *   <li>No other {@link WebDavResourceStore} bean is present</li>
 * </ul>
 *
 * <p>AWS credentials are resolved via the standard
 * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider} chain
 * (environment variables, instance profile, etc.).
 */
@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(prefix = "spring.webdav.s3", name = "bucket")
@EnableConfigurationProperties(S3WebDavAutoConfiguration.S3Properties.class)
public class S3WebDavAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(S3Client.class)
    public S3Client s3Client(S3Properties props) {
        var builder = S3Client.builder();
        if (props.getRegion() != null && !props.getRegion().isBlank()) {
            builder.region(Region.of(props.getRegion()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(WebDavResourceStore.class)
    public S3ResourceStore s3ResourceStore(S3Client s3Client, S3Properties props) {
        return new S3ResourceStore(s3Client, props.getBucket());
    }

    @ConfigurationProperties(prefix = "spring.webdav.s3")
    public static class S3Properties {
        /** S3 bucket name to serve over WebDAV. */
        private String bucket;
        /** AWS region (e.g. {@code eu-west-1}). Defaults to SDK auto-detection. */
        private String region;

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }
}

