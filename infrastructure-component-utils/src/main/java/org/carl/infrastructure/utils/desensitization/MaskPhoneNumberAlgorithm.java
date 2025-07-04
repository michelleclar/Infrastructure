package org.carl.infrastructure.utils.desensitization;

/** 15211117256 -> 152****7256 */
public enum MaskPhoneNumberAlgorithm implements IDesensitizationAlgorithm {
    INSTANCE;

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() != 11) {
            return source;
        }
        return source.substring(0, 3) + "****" + source.substring(7);
    }
}
