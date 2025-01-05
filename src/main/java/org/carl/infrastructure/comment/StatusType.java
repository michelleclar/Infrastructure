package org.carl.infrastructure.comment;

import jakarta.ws.rs.core.Response;

public enum StatusType implements Response.StatusType {
    ERROR_DUPLICATE(467, "Invalid email format");
    private final int code;
    private final String reason;
    private final Response.Status.Family family;

    StatusType(final int statusCode, final String reasonPhrase) {
        this.code = statusCode;
        this.reason = reasonPhrase;
        this.family = Response.Status.Family.familyOf(statusCode);
    }

    /**
     * Get the class of status code.
     *
     * @return the class of status code.
     */
    @Override
    public Response.Status.Family getFamily() {
        return family;
    }

    /**
     * Get the associated status code.
     *
     * @return the status code.
     */
    @Override
    public int getStatusCode() {
        return code;
    }

    /**
     * Get the reason phrase.
     *
     * @return the reason phrase.
     */
    @Override
    public String getReasonPhrase() {
        return toString();
    }

    @Override
    public String toString() {
        return reason;
    }
}
