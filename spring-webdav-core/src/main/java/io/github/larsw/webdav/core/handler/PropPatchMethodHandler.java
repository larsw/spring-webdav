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

package io.github.larsw.webdav.core.handler;

import io.github.larsw.webdav.core.spi.WebDavPropertyStore;
import io.github.larsw.webdav.core.spi.WebDavResourceStore;
import io.github.larsw.webdav.core.xml.DavXmlUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

import static io.github.larsw.webdav.core.xml.DavNamespaces.METHOD_PROPPATCH;

/**
 * Handles PROPPATCH (RFC 4918, Section 9.2) — sets and removes dead properties.
 */
public class PropPatchMethodHandler implements WebDavMethodHandler {

    private static final int SC_MULTI_STATUS = 207;

    private final WebDavResourceStore store;
    private final WebDavPropertyStore propertyStore;
    private final String davPrefix;

    public PropPatchMethodHandler(WebDavResourceStore store,
                                   WebDavPropertyStore propertyStore,
                                   String davPrefix) {
        this.store = store;
        this.propertyStore = propertyStore;
        this.davPrefix = davPrefix;
    }

    @Override
    public String getMethod() { return METHOD_PROPPATCH; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = DavPathUtils.extractDavPath(request, davPrefix);
        String href = davPrefix + path;

        if (store.getResource(path).isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Map<QName, String> toSet = new LinkedHashMap<>();
        Set<QName> toRemove = new LinkedHashSet<>();
        parseProppatch(request, toSet, toRemove);

        Map<QName, Integer> results = new LinkedHashMap<>();

        if (!toSet.isEmpty()) {
            propertyStore.setProperties(path, toSet);
            toSet.keySet().forEach(qn -> results.put(qn, 200));
        }
        if (!toRemove.isEmpty()) {
            propertyStore.removeProperties(path, toRemove);
            toRemove.forEach(qn -> results.put(qn, 200));
        }

        response.setStatus(SC_MULTI_STATUS);
        response.setContentType("application/xml; charset=UTF-8");
        DavXmlUtils.writePropPatchResponse(response.getOutputStream(), href, results);
    }

    private void parseProppatch(HttpServletRequest request,
                                  Map<QName, String> toSet,
                                  Set<QName> toRemove) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(request.getInputStream());
            NodeList children = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                String localName = node.getLocalName();
                NodeList propNodes = ((Element) node).getElementsByTagNameNS("DAV:", "prop");
                if (propNodes.getLength() == 0) continue;
                NodeList props = propNodes.item(0).getChildNodes();
                for (int j = 0; j < props.getLength(); j++) {
                    Node prop = props.item(j);
                    if (prop.getNodeType() != Node.ELEMENT_NODE) continue;
                    QName qn = new QName(prop.getNamespaceURI(), prop.getLocalName());
                    if ("set".equals(localName)) {
                        toSet.put(qn, prop.getTextContent());
                    } else if ("remove".equals(localName)) {
                        toRemove.add(qn);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse PROPPATCH body", e);
        }
    }
}

