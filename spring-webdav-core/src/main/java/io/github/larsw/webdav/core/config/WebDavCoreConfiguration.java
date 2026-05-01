package io.github.larsw.webdav.core.config;

import io.github.larsw.webdav.core.handler.*;
import io.github.larsw.webdav.core.impl.DefaultWebDavPropertyStore;
import io.github.larsw.webdav.core.impl.InMemoryLockManager;
import io.github.larsw.webdav.core.spi.WebDavLockManager;
import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import io.github.larsw.webdav.core.web.WebDavHandlerMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Core Spring WebDAV configuration.
 *
 * <p>Registers all RFC 4918 method handlers and wires the handler mapping.
 * Delegates to any {@link WebDavConfigurer} beans for customisation.
 * Default implementations are provided for the lock manager and property store;
 * the resource store must be supplied by the application or a provider module.
 */
@Configuration(proxyBeanMethods = false)
public class WebDavCoreConfiguration {

    private final String davPrefix;
    private final WebDavResourceStore resourceStore;
    private final WebDavLockManager lockManager;
    private final WebDavPropertyStore propertyStore;

    public WebDavCoreConfiguration(ObjectProvider<WebDavConfigurer> configurers,
                                    ObjectProvider<WebDavResourceStore> storeProvider,
                                    ObjectProvider<WebDavLockManager> lockProvider,
                                    ObjectProvider<WebDavPropertyStore> propertyProvider) {

        WebDavConfigurer configurer = configurers.getIfAvailable();

        this.davPrefix = configurer != null ? configurer.getDavPrefix() : "";

        WebDavResourceStore storeOverride = configurer != null ? configurer.resourceStore() : null;
        this.resourceStore = storeOverride != null ? storeOverride : storeProvider.getIfAvailable();

        WebDavLockManager lockOverride = configurer != null ? configurer.lockManager() : null;
        this.lockManager = lockOverride != null ? lockOverride
                : lockProvider.getIfAvailable(InMemoryLockManager::new);

        WebDavPropertyStore propOverride = configurer != null ? configurer.propertyStore() : null;
        this.propertyStore = propOverride != null ? propOverride
                : propertyProvider.getIfAvailable(DefaultWebDavPropertyStore::new);
    }

    // ---- Method Handlers ---------------------------------------------------

    @Bean
    public OptionsMethodHandler optionsMethodHandler() {
        return new OptionsMethodHandler();
    }

    @Bean
    public PropFindMethodHandler propFindMethodHandler() {
        return new PropFindMethodHandler(resourceStore, propertyStore, lockManager, davPrefix);
    }

    @Bean
    public PropPatchMethodHandler propPatchMethodHandler() {
        return new PropPatchMethodHandler(resourceStore, propertyStore, davPrefix);
    }

    @Bean
    public MkColMethodHandler mkColMethodHandler() {
        return new MkColMethodHandler(resourceStore, davPrefix);
    }

    @Bean
    public GetMethodHandler getMethodHandler() {
        return new GetMethodHandler(resourceStore, davPrefix, false);
    }

    @Bean
    public GetMethodHandler headMethodHandler() {
        return new GetMethodHandler(resourceStore, davPrefix, true);
    }

    @Bean
    public PutMethodHandler putMethodHandler() {
        return new PutMethodHandler(resourceStore, lockManager, davPrefix);
    }

    @Bean
    public DeleteMethodHandler deleteMethodHandler() {
        return new DeleteMethodHandler(resourceStore, lockManager, propertyStore, davPrefix);
    }

    @Bean
    public CopyMethodHandler copyMethodHandler() {
        return new CopyMethodHandler(resourceStore, propertyStore, davPrefix);
    }

    @Bean
    public MoveMethodHandler moveMethodHandler() {
        return new MoveMethodHandler(resourceStore, lockManager, propertyStore, davPrefix);
    }

    @Bean
    public LockMethodHandler lockMethodHandler() {
        return new LockMethodHandler(resourceStore, lockManager, davPrefix);
    }

    @Bean
    public UnlockMethodHandler unlockMethodHandler() {
        return new UnlockMethodHandler(lockManager, davPrefix);
    }

    // ---- Dispatcher & Mapping ----------------------------------------------

    @Bean
    public WebDavHttpRequestHandler webDavHttpRequestHandler(List<WebDavMethodHandler> handlers) {
        return new WebDavHttpRequestHandler(handlers);
    }

    @Bean
    public WebDavHandlerMapping webDavHandlerMapping(WebDavHttpRequestHandler handler) {
        return new WebDavHandlerMapping(handler, davPrefix.isEmpty() ? "/" : davPrefix);
    }
}

