package io.github.larsw.webdav.core.config;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Imported by {@link io.github.larsw.webdav.core.annotation.EnableWebDav} to register
 * the core WebDAV configuration class.
 */
public class WebDavConfigurationSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{ WebDavCoreConfiguration.class.getName() };
    }
}

