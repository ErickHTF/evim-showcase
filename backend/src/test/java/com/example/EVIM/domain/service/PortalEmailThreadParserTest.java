package com.example.EVIM.domain.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Objects;

import com.example.EVIM.domain.parsing.DateRecognizer;
import com.example.EVIM.domain.parsing.HeaderDetector;

import static org.junit.jupiter.api.Assertions.*;

class PortalEmailThreadParserTest {

    private static String loadFixture(String name) throws IOException {
        try (InputStream in = Objects.requireNonNull(
                PortalEmailThreadParserTest.class.getResourceAsStream("/emails/" + name),
                "fixture not found: " + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ---------------------------------------------------------------- parse()

    @Test
    void parses_acme_thread_into_three_messages_in_chronological_order() throws Exception {
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(loadFixture("portal-thread.html"));

        assertEquals(3, msgs.size(), "expected pre-header content + 2 portal messages");

        PortalEmailThreadParser.ParsedMessage latest = msgs.get(0);
        assertNull(latest.author());
        assertNull(latest.date());
        assertTrue(latest.body().toLowerCase().contains("boa tarde"),
                "pre-header content should be preserved verbatim");

        PortalEmailThreadParser.ParsedMessage joao = msgs.get(1);
        assertEquals("João da Silva", joao.author());
        assertEquals("Portal", joao.company());
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13), joao.date());
        assertTrue(joao.body().toLowerCase().contains("validem o processo"));

        PortalEmailThreadParser.ParsedMessage maria = msgs.get(2);
        assertEquals("Maria Souza", maria.author());
        assertEquals("Vendor", maria.company());
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 12, 9, 30), maria.date());
        assertTrue(maria.body().toLowerCase().contains("recebido"));
    }

    @Test
    void strips_proofpoint_external_sender_banner_and_parses_english_date() throws Exception {
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(loadFixture("proofpoint-en.html"));

        // Banner must not leak into any message body.
        for (PortalEmailThreadParser.ParsedMessage m : msgs) {
            assertFalse(m.body().contains("ZjQcmQRYFpfpt"),
                    "Proofpoint banner markers should be stripped, found in: " + m.body());
            assertFalse(m.body().toLowerCase().contains("[external] this message is from"),
                    "External sender banner should be stripped, found in: " + m.body());
        }

        // English-formatted date must be recognized (Apr 13, 2026 2:13 PM).
        boolean englishDateRecognized = msgs.stream()
                .anyMatch(m -> m.date() != null
                        && m.date().equals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13)));
        assertTrue(englishDateRecognized,
                "English-formatted date 'Apr 13, 2026 2:13 PM' should parse to 2026-04-13T14:13");
    }

    @Test
    void preserves_short_greeting_when_followed_by_proper_header() throws Exception {
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(loadFixture("short-greeting.html"));

        assertFalse(msgs.isEmpty(), "short-greeting fixture should produce at least one message");
        // The trailing "boa noite" greeting (preceding the Carlos Mendes header) must
        // survive — it lives in the pre-header bucket as the most recent reply.
        boolean preserved = msgs.stream()
                .anyMatch(m -> m.body().toLowerCase().contains("boa noite"));
        assertTrue(preserved, "short greeting 'boa noite' should not be dropped");
    }

    // ----------------------------------------------------------- parseDate()

    private static final DateRecognizer DATE_RECOGNIZER = new DateRecognizer();

    @Test
    void parseDate_supports_portuguese_named_month() {
        LocalDateTime dt = DATE_RECOGNIZER.parseDate("13 de abr. de 2026, 14:13 BRT");
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13), dt);
    }

    @Test
    void parseDate_supports_english_month_first_with_am_pm() {
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13),
                DATE_RECOGNIZER.parseDate("Apr 13, 2026 2:13 PM"));
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 0, 30),
                DATE_RECOGNIZER.parseDate("April 13, 2026 12:30 AM"));
    }

    @Test
    void parseDate_supports_english_day_first() {
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13),
                DATE_RECOGNIZER.parseDate("13 Apr 2026 14:13"));
    }

    @Test
    void parseDate_supports_iso_8601() {
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13),
                DATE_RECOGNIZER.parseDate("2026-04-13T14:13:00"));
    }

    @Test
    void parseDate_supports_numeric_day_first() {
        assertEquals(LocalDateTime.of(2026, Month.APRIL, 13, 14, 13),
                DATE_RECOGNIZER.parseDate("13/04/2026 14:13"));
    }

    @Test
    void parseDate_returns_null_for_unknown_month_instead_of_defaulting_to_january() {
        // Previously this returned LocalDateTime.of(2026, 1, 13, 14, 13) which
        // silently corrupted message ordering. Now it must return null.
        assertNull(DATE_RECOGNIZER.parseDate("13 de xyz. de 2026, 14:13 BRT"));
    }

    @Test
    void parseDate_returns_null_for_invalid_calendar_values() {
        assertNull(DATE_RECOGNIZER.parseDate("31 de fev. de 2026, 14:13 BRT"));
        assertNull(DATE_RECOGNIZER.parseDate("13 de abr. de 2026, 99:13 BRT"));
    }

    @Test
    void parseDate_returns_null_for_blank_or_garbage() {
        assertNull(DATE_RECOGNIZER.parseDate(null));
        assertNull(DATE_RECOGNIZER.parseDate(""));
        assertNull(DATE_RECOGNIZER.parseDate("not a date"));
    }

    // ----------------------------------------------------- startsWithUpper()

    @Test
    void startsWithUpper_handles_accented_capitals() {
        assertTrue(HeaderDetector.startsWithUpper("Ângela"));
        assertTrue(HeaderDetector.startsWithUpper("Élio"));
        assertTrue(HeaderDetector.startsWithUpper("Úrsula"));
        assertTrue(HeaderDetector.startsWithUpper("JOÃO"));
        assertFalse(HeaderDetector.startsWithUpper("ângela"));
        assertFalse(HeaderDetector.startsWithUpper(""));
        assertFalse(HeaderDetector.startsWithUpper(null));
    }

    // ----------------------------------------- BOILERPLATE: anchored matches

    @Test
    void portal_de_atendimento_phrase_is_anchored_so_user_replies_survive() throws Exception {
        // A user reply that legitimately mentions "Portal de Atendimento" inside
        // a longer sentence must NOT be eaten by the boilerplate filter.
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Não consigo acessar o Portal de Atendimento desde ontem, podem verificar?</p>"
                + "</div>";

        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        // The Portal de Atendimento pattern is anchored to a full chrome line —
        // the user sentence containing it should still be kept somewhere.
        boolean preserved = msgs.stream()
                .anyMatch(m -> m.body().contains("Não consigo acessar o Portal"));
        assertTrue(preserved, "user sentence mentioning 'Portal de Atendimento' was wrongly stripped");
    }

    // ---------------------------------------------- HTML pre-processing

    @Test
    void parses_double_entity_encoded_html() throws Exception {
        // The fixture has <a> tags encoded as &lt;a&gt; (common when an email
        // template was double-escaped). Pre-parse unescape should let JSoup
        // build a real DOM and produce a markdown link in the body.
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(loadFixture("double-encoded-html.html"));

        assertFalse(msgs.isEmpty());
        boolean linkRendered = msgs.stream()
                .anyMatch(m -> m.body().contains("[Result.xlsx](https://example.com/Result.xlsx)"));
        assertTrue(linkRendered,
                "double-entity-encoded anchor should round-trip to a markdown link");
        // No raw HTML should leak through.
        for (PortalEmailThreadParser.ParsedMessage m : msgs) {
            assertFalse(m.body().contains("<a "), "raw <a> tag leaked: " + m.body());
            assertFalse(m.body().contains("&lt;"), "literal entity leaked: " + m.body());
        }
    }

    @Test
    void preserves_author_name_from_image_alt_text() throws Exception {        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(loadFixture("img-alt-author.html"));

        // The avatar img alt="Maria Aparecida Silva" must be preserved — by being
        // promoted to plain text it serves as the AUTHOR_LINE of a two-line
        // header (date is on the next line) and becomes the message author.
        boolean nameIsAuthor = msgs.stream()
                .anyMatch(m -> "Maria Aparecida Silva".equals(m.author()));
        assertTrue(nameIsAuthor,
                "author name from <img alt=...> should drive the parsed author");
    }

    // -------------------------------- BOILERPLATE: dominant-term gating

    @Test
    void short_user_line_mentioning_disclaimer_word_is_preserved() {
        // Regression: "favor revisar o disclaimer do contrato" used to be
        // eaten by ^.*disclaimer.*$. Dominant-term gating requires the line
        // to be long enough to look like an actual disclaimer paragraph.
        String html = "<div>"
                + "<p>Carlos Mendes (Jur\u00eddico)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Favor revisar o disclaimer do contrato.</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        boolean preserved = msgs.stream()
                .anyMatch(m -> m.body().contains("Favor revisar o disclaimer do contrato"));
        assertTrue(preserved, "short user line containing 'disclaimer' was wrongly stripped");
    }

    @Test
    void long_legal_disclaimer_paragraph_is_still_stripped() {
        String longDisclaimer = "Esta mensagem e seus anexos s\u00e3o confidenciais e destinam-se "
                + "exclusivamente ao destinat\u00e1rio. Caso voc\u00ea n\u00e3o seja o destinat\u00e1rio, "
                + "favor notificar o remetente e apagar imediatamente esta mensagem. Disclaimer applies.";
        String html = "<div>"
                + "<p>Carlos Mendes (Jur\u00eddico)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Mensagem real do usu\u00e1rio.</p>"
                + "<p>" + longDisclaimer + "</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        for (PortalEmailThreadParser.ParsedMessage m : msgs) {
            assertFalse(m.body().contains(longDisclaimer),
                    "long legal disclaimer paragraph should be stripped: " + m.body());
        }
    }

    @Test
    void user_reply_mentioning_options_keyword_survives() {
        // Regression: "^op\u00e7\u00f5es \\d.*$" used to nuke any user reply
        // mentioning options. The pattern was removed entirely.
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Op\u00e7\u00f5es 3 e 4 est\u00e3o off no momento.</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        boolean preserved = msgs.stream()
                .anyMatch(m -> m.body().contains("Op\u00e7\u00f5es 3 e 4"));
        assertTrue(preserved, "user reply mentioning 'op\u00e7\u00f5es N' was wrongly stripped");
    }

    // ------------------------- cleanOrphanHeaders: date-line filter

    @Test
    void user_sentence_containing_a_date_is_not_dropped() {
        // Regression: any line under MAX_AUTHOR_LINE_LENGTH containing a date
        // substring used to be silently removed by cleanOrphanHeaders. A
        // legitimate sentence with sentence-terminator punctuation must survive.
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Reuni\u00e3o marcada para 13/04/2026 14:13.</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        boolean preserved = msgs.stream()
                .anyMatch(m -> m.body().contains("Reuni\u00e3o marcada para 13/04/2026"));
        assertTrue(preserved, "user sentence containing a date was wrongly stripped");
    }

    @Test
    void standalone_date_line_inside_body_is_still_stripped() {
        // The whole-line date case (header leak) must still be removed.
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Conte\u00fado relevante</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        // No body should still contain a bare standalone date line.
        for (PortalEmailThreadParser.ParsedMessage m : msgs) {
            for (String l : m.body().split("\\n")) {
                assertFalse(l.trim().equals("13 de abr. de 2026, 14:13 BRT"),
                        "standalone date line leaked into body: " + m.body());
            }
        }
    }

    // ------------------------ Paragraph separators / Unicode noise

    @Test
    void multi_paragraph_user_body_keeps_paragraph_separators() {
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Primeiro par\u00e1grafo da resposta.</p>"
                + "<p>Segundo par\u00e1grafo, depois de uma linha em branco.</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        boolean hasSeparator = msgs.stream()
                .anyMatch(m -> m.body().contains("Primeiro par\u00e1grafo da resposta.\n\nSegundo par\u00e1grafo"));
        assertTrue(hasSeparator,
                "paragraph separator (blank line) between user paragraphs was lost");
    }

    @Test
    void unicode_box_drawing_separator_is_treated_as_chrome() {
        // U+2500 (\u2500) divider line between avatar and header should not
        // break two-line header detection.
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>\u2500\u2500\u2500\u2500\u2500\u2500</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Conte\u00fado relevante.</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        boolean parsedAuthor = msgs.stream()
                .anyMatch(m -> "Carlos Mendes".equals(m.author()));
        assertTrue(parsedAuthor,
                "Unicode \u2500 separator should be skipped during header look-ahead");
    }

    @Test
    void long_dash_or_equals_separator_is_stripped_from_body() {
        String html = "<div>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Conte\u00fado.</p>"
                + "<p>------------</p>"
                + "<p>============</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        for (PortalEmailThreadParser.ParsedMessage m : msgs) {
            assertFalse(m.body().contains("------------"),
                    "dash separator leaked into body: " + m.body());
            assertFalse(m.body().contains("============"),
                    "equals separator leaked into body: " + m.body());
        }
    }

    // ----------------------------------------- Single sanitization pipeline

    @Test
    void htmlToPlainText_strips_script_tags_via_sanitizer() {
        String dirty = "<div><p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Confirmado.</p>"
                + "<script>alert('xss')</script>"
                + "<p onclick=\"steal()\">tail</p></div>";
        String plain = PortalEmailThreadParser.htmlToPlainText(dirty);
        assertFalse(plain.toLowerCase().contains("alert"),
                "script body must not survive the sanitizer pass");
        assertFalse(plain.toLowerCase().contains("onclick"),
                "dangerous attributes must be stripped");
        assertTrue(plain.contains("Confirmado."));
        assertTrue(plain.contains("Carlos Mendes"));
    }

    @Test
    void parser_does_not_extract_author_from_injected_script() {
        // A malicious payload tries to forge an author header via a <script>
        // block; sanitization must remove it before parsing so it cannot
        // appear as a parsed message author.
        String html = "<div>"
                + "<script>document.write('Evil Hacker (Bad)<br>13 de abr. de 2026, 14:13 BRT<br>pwned')</script>"
                + "<p>Carlos Mendes (TI)</p>"
                + "<p>13 de abr. de 2026, 14:13 BRT</p>"
                + "<p>Tudo ok.</p></div>";

        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);
        for (PortalEmailThreadParser.ParsedMessage m : msgs) {
            assertNotEquals("Evil Hacker", m.author());
            assertFalse(m.body().toLowerCase().contains("pwned"),
                    "script-injected text must not leak into parsed body: " + m.body());
        }
    }

    @Test
    void single_name_with_company_splits_without_date() {
        // Simulates ACME portal pattern: quoted previous message has only
        // "Renata (ACME)" without a timestamp line.
        String html = "<div>"
                + "<p>Marcos Lima (TI)</p>"
                + "<p>15/04/2026, 11:44 BRT</p>"
                + "<p>Renata, bom dia!</p>"
                + "<p>Segue em anexo o arquivo trace.</p>"
                + "<p>Renata (ACME)</p>"
                + "<p>Marcos, boa tarde!</p>"
                + "<p>Aguardamos o TRACE e o arquivo .ini de HML.</p>"
                + "</div>";
        List<PortalEmailThreadParser.ParsedMessage> msgs =
                PortalEmailThreadParser.parse(html);

        assertTrue(msgs.size() >= 2,
                "Should split into at least 2 messages, got " + msgs.size()
                + ": " + msgs);

        boolean hasRenata = msgs.stream()
                .anyMatch(m -> "Renata".equals(m.author()) && "ACME".equals(m.company()));
        assertTrue(hasRenata,
                "Expected a message authored by 'Renata (ACME)': " + msgs);

        PortalEmailThreadParser.ParsedMessage renataMsg = msgs.stream()
                .filter(m -> "Renata".equals(m.author()))
                .findFirst().orElseThrow();
        assertTrue(renataMsg.body().contains("Aguardamos"),
                "Renata's message body should contain her text");
        assertNull(renataMsg.date(),
                "Date should be null when portal omits timestamp on quoted message");
    }

    @Test
    void acme_full_body_preserved_after_split_and_strip() {
        // Reproduces: Marcos's body truncated to "Oi Fernando, bom dia!" losing
        // "Sim, estamos abrindo o arquivo e apagando manualmente os negativos."
        String html = "<div>"
                + "<p>Marcos Lima</p>"
                + "<p>6 de abr. de 2026, 08:49 BRT</p>"
                + "<p>Oi Fernando, bom dia!</p>"
                + "<p>Sim, estamos abrindo o arquivo e apagando manualmente os negativos.</p>"
                + "<p>FERNANDO AUGUSTO PEREIRA DOS SANTOS (ACME)</p>"
                + "<p>2 de abr. de 2026, 09:19 BRT</p>"
                + "<p>Marcos, bom dia! Tudo bem?</p>"
                + "<p>Voc\u00eas atualmente est\u00e3o aplicando algum paliativo para essa situa\u00e7\u00e3o?</p>"
                + "<p>Atenciosamente,</p>"
                + "<p>Fernando Augusto P. dos Santos</p>"
                + "<p>Analista N2 - Sustenta\u00e7\u00e3o Northwind</p>"
                + "<p>fernando.santos@northwind.example</p>"
                + "</div>";

        var msgs = PortalEmailThreadParser.parse(html);
        assertTrue(msgs.size() >= 2, "Expected at least 2 messages: " + msgs);

        // Find Marcos's message
        var marcos = msgs.stream()
                .filter(m -> "Marcos Lima".equals(m.author()))
                .findFirst().orElseThrow(() -> new AssertionError("Marcos not found: " + msgs));

        assertTrue(marcos.body().contains("Sim, estamos abrindo"),
                "Marcos's full body must be preserved: " + marcos.body());

        // Also verify that stripBoilerplate does not eat it
        String stripped = PortalEmailThreadParser.stripBoilerplate(marcos.body());
        assertTrue(stripped.contains("Sim, estamos abrindo"),
                "stripBoilerplate must not remove Marcos's body content: " + stripped);
    }
}
