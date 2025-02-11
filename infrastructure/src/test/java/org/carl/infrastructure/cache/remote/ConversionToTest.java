package org.carl.infrastructure.cache.remote;

import org.carl.infrastructure.util.Conversion;
import org.carl.infrastructure.util.gen.DatabaseGenerator;
import org.junit.jupiter.api.Test;

class ConversionToTest {

    @Test
    void to() {
        String json =
                """
                {a:1}
                """;
        Conversion conversionTo = Conversion.create(json);
        System.out.println(DatabaseGenerator.class.getName());
    }

    @Test
    void toJsonString() {}

    @Test
    void toJsonObject() {}

    @Test
    void testTo() {}
}
