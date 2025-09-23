package org.carl.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;

public class SimHash {
    public static BigInteger computeSimHash(String text) {
        int[] v = new int[64];
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            BigInteger hash = hash(token);
            for (int i = 0; i < 64; i++) {
                if (hash.testBit(i)) {
                    v[i] += 1;
                } else {
                    v[i] -= 1;
                }
            }
        }

        BigInteger fingerprint = BigInteger.ZERO;
        for (int i = 0; i < 64; i++) {
            if (v[i] > 0) {
                fingerprint = fingerprint.setBit(i);
            }
        }
        return fingerprint;
    }

    private static BigInteger hash(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(source.getBytes());
            return new BigInteger(1, Arrays.copyOf(bytes, 8)); // only 64-bit
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int hammingDistance(BigInteger a, BigInteger b) {
        return a.xor(b).bitCount();
    }
}
