package org.carl.infrastructure.cache.remote;

import org.carl.infrastructure.comment.Conversion;
import org.junit.jupiter.api.Test;

class ConversionToTest {

    @Test
    void to() {
        String json =
                """
                {a:1}
                """;
        Conversion conversionTo = Conversion.create(json);
    }

    @Test
    void toJsonString() {}

    @Test
    void toJsonObject() {}

    @Test
    void testTo() {}
}
