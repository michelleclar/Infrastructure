package org.carl.infrastructure.utils.desensitization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MaskMiddleAlgorithmTest {

    @Test
    void desensitize() {
        MaskMiddleAlgorithm maskMiddleAlgorithm = MaskMiddleAlgorithm.INSTANCE;
        Assertions.assertEquals("ac", maskMiddleAlgorithm.desensitize("ac"));
        Assertions.assertEquals("a*c", maskMiddleAlgorithm.desensitize("abc"));
    }
}
