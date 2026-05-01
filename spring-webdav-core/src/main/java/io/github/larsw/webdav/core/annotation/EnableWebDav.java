package io.github.larsw.webdav.core.annotation;

import io.github.larsw.webdav.core.config.WebDavConfigurationSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables Spring WebDAV support in the application context.
 *
 * <p>Add this annotation to a {@code @Configuration} class to activate the WebDAV
 * framework. In Spring Boot applications this is done automatically by the
 * autoconfiguration; explicit use is only needed in non-Boot Spring applications.
 *
 * <pre>{@code
 * @EnableWebDav
 * @Configuration
 * public class AppConfig { }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(WebDavConfigurationSelector.class)
public @interface EnableWebDav {
}

