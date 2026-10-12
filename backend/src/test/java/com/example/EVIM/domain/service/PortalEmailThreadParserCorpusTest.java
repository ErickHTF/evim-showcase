package com.example.EVIM.domain.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.example.EVIM.domain.service.PortalEmailThreadParser.ParseResult;
import com.example.EVIM.domain.service.PortalEmailThreadParser.ParsedMessage;

/**
 * Runs the parser against a local directory of real-world email HTML bodies
 * and prints aggregate statistics. Intended for local diagnostic runs, not
 * CI \u2014 enabled only when {@code -Dparser.corpus.dir=<path>} is supplied.
 *
 * <p>Usage:
 * <pre>{@code
 * .\mvnw.cmd "-Dparser.corpus.dir=C:\corpus" \
 *            "-Dtest=PortalEmailThreadParserCorpusTest" test
 * }</pre>
 *
 * <p>The corpus directory should contain one {@code .html} file per email
 * (recursive scan). Files are read as UTF-8. The test never fails on
 * individual parse outcomes \u2014 it asserts only that the overall fallback
 * ratio stays under {@link #MAX_FALLBACK_RATIO}, which is a sanity bound
 * intended to catch a regression that doubles the fallback rate.
 *
 * <p>The corpus directory itself is intentionally outside the repository
 * (gitignored at the dev-environment level); commit only the scaffold.
 */
class PortalEmailThreadParserCorpusTest {

    /** Bound the test trips on. Tune up/down as the dataset evolves. */
    private static final double MAX_FALLBACK_RATIO = 0.20;

    @Test
    @EnabledIfSystemProperty(named = "parser.corpus.dir", matches = ".+")
    void parses_corpus_with_acceptable_fallback_ratio() throws IOException {
        Path root = Paths.get(System.getProperty("parser.corpus.dir"));
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Not a directory: " + root);
        }

        AtomicInteger total = new AtomicInteger();
        AtomicInteger empty = new AtomicInteger();
        AtomicInteger emitted = new AtomicInteger();
        AtomicInteger unparsedDates = new AtomicInteger();

        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".html"))
                  .forEach(p -> {
                      try {
                          String html = Files.readString(p, StandardCharsets.UTF_8);
                          ParseResult result = PortalEmailThreadParser.parseDetailed(html);
                          total.incrementAndGet();
                          List<ParsedMessage> msgs = result.messages();
                          if (msgs.isEmpty()) {
                              empty.incrementAndGet();
                          } else {
                              emitted.addAndGet(msgs.size());
                              for (ParsedMessage m : msgs) {
                                  if (m.date() == null) unparsedDates.incrementAndGet();
                              }
                          }
                      } catch (IOException ioe) {
                          // Skip unreadable file but don't fail the run \u2014
                          // corpus may contain transient/locked files.
                          System.err.println("Skipped " + p + ": " + ioe.getMessage());
                      }
                  });
        }

        int t = total.get();
        int e = empty.get();
        double ratio = t == 0 ? 0.0 : ((double) e) / t;
        System.out.printf("Corpus: %d emails, %d empty (ratio=%.3f), %d messages emitted, %d unparsed dates%n",
                t, e, ratio, emitted.get(), unparsedDates.get());

        if (t == 0) {
            throw new IllegalStateException("Corpus directory is empty: " + root);
        }
        if (ratio > MAX_FALLBACK_RATIO) {
            throw new AssertionError(String.format(
                    "Fallback ratio %.3f exceeds bound %.3f (corpus=%s)",
                    ratio, MAX_FALLBACK_RATIO, root));
        }
    }
}
