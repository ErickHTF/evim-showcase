package com.example.EVIM.domain.service;

import java.nio.charset.StandardCharsets;

public final class TextNormalizer {

    private TextNormalizer() {}

    public static String fixDoubleEncodedUtf8(String text) {
        if (text == null || text.isEmpty()) return text;

        int signalsBefore = countMojibakeSignals(text);
        if (signalsBefore == 0) return text;

        try {
            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            String decoded = new String(bytes, StandardCharsets.UTF_8);

            if (decoded.contains("\uFFFD")) return text;

            int signalsAfter = countMojibakeSignals(decoded);
            if (signalsAfter < signalsBefore) {
                return decoded;
            }
        } catch (Exception ignored) {
        }
        return text;
    }

    private static int countMojibakeSignals(String s) {
        int count = 0;
        int len = s.length();
        for (int i = 0; i < len - 1; i++) {
            char c = s.charAt(i);
            if (c == 'Ã' || c == 'Â') {
                char n = s.charAt(i + 1);
                if (n >= 0x80 && n <= 0xBF) count++;
            }
        }
        return count;
    }
}
