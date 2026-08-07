package org.carl.infra.discover.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.carl.infra.discover.DynamicConfigSnapshot;
import org.carl.infra.discover.DynamicConfigValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConsulConfigParserTest {

    @Test
    void parsesPropertiesAndRunsValidators() {
        DynamicConfigSnapshot snapshot =
                ConsulConfigParser.parse(
                        "feature.enabled=true\nlimit=12\n",
                        "41",
                        List.of(config -> config.get("limit", Integer.class)));

        assertEquals("41", snapshot.version());
        assertEquals(true, snapshot.get("feature.enabled", Boolean.class));
        assertEquals(12, snapshot.get("limit", Integer.class));
    }

    @Test
    void rejectsFailedValidation() {
        assertThrows(
                DynamicConfigValidationException.class,
                () ->
                        ConsulConfigParser.parse(
                                "limit=wrong\n",
                                "42",
                                List.of(config -> config.get("limit", Integer.class))));
    }
}
