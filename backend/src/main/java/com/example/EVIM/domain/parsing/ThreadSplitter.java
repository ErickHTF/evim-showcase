package com.example.EVIM.domain.parsing;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.EVIM.domain.service.PortalEmailThreadParser.ParsedMessage;

public final class ThreadSplitter {

    private static final Logger log = LoggerFactory.getLogger(ThreadSplitter.class);

    private static final Pattern NEWLINE_CRLF = Pattern.compile("\\r?\\n");
    private static final Pattern NEWLINE = Pattern.compile("\\n");

    private final HeaderDetector headerDetector;
    private final DateRecognizer dateRecognizer;
    private final BoilerplateFilter boilerplateFilter;

    public ThreadSplitter(HeaderDetector headerDetector, DateRecognizer dateRecognizer,
                          BoilerplateFilter boilerplateFilter) {
        this.headerDetector = headerDetector;
        this.dateRecognizer = dateRecognizer;
        this.boilerplateFilter = boilerplateFilter;
    }

    public List<ParsedMessage> split(String plainText) {
        List<String> lines = normalizeLines(plainText);
        log.debug("ThreadSplitter cleaned {} lines", lines.size());

        List<ParsedMessage> messages = new ArrayList<>();
        StringBuilder firstMessageBody = new StringBuilder();
        boolean headerFound = false;
        int i = 0;

        while (i < lines.size()) {
            String line = lines.get(i);
            if (boilerplateFilter.isBoilerplate(line)) { i++; continue; }

            boolean matched = false;
            if (line.length() <= HeaderDetector.MAX_AUTHOR_LINE_LENGTH) {
                Matcher am = HeaderDetector.AUTHOR_LINE.matcher(line);
                if (am.matches()
                        && (am.group(2) != null || headerDetector.isProperName(am.group(1).trim()))
                        && i + 1 < lines.size()) {
                    String author = am.group(1).trim();
                    String company = am.group(2);
                    int dateIdx = headerDetector.findDateLineWithin(lines, i + 1, 5);
                    if (dateIdx >= 0) {
                        Matcher dm = dateRecognizer.matcher(lines.get(dateIdx));
                        dm.find();
                        matched = true;
                        headerFound = true;
                        LocalDateTime date = dateRecognizer.parseDate(dm.group());
                        i = dateIdx + 1;

                        StringBuilder bodyBuilder = new StringBuilder();
                        i = collectBody(lines, i, bodyBuilder);

                        String body = bodyBuilder.toString().trim();
                        if (!body.isEmpty()) {
                            messages.add(new ParsedMessage(author, company, date, body));
                        }
                        continue;
                    } else if (company != null && headerFound) {


                        matched = true;
                        i++;

                        while (i < lines.size() && headerDetector.isHeaderNoise(lines.get(i))) {
                            i++;
                        }

                        StringBuilder bodyBuilder = new StringBuilder();
                        i = collectBody(lines, i, bodyBuilder);

                        String body = bodyBuilder.toString().trim();
                        if (!body.isEmpty()) {
                            messages.add(new ParsedMessage(author, company, null, body));
                        }
                        continue;
                    }
                }
            }

            if (!matched) {
                HeaderDetector.InlineHeader ih = headerDetector.tryInlineHeader(line);
                if (ih != null) {
                    headerFound = true;
                    i++;

                    StringBuilder bodyBuilder = new StringBuilder();
                    if (ih.bodyStart() != null && !ih.bodyStart().isEmpty()) {
                        bodyBuilder.append(ih.bodyStart());
                    }
                    i = collectBody(lines, i, bodyBuilder);

                    if (ih.prefixBody() != null && !ih.prefixBody().isEmpty()) {
                        if (!messages.isEmpty()) {
                            ParsedMessage prev = messages.get(messages.size() - 1);
                            messages.set(messages.size() - 1, new ParsedMessage(
                                    prev.author(), prev.company(), prev.date(),
                                    prev.body() + "\n" + ih.prefixBody()));
                        } else if (firstMessageBody.length() > 0) {
                            firstMessageBody.append("\n").append(ih.prefixBody());
                        }
                    }

                    String body = bodyBuilder.toString().trim();
                    if (!body.isEmpty()) {
                        messages.add(new ParsedMessage(ih.author(), ih.company(), ih.date(), body));
                    }
                    continue;
                }
            }

            if (!headerFound) {
                if (firstMessageBody.length() > 0) firstMessageBody.append("\n");
                firstMessageBody.append(line);
            }
            i++;
        }

        if (headerFound && firstMessageBody.length() > 0) {
            String body = firstMessageBody.toString().trim();
            if (!body.isEmpty()) {
                messages.add(0, new ParsedMessage(null, null, null, body));
            }
        }





        List<ParsedMessage> cleaned = new ArrayList<>();
        for (ParsedMessage msg : messages) {
            String cleanedBody = cleanOrphanHeaders(msg.body());
            cleanedBody = SignatureStripper.strip(cleanedBody);
            if (cleanedBody == null || cleanedBody.isEmpty()) continue;
            cleaned.add(new ParsedMessage(msg.author(), msg.company(), msg.date(), cleanedBody));
        }

        return cleaned;
    }



    private List<String> normalizeLines(String plainText) {
        String[] rawLines = NEWLINE_CRLF.split(plainText);
        List<String> lines = new ArrayList<>();
        boolean prevBlank = false;
        for (String l : rawLines) {
            String trimmed = l.replace('\u00A0', ' ')
                              .replace("\u200B", "")
                              .replace("\uFEFF", "")
                              .trim();
            if (trimmed.isEmpty()) {
                if (!lines.isEmpty() && !prevBlank) {
                    lines.add("");
                    prevBlank = true;
                }
            } else {
                lines.add(trimmed);
                prevBlank = false;
            }
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private int collectBody(List<String> lines, int start, StringBuilder bodyBuilder) {
        int i = start;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (boilerplateFilter.isBoilerplate(line)) { i++; continue; }

            if (line.length() <= HeaderDetector.MAX_AUTHOR_LINE_LENGTH) {
                Matcher am = HeaderDetector.AUTHOR_LINE.matcher(line);
                if (am.matches()
                        && (am.group(2) != null || headerDetector.isProperName(am.group(1).trim()))
                        && i + 1 < lines.size()) {
                    if (headerDetector.findDateLineWithin(lines, i + 1, 5) >= 0) break;

                    if (am.group(2) != null) break;
                }
            }

            if (headerDetector.tryInlineHeader(line) != null) break;

            if (bodyBuilder.length() > 0) bodyBuilder.append("\n");
            bodyBuilder.append(line);
            i++;
        }
        return i;
    }

    private String cleanOrphanHeaders(String body) {
        if (body == null || body.isEmpty()) return "";
        String[] ls = NEWLINE.split(body);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ls.length; i++) {
            String l = ls[i].trim();
            if (l.length() <= HeaderDetector.MAX_AUTHOR_LINE_LENGTH && i + 1 < ls.length) {
                Matcher am = HeaderDetector.AUTHOR_LINE.matcher(l);
                if (am.matches()
                        && (am.group(2) != null || headerDetector.isProperName(am.group(1).trim()))
                        && dateRecognizer.containsDate(ls[i + 1].trim())) {
                    i++;
                    continue;
                }
            }
            if (dateRecognizer.isExactlyDate(l)) continue;
            if (l.length() <= 40 && !l.matches(".*[.!?].*")
                    && dateRecognizer.containsDate(l)) continue;
            if (boilerplateFilter.isBoilerplate(l)) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(l);
        }
        return sb.toString().trim();
    }
}
