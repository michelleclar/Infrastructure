package org.carl.utils;

import java.io.ByteArrayOutputStream;

// 安卓 base64
public class Base64Utils {
    private static final char[] BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    // 简易 Base64 编码实现（仅作示例，实际项目建议使用标准库）
    public static String encode(byte[] input) {
        if (input == null) return null;

        int length = input.length;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i += 3) {
            int b0 = input[i] & 0xFF;
            int b1 = (i + 1 < length) ? input[i + 1] & 0xFF : 0;
            int b2 = (i + 2 < length) ? input[i + 2] & 0xFF : 0;

            int combined = (b0 << 16) | (b1 << 8) | b2;

            result.append(BASE64_ALPHABET[(combined >> 18) & 0x3F]);
            result.append(BASE64_ALPHABET[(combined >> 12) & 0x3F]);

            if (i + 1 < length) result.append(BASE64_ALPHABET[(combined >> 6) & 0x3F]);
            else result.append('=');

            if (i + 2 < length) result.append(BASE64_ALPHABET[combined & 0x3F]);
            else result.append('=');
        }

        return result.toString();
    }

    // 简易 Base64 解码实现
    public static byte[] decode(String base64) {
        if (base64 == null) return null;

        base64 = base64.replaceAll("[^A-Za-z0-9+/=]", "");
        int length = base64.length();
        if (length % 4 != 0) {
            throw new IllegalArgumentException("Invalid Base64 string");
        }

        int padding = 0;
        if (length >= 2) {
            if (base64.charAt(length - 1) == '=') padding++;
            if (base64.charAt(length - 2) == '=') padding++;
        }

        int outputLength = (length * 3) / 4 - padding;
        ByteArrayOutputStream out = new ByteArrayOutputStream(outputLength);

        for (int i = 0; i < length; i += 4) {
            int c0 = indexOf(base64.charAt(i));
            int c1 = indexOf(base64.charAt(i + 1));
            int c2 = (i + 2 < length) ? indexOf(base64.charAt(i + 2)) : 0;
            int c3 = (i + 3 < length) ? indexOf(base64.charAt(i + 3)) : 0;

            int combined = (c0 << 18) | (c1 << 12) | (c2 << 6) | c3;

            out.write((combined >> 16) & 0xFF);
            if (i + 2 < length) out.write((combined >> 8) & 0xFF);
            if (i + 3 < length) out.write(combined & 0xFF);
        }

        return out.toByteArray();
    }

    private static int indexOf(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return 0; // 无效字符，应该抛出异常，但简化处理
    }
}
