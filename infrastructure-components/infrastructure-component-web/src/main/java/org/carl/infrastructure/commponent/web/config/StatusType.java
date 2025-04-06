package org.carl.infrastructure.commponent.web.config;

import jakarta.ws.rs.core.Response;

// TODO: need code ,reason ,change family
public class StatusType implements Response.StatusType {
    public static final StatusType ERROR_DUPLICATE = StatusType.create(467, "ERROR_DUPLICATE");
    private final int code;
    private final String reason;
    private final Response.Status.Family family;

    StatusType(final int statusCode, final String reasonPhrase) {
        this.code = statusCode;
        this.reason = reasonPhrase;
        this.family = Response.Status.Family.familyOf(statusCode);
    }

    public static StatusType create(final int statusCode, final String reasonPhrase) {
        return new StatusType(statusCode, reasonPhrase);
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
