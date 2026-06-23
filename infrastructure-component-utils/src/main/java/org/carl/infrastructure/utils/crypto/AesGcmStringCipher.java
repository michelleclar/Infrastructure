package org.carl.infrastructure.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmStringCipher {

    public static final String VERSION = "v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SecureRandom secureRandom;

    public AesGcmStringCipher() {
        this(new SecureRandom());
    }

    public AesGcmStringCipher(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    public String encrypt(String plaintext, byte[] key) {
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(key), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + ENCODER.encodeToString(iv) + ":" + ENCODER.encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("AES-GCM encryption failed", e);
        }
    }

    public String decrypt(String ciphertext, byte[] key) {
        VersionedCiphertext parsed = VersionedCiphertext.parse(ciphertext);
        if (!VERSION.equals(parsed.version())) {
            throw new CryptoException("Unsupported AES-GCM ciphertext version");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec(key),
                    new GCMParameterSpec(TAG_LENGTH_BITS, DECODER.decode(parsed.iv())));
            byte[] decrypted = cipher.doFinal(DECODER.decode(parsed.payload()));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CryptoException("AES-GCM decryption failed", e);
        }
    }

    private SecretKeySpec keySpec(byte[] key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new CryptoException("AES key must be 16, 24, or 32 bytes");
        }
        return new SecretKeySpec(key.clone(), "AES");
    }
}
