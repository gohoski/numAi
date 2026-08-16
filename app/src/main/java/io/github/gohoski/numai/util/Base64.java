package io.github.gohoski.numai.util;

import java.io.ByteArrayOutputStream;

public class Base64 {
    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    public static String encode(byte[] source) {
        if (source == null) {
            return null;
        }

        int len = source.length;
        char[] out = new char[((len + 2) / 3) * 4];

        int i = 0;
        int outIdx = 0;

        while (i < len - 2) {
            int b1 = source[i++] & 0xFF;
            int b2 = source[i++] & 0xFF;
            int b3 = source[i++] & 0xFF;

            int val = (b1 << 16) | (b2 << 8) | b3;

            out[outIdx++] = ALPHABET[(val >>> 18) & 0x3F];
            out[outIdx++] = ALPHABET[(val >>> 12) & 0x3F];
            out[outIdx++] = ALPHABET[(val >>> 6) & 0x3F];
            out[outIdx++] = ALPHABET[val & 0x3F];
        }

        if (i < len) {
            int b1 = source[i++] & 0xFF;
            int b2 = (i < len) ? source[i] & 0xFF : 0;

            int val = (b1 << 16) | (b2 << 8);

            out[outIdx++] = ALPHABET[(val >>> 18) & 0x3F];
            out[outIdx++] = ALPHABET[(val >>> 12) & 0x3F];

            out[outIdx++] = (i < len) ? ALPHABET[(val >>> 6) & 0x3F] : '=';
            out[outIdx++] = '=';
        }

        return new String(out);
    }

    /**
     * Decodes standard Base64 without relying on android.util.Base64, which is
     * unavailable on the Android versions supported by numAi.
     */
    public static byte[] decode(String source) {
        if (source == null) {
            return null;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream((source.length() * 3) / 4);
        int accumulator = 0;
        int bits = 0;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '=') {
                break;
            }
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                continue;
            }

            int value = decodeCharacter(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base64 character");
            }

            accumulator = (accumulator << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                output.write((accumulator >> bits) & 0xFF);
                if (bits == 0) {
                    accumulator = 0;
                } else {
                    accumulator &= (1 << bits) - 1;
                }
            }
        }
        return output.toByteArray();
    }

    private static int decodeCharacter(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    }

    private Base64() {}
}
