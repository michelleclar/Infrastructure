package org.carl.infrastructure.pdp;

public record PolicyRequest(
        String subject,
        String action,
        String resource
) {
}
