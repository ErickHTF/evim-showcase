package com.example.EVIM.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    @Test
    void plainTextToHtml_round_trips_simple_markdown_link() {
        String html = HtmlSanitizer.plainTextToHtml("See [Report](https://example.com/r.xlsx).");
        assertTrue(html.contains("href=\"https://example.com/r.xlsx\""), html);
        assertTrue(html.contains(">Report</a>"), html);
    }

    @Test
    void plainTextToHtml_handles_escaped_brackets_in_text_and_url() {
        // Backslash-escaped ] in text and ) in url, as produced by the parser
        // when a link's display text or href contains those characters.
        String html = HtmlSanitizer.plainTextToHtml(
                "Open [foo\\]bar](https://example.com/page\\)x).");
        assertTrue(html.contains("href=\"https://example.com/page)x\""), html);
        assertTrue(html.contains(">foo]bar</a>"), html);
    }

    @Test
    void plainTextToHtml_does_not_break_when_no_links_present() {
        String html = HtmlSanitizer.plainTextToHtml("Bom dia.\nTudo certo?");
        assertEquals("Bom dia.<br>Tudo certo?", html);
    }

    @Test
    void plainTextToHtml_linkifies_bare_url() {
        String html = HtmlSanitizer.plainTextToHtml("Veja https://example.com aqui.");
        assertTrue(html.contains("<a href=\"https://example.com\""), html);
    }

    @Test
    void sanitize_strips_scripts_and_keeps_safe_tags() {
        String dirty = "<p>Hi</p><script>alert(1)</script><b>x</b>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertFalse(clean.toLowerCase().contains("<script"));
        assertTrue(clean.contains("<p>Hi</p>"));
        assertTrue(clean.contains("<b>x</b>"));
    }
}
