package io.github.larsw.webdav.core.xml;

import javax.xml.namespace.QName;

/** WebDAV namespace constants (RFC 4918). */
public final class DavNamespaces {

    public static final String DAV_NS = "DAV:";
    public static final String DAV_PREFIX = "D";

    // Live property QNames
    public static final QName RESOURCETYPE       = new QName(DAV_NS, "resourcetype");
    public static final QName DISPLAYNAME         = new QName(DAV_NS, "displayname");
    public static final QName CREATIONDATE        = new QName(DAV_NS, "creationdate");
    public static final QName GETLASTMODIFIED     = new QName(DAV_NS, "getlastmodified");
    public static final QName GETCONTENTLENGTH    = new QName(DAV_NS, "getcontentlength");
    public static final QName GETCONTENTTYPE      = new QName(DAV_NS, "getcontenttype");
    public static final QName GETETAG             = new QName(DAV_NS, "getetag");
    public static final QName LOCKDISCOVERY       = new QName(DAV_NS, "lockdiscovery");
    public static final QName SUPPORTEDLOCK       = new QName(DAV_NS, "supportedlock");

    // Lock XML element names
    public static final String LOCKINFO           = "lockinfo";
    public static final String LOCKSCOPE          = "lockscope";
    public static final String LOCKTYPE           = "locktype";
    public static final String EXCLUSIVE          = "exclusive";
    public static final String SHARED             = "shared";
    public static final String WRITE              = "write";
    public static final String OWNER              = "owner";
    public static final String ACTIVELOCK         = "activelock";
    public static final String LOCKTOKEN          = "locktoken";
    public static final String DEPTH              = "depth";
    public static final String TIMEOUT            = "timeout";
    public static final String HREF               = "href";

    // HTTP methods not in standard javax.servlet.http.HttpMethod
    public static final String METHOD_PROPFIND    = "PROPFIND";
    public static final String METHOD_PROPPATCH   = "PROPPATCH";
    public static final String METHOD_MKCOL       = "MKCOL";
    public static final String METHOD_COPY        = "COPY";
    public static final String METHOD_MOVE        = "MOVE";
    public static final String METHOD_LOCK        = "LOCK";
    public static final String METHOD_UNLOCK      = "UNLOCK";

    private DavNamespaces() {}
}

