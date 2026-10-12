package com.example.EVIM.domain.parsing;

import com.example.EVIM.domain.service.HtmlSanitizer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the production bug where ACME emails leak their "preheader"
 * (the hidden inbox-preview blurb wrapped in display:none/visibility:hidden
 * containers) into the parsed thread body.
 *
 * <p>Each fixture is a real HTML body shape captured from the corpus. The
 * preheader is one long collapsed line that boilerplate filters cannot
 * match (they are line-anchored), and it duplicates the visible content
 * the recipient already sees below.
 */
class HtmlPreheaderStrippingTest {

    private static final HtmlToPlainTextConverter CONVERTER =
            new HtmlToPlainTextConverter(text -> false, HtmlSanitizer::sanitize);

    @Test
    void strips_hidden_preheader_status_update() throws IOException {
        String html = loadFixture("preheader-status-update.html");
        String plain = CONVERTER.convert(html);

        // The hidden preheader collapses the whole message into one line.
        // It must not appear in the converted plaintext.
        assertFalse(plain.contains("##- Não escreva abaixo desta linha -## ________ Portal de Atendimento ACME Solicitação"),
                "Preheader (collapsed line) leaked into output:\n" + plain);

        // The visible content MUST be preserved.
        assertTrue(plain.contains("Olá Lucas"), "Visible greeting missing:\n" + plain);
        assertTrue(plain.contains("Renata"), "Visible author missing:\n" + plain);
        assertTrue(plain.contains("agenda operacional"), "Visible body missing:\n" + plain);
    }

    @Test
    void strips_hidden_preheader_incident_ack() throws IOException {
        String html = loadFixture("preheader-incident-ack.html");
        String plain = CONVERTER.convert(html);

        assertFalse(plain.contains("##- Não escreva abaixo desta linha -## ________ Portal de Atendimento ACME Olá Lucas Andrade O incidente"),
                "Preheader (collapsed line) leaked into output:\n" + plain);

        assertTrue(plain.contains("Olá Lucas Andrade"), "Visible greeting missing");
        assertTrue(plain.contains("será analisado pela nossa equipe de Desenvolvimento"),
                "Visible body missing");
        assertTrue(plain.contains("Equipe ACME"), "Visible footer missing");
    }

    private static String loadFixture(String name) throws IOException {
        Path p = Path.of("src/test/resources/emails/portal-preheader", name);
        return Files.readString(p, StandardCharsets.UTF_8);
    }
}
