package org.carl.infrastructure.utils.desensitization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MaskEmailAlgorithmTest {

    @Test
    void desensitize() {
        Assertions.assertEquals(
                "2********2@qq.com", MaskEmailAlgorithm.INSTANCE.desensitize("2********2@qq.com"));
    }
}
