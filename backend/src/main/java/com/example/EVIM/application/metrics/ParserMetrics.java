package com.example.EVIM.application.metrics;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class ParserMetrics {

    private static final int MAX_DOMAIN_CARDINALITY = 100;
    private static final String OTHER_DOMAIN = "other";
    private static final String UNKNOWN_DOMAIN = "unknown";

    private final AtomicLong success = new AtomicLong();
    private final AtomicLong fallback = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong dateUnparsed = new AtomicLong();
    private final AtomicLong messagesEmitted = new AtomicLong();

    private final ConcurrentHashMap<String, AtomicLong> successByDomain = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> fallbackByDomain = new ConcurrentHashMap<>();

    private volatile Instant since = Instant.now();

    public void recordSuccess(String senderDomain, int messageCount) {
        success.incrementAndGet();
        if (messageCount > 0) messagesEmitted.addAndGet(messageCount);
        bump(successByDomain, normalizeDomain(senderDomain));
    }

    public void recordFallback(String senderDomain) {
        fallback.incrementAndGet();
        bump(fallbackByDomain, normalizeDomain(senderDomain));
    }

    public void recordFailed() {
        failed.incrementAndGet();
    }

    public void recordDateUnparsed(int count) {
        if (count > 0) dateUnparsed.addAndGet(count);
    }

    public void record(ParseDiagnostic d) {
        if (d == null) return;
        switch (d.outcome()) {
            case SUCCESS -> {
                recordSuccess(d.senderDomain(), d.messageCount());
                recordDateUnparsed(d.unparsedDateCount());
            }
            case FALLBACK -> recordFallback(d.senderDomain());
            case FAILED -> recordFailed();
        }
    }

    public Map<String, Object> snapshot() {
        long s = success.get();
        long f = fallback.get();
        long total = s + f;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("since", since.toString());
        out.put("success", s);
        out.put("fallback", f);
        out.put("failed", failed.get());
        out.put("dateUnparsed", dateUnparsed.get());
        out.put("messagesEmitted", messagesEmitted.get());
        out.put("fallbackRatio", total == 0 ? 0.0 : ((double) f) / total);
        out.put("avgMessagesPerSuccess", s == 0 ? 0.0 : ((double) messagesEmitted.get()) / s);
        out.put("successByDomain", snapshotMap(successByDomain));
        out.put("fallbackByDomain", snapshotMap(fallbackByDomain));
        out.put("boilerplateByRule", snapshotMap(boilerplateByRule));
        return out;
    }

    public void reset() {
        success.set(0);
        fallback.set(0);
        failed.set(0);
        dateUnparsed.set(0);
        messagesEmitted.set(0);
        successByDomain.clear();
        fallbackByDomain.clear();
        boilerplateByRule.clear();
        since = Instant.now();
    }

    public static String extractDomain(String emailAddress) {
        if (emailAddress == null) return UNKNOWN_DOMAIN;
        int at = emailAddress.lastIndexOf('@');
        if (at < 0 || at == emailAddress.length() - 1) return UNKNOWN_DOMAIN;
        String d = emailAddress.substring(at + 1).trim().toLowerCase();
        return d.isEmpty() ? UNKNOWN_DOMAIN : d;
    }

    private void bump(ConcurrentHashMap<String, AtomicLong> map, String key) {
        AtomicLong existing = map.get(key);
        if (existing != null) {
            existing.incrementAndGet();
            return;
        }

        if (map.size() >= MAX_DOMAIN_CARDINALITY) {
            map.computeIfAbsent(OTHER_DOMAIN, k -> new AtomicLong()).incrementAndGet();
            return;
        }
        map.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    private String normalizeDomain(String domain) {
        return Objects.requireNonNullElse(domain, UNKNOWN_DOMAIN).toLowerCase();
    }

    private Map<String, Long> snapshotMap(ConcurrentHashMap<String, AtomicLong> map) {
        Map<String, Long> copy = new LinkedHashMap<>(map.size());
        map.forEach((k, v) -> copy.put(k, v.get()));
        return copy;
    }

    private final ConcurrentHashMap<String, AtomicLong> boilerplateByRule = new ConcurrentHashMap<>();

    public void recordBoilerplateHit(String ruleName) {
        if (ruleName == null) return;
        boilerplateByRule.computeIfAbsent(ruleName, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Long> boilerplateSnapshot() {
        return snapshotMap(boilerplateByRule);
    }
}
