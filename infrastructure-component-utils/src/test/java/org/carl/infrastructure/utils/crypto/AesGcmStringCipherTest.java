package org.carl.infrastructure.utils.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class AesGcmStringCipherTest {

    @Test
    void encryptsVersionedCiphertextAndDecrypts() {
        byte[] key = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
        AesGcmStringCipher cipher = new AesGcmStringCipher();

        String encrypted = cipher.encrypt("secret text", key);

        assertTrue(encrypted.startsWith("v1:"));
        assertEquals("secret text", cipher.decrypt(encrypted, key));
    }

    @Test
    void rejectsInvalidKeyLength() {
        AesGcmStringCipher cipher = new AesGcmStringCipher();

        assertThrows(CryptoException.class, () -> cipher.encrypt("secret", new byte[] {1, 2, 3}));
    }
}
