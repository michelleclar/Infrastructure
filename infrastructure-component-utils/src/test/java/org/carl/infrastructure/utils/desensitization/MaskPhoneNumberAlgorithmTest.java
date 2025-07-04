package org.carl.infrastructure.utils.desensitization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MaskPhoneNumberAlgorithmTest {

    @Test
    void desensitize() {
        MaskPhoneNumberAlgorithm maskPhoneNumberAlgorithm = MaskPhoneNumberAlgorithm.INSTANCE;
        Assertions.assertEquals("152****7256", maskPhoneNumberAlgorithm.desensitize("15211117256"));
        Assertions.assertEquals("123456789", maskPhoneNumberAlgorithm.desensitize("123456789"));
    }
}
