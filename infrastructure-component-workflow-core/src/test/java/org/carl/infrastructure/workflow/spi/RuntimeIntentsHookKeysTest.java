package org.carl.infrastructure.workflow.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.handlers.RuntimeIntents;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class RuntimeIntentsHookKeysTest {

    @Test
    void hookPhaseExistsAndIsNonEmpty() {
        assertHookKey("HOOK_PHASE");
    }

    @Test
    void hookPayloadExistsAndIsNonEmpty() {
        assertHookKey("HOOK_PAYLOAD");
    }

    @Test
    void hookInterceptorsExistsAndIsNonEmpty() {
        assertHookKey("HOOK_INTERCEPTORS");
    }

    private static void assertHookKey(String fieldName) {
        Field field;
        try {
            field = RuntimeIntents.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("RuntimeIntents must declare a field named " + fieldName, e);
        }

        int mods = field.getModifiers();
        assertTrue(Modifier.isPublic(mods), fieldName + " must be public");
        assertTrue(Modifier.isStatic(mods), fieldName + " must be static");
        assertTrue(Modifier.isFinal(mods), fieldName + " must be final");
        assertEquals(String.class, field.getType(), fieldName + " must be a String");

        Object value;
        try {
            value = field.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Failed to read " + fieldName, e);
        }
        assertNotNull(value, fieldName + " value must not be null");
        String str = (String) value;
        assertFalse(str.isBlank(), fieldName + " value must not be blank");
    }
}
