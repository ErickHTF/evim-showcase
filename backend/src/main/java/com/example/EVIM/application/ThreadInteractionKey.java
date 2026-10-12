package com.example.EVIM.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class ThreadInteractionKey {

    private static final Pattern WS_RUN = Pattern.compile("\\s+");
    private static final HexFormat HEX = HexFormat.of();

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    });

    private ThreadInteractionKey() {
    }

    public static String of(String author, LocalDateTime sentAt, String body) {
        String a = author == null ? "" : author.trim().toLowerCase();
        String d = sentAt == null ? "" : sentAt.toString();
        String b = body == null ? "" : WS_RUN.matcher(body).replaceAll(" ").trim();
        String raw = a + "|" + d + "|" + b;
        try {
            MessageDigest md = SHA256.get();
            md.reset();
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (Exception e) {

            return Integer.toHexString(raw.hashCode()) + ":" + raw.length();
        }
    }
}
