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

