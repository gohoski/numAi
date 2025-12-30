package io.github.gohoski.numai;

/**
 * Created by Gleb on 14.12.2025.
 *
 * Minimal Base64 encoder based on public domain Base64 code by Robert Harder
 */
class Base64 {
    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    static String encode(byte[] source) {
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
    } private Base64() {}
}