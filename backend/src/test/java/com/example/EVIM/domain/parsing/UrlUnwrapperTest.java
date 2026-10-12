package com.example.EVIM.domain.parsing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlUnwrapperTest {

    @Test
    void unwraps_microsoft_safelinks() {
        String wrapped = "https://nam04.safelinks.protection.outlook.com/"
                + "?url=https%3A%2F%2Fsuporte.acme.example%2Fhc%2Fpt-br%2Frequests%2F10000002"
                + "&data=05%7C02%7C&sdata=abc&reserved=0";
        assertEquals(
                "https://suporte.acme.example/hc/pt-br/requests/10000002",
                UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void unwraps_safelinks_with_eur_subdomain() {
        String wrapped = "https://eur02.safelinks.protection.outlook.com/?url=https%3A%2F%2Fexample.com%2Fa%3Fb%3D1&data=x";
        assertEquals("https://example.com/a?b=1", UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void unwraps_proofpoint_v2() {
        // v2: ?u=<encoded>&  with - => % and _ => /
        // Original: https://example.com/a/b?x=1
        // %2F => -2F (not exactly — proofpoint encodes differently, but our impl
        // restores '_'->'/' and '-'->'%' before URLDecoder)
        String wrapped = "https://urldefense.proofpoint.com/v2/url"
                + "?u=https-3A__example.com_a_b-3Fx-3D1&d=DwIFaQ&c=abc";
        assertEquals("https://example.com/a/b?x=1", UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void unwraps_proofpoint_v1_url_param() {
        String wrapped = "https://urldefense.proofpoint.com/v1/url?url=https%3A%2F%2Fexample.com%2F&k=abc";
        assertEquals("https://example.com/", UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void unwraps_proofpoint_v3_path_based() {
        String wrapped = "https://urldefense.com/v3/__https://suporte.acme.example/hc/pt-br/requests/10000002__;!!ABC!XYZ$";
        assertEquals(
                "https://suporte.acme.example/hc/pt-br/requests/10000002",
                UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void unwraps_proofpoint_v3_with_proofpoint_host() {
        String wrapped = "https://urldefense.proofpoint.com/v3/__https://example.com/path?x=1__;Ig!!a!b";
        assertEquals("https://example.com/path?x=1", UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void leaves_mimecast_unchanged_but_does_not_crash() {
        String wrapped = "https://protect-eu.mimecast.com/s/abcDEF123";
        assertEquals(wrapped, UrlUnwrapper.unwrap(wrapped));
    }

    @Test
    void leaves_plain_url_unchanged() {
        String url = "https://example.com/path?x=1";
        assertEquals(url, UrlUnwrapper.unwrap(url));
    }

    @Test
    void handles_null_and_blank() {
        assertNull(UrlUnwrapper.unwrap(null));
        assertEquals("", UrlUnwrapper.unwrap(""));
        assertEquals("   ", UrlUnwrapper.unwrap("   "));
    }

    @Test
    void leaves_invalid_uri_unchanged() {
        String bad = "not a url";
        assertEquals(bad, UrlUnwrapper.unwrap(bad));
    }

    @Test
    void unwraps_double_wrapped_safelinks_inside_proofpoint() {
        // Proofpoint wrapping a Safelinks URL — should resolve to the innermost.
        String inner = "https://nam04.safelinks.protection.outlook.com/?url=https%3A%2F%2Fexample.com%2F&data=x";
        // proofpoint v1 fallback with url= param keeps things deterministic for the test
        String wrapped = "https://urldefense.proofpoint.com/v1/url?url="
                + java.net.URLEncoder.encode(inner, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("https://example.com/", UrlUnwrapper.unwrap(wrapped));
    }
}
