package io.github.larsw.webdav.autoconfigure;

import io.github.larsw.webdav.core.config.WebDavConfigurer;
import io.github.larsw.webdav.core.config.WebDavCoreConfiguration;
import io.github.larsw.webdav.core.impl.DefaultWebDavPropertyStore;
import io.github.larsw.webdav.core.impl.InMemoryLockManager;
import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot autoconfiguration for Spring WebDAV.
 *
 * <p>Activated automatically when {@code spring-webdav-autoconfigure} is on the classpath
 * inside a servlet-based web application. Registers default beans for the lock manager,
 * property store, and — via the imported {@link WebDavCoreConfiguration} — all method
 * handlers and the handler mapping.
 *
 * <p>Override any bean by declaring your own:
 * <ul>
 *   <li>{@link io.github.larsw.webdav.core.spi.WebDavResourceStore} — <b>required</b></li>
 *   <li>{@link WebDavLockManager} — optional, replaces {@link InMemoryLockManager}</li>
 *   <li>{@link WebDavPropertyStore} — optional, replaces {@link DefaultWebDavPropertyStore}</li>
 *   <li>{@link WebDavConfigurer} — optional, for prefix / wiring customisation</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebDavProperties.class)
@Import(WebDavCoreConfiguration.class)
public class WebDavAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebDavLockManager.class)
    public WebDavLockManager webDavLockManager() {
        return new InMemoryLockManager();
    }

    @Bean
    @ConditionalOnMissingBean(WebDavPropertyStore.class)
    public WebDavPropertyStore webDavPropertyStore() {
        return new DefaultWebDavPropertyStore();
    }

    /**
     * Bridges {@link WebDavProperties} to {@link WebDavConfigurer} so that the
     * {@code spring.webdav.prefix} property is honored without requiring the user to
     * implement the configurer themselves.
     */
    @Bean
    @ConditionalOnMissingBean(WebDavConfigurer.class)
    public WebDavConfigurer webDavConfigurer(WebDavProperties properties) {
        return new WebDavConfigurer() {
            @Override
            public String getDavPrefix() { return properties.getPrefix(); }
        };
    }
}

