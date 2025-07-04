package org.carl.infrastructure.utils.desensitization;

/**
 * ab -> ab
 *
 * <p>abc -> a*c
 */
public enum MaskMiddleAlgorithm implements IDesensitizationAlgorithm {
    INSTANCE;

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() <= 2) {
            return source;
        }
        int length = source.length();
        return source.charAt(0) + "*".repeat(length - 2) + source.charAt(length - 1);
    }
}
