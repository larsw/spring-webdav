package io.github.larsw.webdav.core.impl;

import io.github.larsw.webdav.core.spi.WebDavPropertyStore;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link WebDavPropertyStore}.
 *
 * <p>Dead property state is lost on restart. Replace with a persistent implementation
 * (e.g. JDBC, embedded DB) for production use.
 */
public class DefaultWebDavPropertyStore implements WebDavPropertyStore {

    /** path → (QName → value) */
    private final Map<String, Map<QName, String>> store = new ConcurrentHashMap<>();

    @Override
    public Map<QName, String> getProperties(String path) {
        return Map.copyOf(store.getOrDefault(path, Map.of()));
    }

    @Override
    public void setProperties(String path, Map<QName, String> properties) {
        store.computeIfAbsent(path, k -> new ConcurrentHashMap<>()).putAll(properties);
    }

    @Override
    public void removeProperties(String path, Set<QName> propertyNames) {
        Map<QName, String> props = store.get(path);
        if (props != null) propertyNames.forEach(props::remove);
    }

    @Override
    public void onDelete(String path) {
        // Remove properties for the path and all descendant paths
        store.keySet().removeIf(k -> k.equals(path) || k.startsWith(path + "/"));
    }

    @Override
    public void onMove(String sourcePath, String destPath) {
        Map<String, Map<QName, String>> moved = new HashMap<>();
        store.entrySet().removeIf(e -> {
            if (e.getKey().equals(sourcePath) || e.getKey().startsWith(sourcePath + "/")) {
                String newKey = destPath + e.getKey().substring(sourcePath.length());
                moved.put(newKey, e.getValue());
                return true;
            }
            return false;
        });
        store.putAll(moved);
    }
}

