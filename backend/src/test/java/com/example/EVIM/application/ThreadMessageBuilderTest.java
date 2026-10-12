package com.example.EVIM.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.example.EVIM.application.metrics.ParseDiagnostic;

class ThreadMessageBuilderTest {

    private final ThreadMessageBuilder builder = new ThreadMessageBuilder();

    @Test
    void assemble_returns_failed_for_null_body() {
        ThreadMessageBuilder.Build b = builder.assemble(
                new ThreadMessageBuilder.Source(null, "user@example.com", "User", null));
        assertTrue(b.messages().isEmpty());
        assertEquals(ParseDiagnostic.Outcome.FAILED, b.diagnostic().outcome());
        assertEquals("null-body", b.diagnostic().fallbackReason());
    }

    @Test
    void assemble_returns_failed_for_blank_after_strip() {
        // A body that strips down to nothing (only whitespace + a known boilerplate line).
        String body = "<html><body><p>   </p><p>Atenciosamente,</p></body></html>";
        ThreadMessageBuilder.Build b = builder.assemble(
                new ThreadMessageBuilder.Source(body, "user@example.com", "User", LocalDateTime.now()));
        assertTrue(b.messages().isEmpty(), "Expected no messages, got: " + b.messages());
        assertEquals(ParseDiagnostic.Outcome.FAILED, b.diagnostic().outcome());
    }

    @Test
    void assemble_falls_back_to_single_message_when_parser_returns_empty() {
        String body = "<html><body><p>Olá, segue minha resposta sem cabeçalho de portal.</p></body></html>";
        LocalDateTime received = LocalDateTime.of(2026, 4, 13, 14, 13);
        ThreadMessageBuilder.Build b = builder.assemble(
                new ThreadMessageBuilder.Source(body, "user@example.com", "Maria Silva", received));

        assertEquals(1, b.messages().size());
        assertEquals(ParseDiagnostic.Outcome.FALLBACK, b.diagnostic().outcome());
        assertEquals("Maria Silva", b.messages().get(0).author());
        assertEquals(received, b.messages().get(0).date());
        assertTrue(b.messages().get(0).body().contains("segue minha resposta"));
    }

    @Test
    void assemble_uses_sender_address_when_display_name_blank() {
        String body = "<p>Texto simples</p>";
        ThreadMessageBuilder.Build b = builder.assemble(
                new ThreadMessageBuilder.Source(body, "noreply@example.com", null, LocalDateTime.now()));
        assertEquals(1, b.messages().size());
        assertEquals("noreply@example.com", b.messages().get(0).author());
    }

    @Test
    void domain_is_extracted_into_diagnostic() {
        String body = "<p>x</p>";
        ThreadMessageBuilder.Build b = builder.assemble(
                new ThreadMessageBuilder.Source(body, "alice@VENDOR.com", "Alice", LocalDateTime.now()));
        assertEquals("vendor.com", b.diagnostic().senderDomain());
    }
}
