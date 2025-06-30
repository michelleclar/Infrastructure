package org.carl.infrastructure.tool.config.desensitization;

/**
 * if email is illegal, return source
 * if email is short,return source
 */
public enum MaskEmailAlgorithm implements IDesensitizationAlgorithm {
    INSTANCE;

    @Override
    public String desensitize(String source) {
        if (source == null || !source.contains("@")) {
            return source;
        }
        String[] parts = source.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 1) {
            return source;
        }

        return localPart.charAt(0)
                + "*".repeat(localPart.length() - 2)
                + localPart.charAt(localPart.length() - 1)
                + "@"
                + domainPart;
    }
}
