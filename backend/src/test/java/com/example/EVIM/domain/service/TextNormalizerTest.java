package com.example.EVIM.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    @Test
    void fixes_classic_mojibake() {
        assertEquals("Não", TextNormalizer.fixDoubleEncodedUtf8("NÃ£o"));
        assertEquals("informação", TextNormalizer.fixDoubleEncodedUtf8("informaÃ§Ã£o"));
    }

    @Test
    void is_idempotent() {
        String src = "informaÃ§Ã£o";
        String once = TextNormalizer.fixDoubleEncodedUtf8(src);
        String twice = TextNormalizer.fixDoubleEncodedUtf8(once);
        assertEquals(once, twice, "running the fix twice must not corrupt the result");
        assertEquals("informação", twice);
    }

    @Test
    void leaves_legitimate_text_with_stray_capital_a_tilde_alone() {
        String legit = "Marca Ã Tabela";
        assertEquals(legit, TextNormalizer.fixDoubleEncodedUtf8(legit));
    }

    @Test
    void leaves_already_correct_utf8_alone() {
        String good = "Ação, comunicação e informação.";
        assertEquals(good, TextNormalizer.fixDoubleEncodedUtf8(good));
    }

    @Test
    void handles_null_and_empty() {
        assertNull(TextNormalizer.fixDoubleEncodedUtf8(null));
        assertEquals("", TextNormalizer.fixDoubleEncodedUtf8(""));
    }
}
