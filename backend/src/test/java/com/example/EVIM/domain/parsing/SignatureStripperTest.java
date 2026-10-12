package com.example.EVIM.domain.parsing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks down the conservative signature stripping behavior on the four
 * canonical examples surfaced from the corpus, plus negative cases that
 * MUST NOT trigger a cut.
 *
 * <p>Attachments live on the {@code Email} aggregate (not in the body
 * text), so signature stripping cannot affect attachment rendering — that
 * invariant is implicit in every assertion below: the input bodies do not
 * contain attachment metadata, and we only ever return a prefix of the
 * input lines.
 */
class SignatureStripperTest {

    @Test
    void strips_northwind_block_after_short_intro() {
        String body = String.join("\n",
                "Bom dia, segue a análise solicitada.",
                "",
                " Clara Mendes Rocha",
                "SUSTENTAÇÃO - NORTHWIND",
                "cm.rocha@northwind.example");

        String out = SignatureStripper.strip(body);

        assertEquals("Bom dia, segue a análise solicitada.", out);
    }

    @Test
    void strips_atte_block() {
        String body = String.join("\n",
                "Olá Lucas, conseguimos reproduzir o cenário.",
                "Vamos prosseguir com a correção.",
                "",
                "Atte.",
                "Débora Antunes",
                "Sustentação - Northwind",
                "(11) 3000-0000 - Opções 2 e 8.",
                "debora.antunes@northwind.example");

        String out = SignatureStripper.strip(body);

        assertTrue(out.startsWith("Olá Lucas"), "Greeting kept");
        assertTrue(out.contains("Vamos prosseguir com a correção."), "Real content kept");
        assertFalse(out.contains("Débora Antunes"), "Signature name removed");
        assertFalse(out.contains("debora.antunes@northwind.example"), "Signature email removed");
        assertFalse(out.contains("Atte"), "Closing delimiter removed");
    }

    @Test
    void strips_ricardo_acme_block_with_slogan() {
        String body = String.join("\n",
                "Marcos,",
                "",
                "Segue análise sobre o contrato 1234567/10. Considerando a ponta ativa "
                        + "da linha de repasse, não observei qualquer divergência.",
                "Há alguma falha nesse entendimento?",
                "RICARDO MOREIRA GUIMARÃES / SUSTENTAÇÃO",
                "Plataforma de Crédito Digital",
                "+55 (11) 3000 0000 R:1234 / (11) 4000 0000",
                "ricardo.guimaraes@acme.example",
                "",
                "TRANSFORMANDO IDEIAS EM RESULTADOS");

        String out = SignatureStripper.strip(body);

        assertTrue(out.contains("Marcos,"));
        assertTrue(out.contains("contrato 1234567/10"));
        assertTrue(out.contains("Há alguma falha nesse entendimento?"));
        assertFalse(out.contains("RICARDO MOREIRA GUIMARÃES"));
        assertFalse(out.contains("ricardo.guimaraes@acme.example"));
        assertFalse(out.contains("TRANSFORMANDO IDEIAS EM RESULTADOS"));
    }

    @Test
    void strips_ricardo_block_with_aguardo_retorno_above() {
        String body = String.join("\n",
                "Conforme conversamos, segue o retorno técnico.",
                "Aguardo seu retorno.",
                "RICARDO MOREIRA GUIMARÃES / SUSTENTAÇÃO",
                "Plataforma de Crédito Digital",
                "+55 (11) 3000 0000 R:1234 / (11) 4000 0000",
                "ricardo.guimaraes@acme.example");

        String out = SignatureStripper.strip(body);

        assertTrue(out.contains("Conforme conversamos"));
        assertTrue(out.contains("Aguardo seu retorno."), "Aguardo line is real content, must stay");
        assertFalse(out.contains("RICARDO MOREIRA GUIMARÃES"));
        assertFalse(out.contains("ricardo.guimaraes@acme.example"));
    }

    @Test
    void does_not_strip_when_no_strong_signal() {
        String body = String.join("\n",
                "Segue conforme combinado.",
                "Boa tarde.",
                "Maria");

        // No email/phone/url/all-caps/Atte → keep as-is. "Maria" alone is not
        // a strong enough indicator to start cutting.
        String out = SignatureStripper.strip(body);
        assertEquals(body, out);
    }

    @Test
    void does_not_strip_when_cut_would_empty_the_body() {
        String body = String.join("\n",
                "Atte.",
                "João da Silva",
                "joao@example.com");
        // The whole body IS a signature. Refuse to cut to nothing.
        String out = SignatureStripper.strip(body);
        assertEquals(body, out);
    }

    @Test
    void preserves_inline_email_addresses_in_content() {
        String body = "Pode encaminhar o retorno para suporte@northwind.example ainda hoje?";
        // The email is INSIDE a sentence (not on its own line), so it must
        // not be classified as a signature line.
        String out = SignatureStripper.strip(body);
        assertEquals(body, out);
    }

    @Test
    void handles_null_and_blank_input() {
        assertEquals(null, SignatureStripper.strip(null));
        assertEquals("", SignatureStripper.strip(""));
        assertEquals("   \n  ", SignatureStripper.strip("   \n  "));
    }

    /**
     * Reproduces the email 10000003 case: greeting + one-line acknowledgement
     * from a Northwind agent, followed by a full Atte. signature block. The
     * acknowledgement line MUST survive (it is real content, not preheader
     * boilerplate); the signature MUST be removed.
     */
    @Test
    void strips_helena_northwind_acknowledgement() {
        String body = String.join("\n",
                "Olá,",
                "Sua solicitação foi recebida em nossa fila de atendimento Northwind, "
                        + "em breve retornaremos.",
                "Atte.",
                "Helena Prates",
                "Sustentação - Northwind",
                "(11) 3000-0000 - Opções 2 e 8.",
                "helena.prates@northwind.example");

        String out = SignatureStripper.strip(body);

        assertTrue(out.contains("Olá,"), "Greeting kept");
        assertTrue(out.contains("Sua solicitação foi recebida em nossa fila"),
                "Agent acknowledgement kept");
        assertFalse(out.contains("Atte"));
        assertFalse(out.contains("Helena Prates"));
        assertFalse(out.contains("helena.prates@northwind.example"));
    }

    /**
     * email 20/03/2026 (Beatriz de Souza Lima): greeting + one-paragraph
     * acknowledgement followed by an Atte. signature. The acknowledgement
     * MUST survive (it is the entire message); the signature MUST be cut.
     */
    @Test
    void strips_beatriz_northwind_acknowledgement() {
        String body = String.join("\n",
                "Olá,",
                "Sua solicitação foi recebida e registrada em nossa fila de atendimento "
                        + "da Northwind. Em breve, um de nossos especialistas entrará em "
                        + "contato para dar seguimento.",
                "Atte.",
                "Beatriz de Souza Lima",
                "SUSTENTAÇÃO - NORTHWIND",
                "(11) 3000-0000 - Opções 2 e 8.",
                "beatriz@northwind.example");

        String out = SignatureStripper.strip(body);

        assertTrue(out.contains("Olá,"));
        assertTrue(out.contains("Sua solicitação foi recebida e registrada em nossa fila"),
                "Acknowledgement (real content) kept");
        assertTrue(out.contains("dar seguimento."),
                "Last sentence of acknowledgement kept");
        assertFalse(out.contains("Atte"));
        assertFalse(out.contains("Beatriz de Souza Lima"));
        assertFalse(out.contains("SUSTENTAÇÃO - NORTHWIND"));
        assertFalse(out.contains("beatriz@northwind.example"));
    }

    @Test
    void isEffectivelyEmpty_detects_phantom_greeting_only() {
        // ACME auto-acknowledgement that surfaces as a separate
        // "interaction" with just a greeting + signature.
        String body = String.join("\n",
                "Olá Lucas,",
                "",
                "Atte.",
                "atendimento@acme.example");
        assertTrue(SignatureStripper.isEffectivelyEmpty(body));
    }

    @Test
    void isEffectivelyEmpty_keeps_real_acknowledgement() {
        String body = String.join("\n",
                "Olá,",
                "Sua solicitação foi recebida em nossa fila de atendimento Northwind.",
                "Atte.",
                "fulano@northwind.example");
        assertFalse(SignatureStripper.isEffectivelyEmpty(body));
    }

    @Test
    void isEffectivelyEmpty_handles_null_and_blank() {
        assertTrue(SignatureStripper.isEffectivelyEmpty(null));
        assertTrue(SignatureStripper.isEffectivelyEmpty(""));
        assertTrue(SignatureStripper.isEffectivelyEmpty("   \n  "));
    }
}
