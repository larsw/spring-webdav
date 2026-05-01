package io.github.larsw.webdav.core.spi;

/**
 * Unchecked exception thrown by WebDAV store operations.
 * Carries an HTTP status code that is forwarded directly to the client.
 */
public class WebDavException extends RuntimeException {

    private final int statusCode;

    public WebDavException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public WebDavException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** The HTTP status code to return to the client (e.g. 404, 409, 412, 423). */
    public int getStatusCode() {
        return statusCode;
    }
}

