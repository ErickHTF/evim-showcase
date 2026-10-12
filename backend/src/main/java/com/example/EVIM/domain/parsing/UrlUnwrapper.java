package com.example.EVIM.domain.parsing;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class UrlUnwrapper {

    private UrlUnwrapper() {}


    public static String unwrap(String url) {
        if (url == null || url.isBlank()) return url;
        String current = url.trim();
        for (int i = 0; i < 3; i++) {
            String next = unwrapOnce(current);
            if (next.equals(current)) return current;
            current = next;
        }
        return current;
    }

    private static String unwrapOnce(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return url;
        }
        String host = uri.getHost();
        if (host == null) return url;
        String h = host.toLowerCase(Locale.ROOT);

        if (h.endsWith(".safelinks.protection.outlook.com") || h.equals("safelinks.protection.outlook.com")) {
            return unwrapSafelinks(uri, url);
        }
        if (h.equals("urldefense.proofpoint.com") || h.equals("urldefense.com")) {




            int v3Idx = url.indexOf("/v3/__");
            if (v3Idx >= 0) {
                return unwrapProofpointV3(url, v3Idx, url);
            }
            return unwrapProofpoint(uri, url);
        }
        if (h.endsWith(".mimecast.com") && h.contains("protect")) {
            return unwrapMimecast(uri, url);
        }
        return url;
    }

    private static String unwrapSafelinks(URI uri, String fallback) {
        String target = queryParam(uri.getRawQuery(), "url");
        if (target == null || target.isBlank()) return fallback;
        return decode(target);
    }

    private static String unwrapProofpointV3(String fullUrl, int v3Idx, String fallback) {





        int start = v3Idx + "/v3/__".length();
        int end = fullUrl.indexOf("__;", start);
        if (end < 0) return fallback;
        String original = fullUrl.substring(start, end);
        return decode(original);
    }

    private static String unwrapProofpoint(URI uri, String fallback) {
        String raw = uri.getRawQuery();
        if (raw == null) return fallback;

        String u = queryParam(raw, "u");
        if (u != null && !u.isBlank()) {
            String restored = u.replace('-', '%').replace('_', '/');
            return decode(restored);
        }

        String url = queryParam(raw, "url");
        if (url != null && !url.isBlank()) return decode(url);
        return fallback;
    }

    private static String unwrapMimecast(URI uri, String fallback) {





        return fallback;
    }

    private static String queryParam(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            if (pair.substring(0, eq).equalsIgnoreCase(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s;
        }
    }
}
