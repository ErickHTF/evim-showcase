package com.example.EVIM.application.metrics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParserMetricsTest {

    @Test
    void counts_success_fallback_failed_and_dates_separately() {
        ParserMetrics m = new ParserMetrics();
        m.recordSuccess("acme.example", 3);
        m.recordSuccess("acme.example", 2);
        m.recordSuccess("acme.com", 1);
        m.recordFallback("acme.com");
        m.recordFailed();
        m.recordDateUnparsed(2);

        Map<String, Object> snap = m.snapshot();
        assertEquals(3L, snap.get("success"));
        assertEquals(1L, snap.get("fallback"));
        assertEquals(1L, snap.get("failed"));
        assertEquals(2L, snap.get("dateUnparsed"));
        assertEquals(6L, snap.get("messagesEmitted"));
        assertEquals(0.25, (double) snap.get("fallbackRatio"), 1e-9);
        assertEquals(2.0, (double) snap.get("avgMessagesPerSuccess"), 1e-9);

        @SuppressWarnings("unchecked")
        Map<String, Long> byDomain = (Map<String, Long>) snap.get("successByDomain");
        assertEquals(2L, byDomain.get("acme.example"));
        assertEquals(1L, byDomain.get("acme.com"));
    }

    @Test
    void extractDomain_handles_edge_cases() {
        assertEquals("acme.example", ParserMetrics.extractDomain("user@acme.example"));
        assertEquals("acme.example", ParserMetrics.extractDomain("USER@ACME.EXAMPLE"));
        assertEquals("unknown", ParserMetrics.extractDomain(null));
        assertEquals("unknown", ParserMetrics.extractDomain(""));
        assertEquals("unknown", ParserMetrics.extractDomain("no-at-sign"));
        assertEquals("unknown", ParserMetrics.extractDomain("trailing@"));
    }

    @Test
    void reset_zeroes_all_counters_and_resets_since_timestamp() throws Exception {
        ParserMetrics m = new ParserMetrics();
        m.recordSuccess("a.com", 1);
        m.recordFallback("b.com");
        String before = (String) m.snapshot().get("since");
        Thread.sleep(5); // ensure since changes
        m.reset();
        Map<String, Object> after = m.snapshot();
        assertEquals(0L, after.get("success"));
        assertEquals(0L, after.get("fallback"));
        assertEquals(0L, after.get("messagesEmitted"));
        assertNotEquals(before, after.get("since"));
    }

    @Test
    void cardinality_cap_buckets_overflow_into_other() {
        ParserMetrics m = new ParserMetrics();
        // Push past the 100-entry cap.
        for (int i = 0; i < 150; i++) {
            m.recordSuccess("d" + i + ".com", 1);
        }
        @SuppressWarnings("unchecked")
        Map<String, Long> byDomain = (Map<String, Long>) m.snapshot().get("successByDomain");
        assertTrue(byDomain.containsKey("other"), "overflow domain bucket missing");
        assertTrue(byDomain.size() <= 101,
                "expected at most 100 domains + 'other', got " + byDomain.size());
        long total = byDomain.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(150L, total, "every recorded success must land somewhere");
    }
}
