package com.example.EVIM.domain.parsing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Locks down the ACME Portal de Atendimento boilerplate patterns surfaced
 * by the conversation 28 corpus (email ids 8601 / 8651 / 11701).
 *
 * <p>Each junk line MUST match a rule; each visible line MUST be preserved.
 */
class BoilerplateFilterPortalTest {

    private final BoilerplateFilter filter = new BoilerplateFilter();

    @Test
    void matches_acme_portal_boilerplate_lines() {
        assertNotNull(filter.matchedRule("##- Não escreva abaixo desta linha -##"));
        assertNotNull(filter.matchedRule("Portal de Atendimento ACME"));
        // The ACME template often glues an <hr> onto the title line.
        assertNotNull(filter.matchedRule("________Portal de Atendimento ACME"));
        assertNotNull(filter.matchedRule("____ Portal de Atendimento ACME"));
        assertNotNull(filter.matchedRule("Solicitação n°: 10000001"));
        assertNotNull(filter.matchedRule("Solicitação nº 10000004"));
        assertNotNull(filter.matchedRule("Data de Abertura: 16/04/2026 09:48 BRT"));
        assertNotNull(filter.matchedRule("Último status: Em atendimento"));
        assertNotNull(filter.matchedRule("Sua solicitação n° 10000004 foi atualizada."));
        assertNotNull(filter.matchedRule("Sua solicitação (10000004) foi recebida"));
        assertNotNull(filter.matchedRule("Anexo(s)"));
        assertNotNull(filter.matchedRule("Você será notificado das atualizações deste atendimento por e-mail."));
        assertNotNull(filter.matchedRule("Se necessário interagir na sua solicitação, responda este email."));
        assertNotNull(filter.matchedRule("Para adicionar outros comentários, responda a este email."));
    }

    @Test
    void matches_external_sender_banners() {
        assertNotNull(filter.matchedRule("[EXTERNO] Esta mensagem foi enviada por um remetente externo"));
        assertNotNull(filter.matchedRule("[EXTERNO]Esta mensagem foi enviada por um remetente externo"));
        assertNotNull(filter.matchedRule("[EXTERNAL] This Message Is From an External Sender"));
        // No space between ] and "This" — ACME strips it.
        assertNotNull(filter.matchedRule("[EXTERNAL]This Message Is From an External Sender"));
    }

    @Test
    void matches_lowercase_zd_encoded_id() {
        assertNotNull(filter.matchedRule("[27abc12-xyz9k0]"));
    }

    @Test
    void matches_csat_feedback_survey_lines() {
        assertNotNull(filter.matchedRule(
                "Queremos saber a sua opinião sobre o nosso atendimento ao cliente "
                        + "no ticket 10000004 sobre Erro de Relatório. Atribua seu nível de "
                        + "satisfação, clicando na escala abaixo:"));
        assertNotNull(filter.matchedRule(
                "Atribua seu nível de satisfação, clicando na escala abaixo:"));
        assertNotNull(filter.matchedRule(
                "A sua opinião será valorizada 🤩 por todo o nosso time, "
                        + "e nos ajudará a melhorar continuamente o nosso atendimento."));
        assertNotNull(filter.matchedRule("Agradecemos sua contribuição."));
        assertNotNull(filter.matchedRule("Este é um lembrete do assunto desta solicitação:"));
        // 0..10 score scale, one per line.
        for (int i = 0; i <= 10; i++) {
            assertNotNull(filter.matchedRule(String.valueOf(i)),
                    "Score line " + i + " must be dropped");
        }
        // Each digit is wrapped in an anchor (the survey link), which the
        // HTML→plain-text converter renders as a markdown link. The CSAT
        // score rule MUST also drop those wrapped lines, otherwise the
        // body collapses to "Olá Lucas," + bare digits and surfaces as a
        // ghost interaction.
        for (int i = 0; i <= 10; i++) {
            assertNotNull(filter.matchedRule(
                    "[" + i + "](https://surveys.acme.example/score?value=" + i + ")"),
                    "Markdown-linked score line " + i + " must be dropped");
        }
    }

    @Test
    void preserves_visible_content() {
        assertNull(filter.matchedRule("Olá Lucas Andrade,"));
        assertNull(filter.matchedRule("Equipe ACME"));
        assertNull(filter.matchedRule("O incidente será analisado pela nossa equipe de Desenvolvimento."));
        assertNull(filter.matchedRule("Renata (ACME) — 16 de abr. de 2026, 09:48 BRT"));
        assertNull(filter.matchedRule("Segue em anexo a agenda operacional para análise."));
        // The "solicitacao-atualizada" preheader rule MUST require a digit
        // (the ticket number) — without it, legitimate agent prose like the
        // line below was being eaten. See BoilerplateFilter#vendor-portal.solicitacao-atualizada.
        assertNull(filter.matchedRule(
                "Sua solicitação foi recebida em nossa fila de atendimento Northwind, em breve retornaremos."));
        assertNull(filter.matchedRule("Sua solicitação foi atualizada conforme combinado."));
        // email 20/03/2026 (Beatriz): same shape, longer prose. MUST stay.
        assertNull(filter.matchedRule(
                "Sua solicitação foi recebida e registrada em nossa fila de atendimento da Northwind. "
                        + "Em breve, um de nossos especialistas entrará em contato para dar seguimento."));
    }

    @Test
    void strips_boilerplate_block_keeping_visible_text() {
        String raw = String.join("\n",
                "Olá Lucas,",
                "[EXTERNO] Esta mensagem foi enviada por um remetente externo",
                "Sua solicitação (10000004) foi recebida",
                "Solicitação n°: 10000004",
                "Data de Abertura: 16/04/2026 09:48 BRT",
                "Último status: Em atendimento",
                "Portal de Atendimento ACME",
                "________________________________",
                "Segue em anexo a agenda operacional para análise.",
                "Equipe ACME");

        String cleaned = filter.stripFromText(raw, name -> false);

        assertEquals(String.join("\n",
                "Olá Lucas,",
                "Segue em anexo a agenda operacional para análise.",
                "Equipe ACME"), cleaned);
    }
}
