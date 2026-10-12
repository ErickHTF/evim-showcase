package com.example.EVIM.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.example.EVIM.domain.service.PortalEmailThreadParser.ParsedMessage;

/**
 * Snapshot tests over the HTML fixtures in {@code src/test/resources/emails/}.
 * <p>
 * For each {@code <name>.html} fixture, expects a sibling {@code <name>.expected.json}
 * containing the parser output. If the JSON file is missing the test fails
 * with the suggested content so the developer can copy it in (after manually
 * verifying it is correct). When parser behavior intentionally changes,
 * delete the JSON and re-run \u2014 the failure message contains the new snapshot.
 *
 * <p>This is the checklist the parser was missing: any rule change that
 * affects an existing fixture surfaces as a visible diff in the PR.
 */
class PortalEmailThreadParserSnapshotTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    static Stream<String> fixtureNames() throws IOException {
        Path dir = Paths.get("src/test/resources/emails");
        if (!Files.isDirectory(dir)) return Stream.empty();
        try (var s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".html"))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.html$", ""))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureNames")
    @DisplayName("Fixture matches expected snapshot")
    void fixture_matches_snapshot(String name) throws IOException {
        Path htmlPath = Paths.get("src/test/resources/emails/" + name + ".html");
        Path expectedPath = Paths.get("src/test/resources/emails/" + name + ".expected.json");

        String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
        List<ParsedMessage> parsed = PortalEmailThreadParser.parse(html);
        String actualJson = MAPPER.writeValueAsString(toSnapshot(parsed));

        if (!Files.exists(expectedPath)) {
            // Auto-bless: when -Dsnapshot.update=true, write the new snapshot
            // and pass. Without the flag, fail with the suggested content.
            if (Boolean.getBoolean("snapshot.update")) {
                Files.writeString(expectedPath, actualJson, StandardCharsets.UTF_8);
                return;
            }
            fail("Missing snapshot file: " + expectedPath
                    + "\nRe-run with -Dsnapshot.update=true to create it,"
                    + " then verify the diff before committing.\n\n" + actualJson);
        }

        String expectedJson = Files.readString(expectedPath, StandardCharsets.UTF_8).trim();
        if (!expectedJson.equals(actualJson.trim()) && Boolean.getBoolean("snapshot.update")) {
            Files.writeString(expectedPath, actualJson, StandardCharsets.UTF_8);
            return;
        }
        assertEquals(expectedJson, actualJson.trim(),
                "Snapshot drift in " + name + ".html. If intentional, re-run with -Dsnapshot.update=true");
    }

    /**
     * Reduce {@link ParsedMessage} to a stable map keeping only the fields we
     * want to track in snapshots. The body is captured as the first 200 chars
     * so trivial rewording inside long bodies doesn't produce noisy diffs;
     * structural changes (different number of messages, different author/date)
     * still surface.
     */
    private static List<Map<String, Object>> toSnapshot(List<ParsedMessage> messages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ParsedMessage m : messages) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("author", m.author());
            entry.put("company", m.company());
            entry.put("date", m.date() == null ? null : m.date().toString());
            entry.put("bodyLength", m.body() == null ? 0 : m.body().length());
            entry.put("bodyPreview", m.body() == null ? "" :
                    m.body().substring(0, Math.min(200, m.body().length())));
            out.add(entry);
        }
        return out;
    }
}
