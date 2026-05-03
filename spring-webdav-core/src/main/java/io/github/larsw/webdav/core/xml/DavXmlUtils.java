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

package io.github.larsw.webdav.core.xml;

import io.github.larsw.webdav.core.model.LockInfo;
import io.github.larsw.webdav.core.model.LockScope;
import io.github.larsw.webdav.core.model.LockType;
import io.github.larsw.webdav.core.model.PropFindRequest;
import io.github.larsw.webdav.core.spi.WebDavResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static io.github.larsw.webdav.core.xml.DavNamespaces.*;

/**
 * Utility methods for reading and writing WebDAV XML payloads.
 * Uses the JDK's built-in DOM/JAXP APIs — no additional dependencies required.
 */
public final class DavXmlUtils {

    private static final DateTimeFormatter ISO8601 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter RFC1123 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                    .withZone(ZoneOffset.UTC)
                    .withLocale(java.util.Locale.US);

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    /** Parses a PROPFIND request body. If the body is empty/null, defaults to allprop. */
    public static PropFindRequest parsePropFind(InputStream body) throws IOException {
        PropFindRequest req = new PropFindRequest();
        if (body == null) return req;

        Document doc = parse(body);
        if (doc == null) return req;

        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            switch (child.getLocalName()) {
                case "allprop"  -> req.setType(PropFindRequest.Type.ALLPROP);
                case "propname" -> req.setType(PropFindRequest.Type.PROPNAME);
                case "prop"     -> {
                    req.setType(PropFindRequest.Type.PROP);
                    NodeList props = child.getChildNodes();
                    for (int j = 0; j < props.getLength(); j++) {
                        Node prop = props.item(j);
                        if (prop.getNodeType() == Node.ELEMENT_NODE) {
                            req.getRequestedProperties().add(
                                    new QName(prop.getNamespaceURI(), prop.getLocalName()));
                        }
                    }
                }
            }
        }
        return req;
    }

    /** Parses a LOCK request body into a {@link LockInfo} seed object. */
    public static LockInfo parseLockInfo(InputStream body) throws IOException {
        LockInfo info = new LockInfo();
        if (body == null) return info;

        Document doc = parse(body);
        if (doc == null) return info;

        Element root = doc.getDocumentElement(); // D:lockinfo

        // lockscope
        Element scopeEl = firstChild(root, LOCKSCOPE);
        if (scopeEl != null) {
            if (firstChild(scopeEl, EXCLUSIVE) != null) info.setScope(LockScope.EXCLUSIVE);
            else if (firstChild(scopeEl, SHARED) != null) info.setScope(LockScope.SHARED);
        }

        // locktype (RFC 4918 only defines write)
        info.setType(LockType.WRITE);

        // owner
        Element ownerEl = firstChild(root, OWNER);
        if (ownerEl != null) {
            Element hrefEl = firstChild(ownerEl, HREF);
            info.setOwner(hrefEl != null ? hrefEl.getTextContent().trim() : ownerEl.getTextContent().trim());
        }

        return info;
    }

    // -----------------------------------------------------------------------
    // Writing — Multi-Status (207)
    // -----------------------------------------------------------------------

    /**
     * Writes a DAV:multistatus document describing the given resources.
     * Each resource is represented as a DAV:response element.
     */
    public static void writeMultiStatus(
            OutputStream out,
            java.util.List<WebDavResource> resources,
            PropFindRequest request,
            Map<String, Map<QName, String>> deadProps,
            Map<String, LockInfo> locks,
            String pathPrefix) throws IOException {

        Document doc = newDocument();
        Element multistatus = elem(doc, null, "multistatus");
        doc.appendChild(multistatus);

        for (WebDavResource resource : resources) {
            Element response = appendElem(doc, multistatus, "response");
            appendTextElem(doc, response, HREF, encodePath(pathPrefix, resource.getPath()));

            Element propstat = appendElem(doc, response, "propstat");
            Element prop = appendElem(doc, propstat, "prop");

            buildLiveProperties(doc, prop, resource, request, locks);

            // dead properties
            if (request.getType() != PropFindRequest.Type.PROPNAME) {
                Map<QName, String> dead = deadProps.getOrDefault(resource.getPath(), Map.of());
                for (Map.Entry<QName, String> entry : dead.entrySet()) {
                    QName qn = entry.getKey();
                    Element deadEl = doc.createElementNS(qn.getNamespaceURI(), qn.getLocalPart());
                    if (request.getType() == PropFindRequest.Type.ALLPROP
                            || request.getRequestedProperties().contains(qn)) {
                        deadEl.setTextContent(entry.getValue());
                        prop.appendChild(deadEl);
                    }
                }
            }

            appendTextElem(doc, propstat, "status", "HTTP/1.1 200 OK");
        }

        serialize(doc, out);
    }

    private static void buildLiveProperties(Document doc, Element prop, WebDavResource r,
                                             PropFindRequest req, Map<String, LockInfo> locks) {
        boolean all = req.getType() == PropFindRequest.Type.ALLPROP;
        boolean names = req.getType() == PropFindRequest.Type.PROPNAME;

        if (shouldInclude(req, RESOURCETYPE)) {
            Element rt = appendElem(doc, prop, "resourcetype");
            if (r.isCollection() && !names) appendElem(doc, rt, "collection");
        }
        if (shouldInclude(req, DISPLAYNAME)) {
            appendTextElem(doc, prop, "displayname", names ? "" : r.getName());
        }
        if (shouldInclude(req, CREATIONDATE)) {
            appendTextElem(doc, prop, "creationdate",
                    names ? "" : ISO8601.format(r.getCreationDate()));
        }
        if (shouldInclude(req, GETLASTMODIFIED)) {
            appendTextElem(doc, prop, "getlastmodified",
                    names ? "" : RFC1123.format(r.getLastModified()));
        }
        if (!r.isCollection()) {
            if (shouldInclude(req, GETCONTENTLENGTH)) {
                appendTextElem(doc, prop, "getcontentlength",
                        names ? "" : String.valueOf(r.getContentLength()));
            }
            if (shouldInclude(req, GETCONTENTTYPE)) {
                appendTextElem(doc, prop, "getcontenttype",
                        names ? "" : (r.getContentType() != null ? r.getContentType() : "application/octet-stream"));
            }
        }
        if (shouldInclude(req, GETETAG)) {
            appendTextElem(doc, prop, "getetag", names ? "" : "\"" + r.getETag() + "\"");
        }
        if (shouldInclude(req, SUPPORTEDLOCK)) {
            appendSupportedLock(doc, prop, names);
        }
        if (shouldInclude(req, LOCKDISCOVERY)) {
            Element ld = appendElem(doc, prop, "lockdiscovery");
            if (!names) {
                LockInfo lock = locks.get(r.getPath());
                if (lock != null && !lock.isExpired()) {
                    appendActiveLock(doc, ld, lock);
                }
            }
        }
    }

    private static boolean shouldInclude(PropFindRequest req, QName name) {
        return req.getType() == PropFindRequest.Type.ALLPROP
                || req.getType() == PropFindRequest.Type.PROPNAME
                || req.getRequestedProperties().contains(name);
    }

    // -----------------------------------------------------------------------
    // Writing — LOCK response
    // -----------------------------------------------------------------------

    public static void writeLockResponse(OutputStream out, LockInfo lock) throws IOException {
        Document doc = newDocument();
        Element prop = elem(doc, null, "prop");
        doc.appendChild(prop);
        Element ld = appendElem(doc, prop, "lockdiscovery");
        appendActiveLock(doc, ld, lock);
        serialize(doc, out);
    }

    private static void appendActiveLock(Document doc, Element parent, LockInfo lock) {
        Element al = appendElem(doc, parent, ACTIVELOCK);
        Element lt = appendElem(doc, al, LOCKTYPE);
        appendElem(doc, lt, WRITE);
        Element ls = appendElem(doc, al, LOCKSCOPE);
        appendElem(doc, ls, lock.getScope() == LockScope.EXCLUSIVE ? EXCLUSIVE : SHARED);
        appendTextElem(doc, al, DEPTH, lock.getDepth() == -1 ? "Infinity" : String.valueOf(lock.getDepth()));
        if (lock.getOwner() != null) {
            Element ownerEl = appendElem(doc, al, OWNER);
            appendTextElem(doc, ownerEl, HREF, lock.getOwner());
        }
        String timeout = lock.getTimeoutSeconds() == -1
                ? "Infinite" : "Second-" + lock.getTimeoutSeconds();
        appendTextElem(doc, al, TIMEOUT, timeout);
        if (lock.getLockToken() != null) {
            Element tokenEl = appendElem(doc, al, LOCKTOKEN);
            appendTextElem(doc, tokenEl, HREF, lock.getLockToken());
        }
    }

    private static void appendSupportedLock(Document doc, Element parent, boolean namesOnly) {
        Element sl = appendElem(doc, parent, "supportedlock");
        if (!namesOnly) {
            // Exclusive write lock
            Element le = appendElem(doc, sl, "lockentry");
            Element lt = appendElem(doc, le, LOCKTYPE);
            appendElem(doc, lt, WRITE);
            Element ls = appendElem(doc, le, LOCKSCOPE);
            appendElem(doc, ls, EXCLUSIVE);
            // Shared write lock
            Element le2 = appendElem(doc, sl, "lockentry");
            Element lt2 = appendElem(doc, le2, LOCKTYPE);
            appendElem(doc, lt2, WRITE);
            Element ls2 = appendElem(doc, le2, LOCKSCOPE);
            appendElem(doc, ls2, SHARED);
        }
    }

    // -----------------------------------------------------------------------
    // Writing — PROPPATCH 207 response
    // -----------------------------------------------------------------------

    public static void writePropPatchResponse(OutputStream out, String href,
                                               Map<QName, Integer> results) throws IOException {
        Document doc = newDocument();
        Element ms = elem(doc, null, "multistatus");
        doc.appendChild(ms);
        Element response = appendElem(doc, ms, "response");
        appendTextElem(doc, response, HREF, href);

        // Group results by status code
        Map<Integer, java.util.List<QName>> byStatus = new java.util.LinkedHashMap<>();
        results.forEach((qn, code) -> byStatus.computeIfAbsent(code, k -> new java.util.ArrayList<>()).add(qn));
        byStatus.forEach((code, qnames) -> {
            Element propstat = appendElem(doc, response, "propstat");
            Element prop = appendElem(doc, propstat, "prop");
            qnames.forEach(qn -> prop.appendChild(
                    doc.createElementNS(qn.getNamespaceURI(), qn.getLocalPart())));
            appendTextElem(doc, propstat, "status", "HTTP/1.1 " + code + " " + statusText(code));
        });

        serialize(doc, out);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static Document parse(InputStream in) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // XXE protection
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse WebDAV XML body", e);
        }
    }

    private static Document newDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Element elem(Document doc, Element parent, String localName) {
        Element el = doc.createElementNS(DAV_NS, DAV_PREFIX + ":" + localName);
        if (parent != null) parent.appendChild(el);
        return el;
    }

    private static Element appendElem(Document doc, Element parent, String localName) {
        return elem(doc, parent, localName);
    }

    private static void appendTextElem(Document doc, Element parent, String localName, String text) {
        Element el = appendElem(doc, parent, localName);
        el.setTextContent(text);
    }

    private static void appendTextElem(Document doc, Element parent, QName qn, String text) {
        Element el = doc.createElementNS(qn.getNamespaceURI(), qn.getLocalPart());
        el.setTextContent(text);
        parent.appendChild(el);
    }

    private static Element firstChild(Element parent, String localName) {
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && localName.equals(n.getLocalName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private static void serialize(Document doc, OutputStream out) throws IOException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.transform(new DOMSource(doc), new StreamResult(out));
        } catch (TransformerException e) {
            throw new IOException("Failed to serialize WebDAV XML", e);
        }
    }

    private static String encodePath(String prefix, String path) {
        // Normalize prefix and path concatenation
        if (prefix == null) prefix = "";
        if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
        if (!path.startsWith("/")) path = "/" + path;
        return prefix + path;
    }

    private static String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 424 -> "Failed Dependency";
            default -> "Error";
        };
    }

    private DavXmlUtils() {}
}

