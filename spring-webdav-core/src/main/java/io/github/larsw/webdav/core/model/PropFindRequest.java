package io.github.larsw.webdav.core.model;

import javax.xml.namespace.QName;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parsed representation of a PROPFIND request body (RFC 4918, Section 9.1).
 */
public class PropFindRequest {

    public enum Type {
        /** Return all live and dead properties. */
        ALLPROP,
        /** Return only property names (no values). */
        PROPNAME,
        /** Return the specified properties. */
        PROP
    }

    private Type type = Type.ALLPROP;
    private final Set<QName> requestedProperties = new LinkedHashSet<>();

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Set<QName> getRequestedProperties() { return requestedProperties; }
}

