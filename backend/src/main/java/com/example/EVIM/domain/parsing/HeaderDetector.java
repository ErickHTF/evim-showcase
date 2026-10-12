package com.example.EVIM.domain.parsing;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeaderDetector {

    private static final Logger log = LoggerFactory.getLogger(HeaderDetector.class);

    public static final int MAX_AUTHOR_LINE_LENGTH = 100;

    public static final Pattern AUTHOR_LINE = Pattern.compile(
            "^(\\p{Lu}[\\p{L}'\\-\\s]{2,80}?)(?:\\s*\\(([^)]+)\\))?\\s*[:\\-\u2013]?\\s*$",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern COMPANY_SUFFIX = Pattern.compile("\\(([^)]+)\\)\\s*$");



    private static final Pattern WS_SPLIT = Pattern.compile("\\s+");
    private static final Pattern PREPOSITION = Pattern.compile("(?i)^(de|da|do|dos|das|e|di|van|von)$");
    private static final Pattern PREPOSITION_SHORT = Pattern.compile("(?i)^(de|da|do|dos|das|e)$");
    private static final Pattern ENDS_WITH_PUNCT = Pattern.compile(".*[,;?!:]$");
    private static final Pattern ABBREV_DOT = Pattern.compile("^[A-Z][a-z]?\\.\\$");
    private static final Pattern ABBREV_DOT_REAL = Pattern.compile("^[A-Z][a-z]?\\.$");
    private static final Pattern HAS_SENTENCE_PUNCT = Pattern.compile(".*[,;?!].*");
    private static final Pattern LEADING_EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\s+");
    private static final Pattern HEADER_NOISE_BLANK = Pattern.compile("\\s*");
    private static final Pattern HEADER_NOISE_SEP = Pattern.compile("^[\\-=_|*\u2500-\u257f]{1,10}$");
    private static final Pattern HEADER_NOISE_VIA_EMAIL = Pattern.compile("(?i)^via\\s+email$");

    private static final Set<String> NON_NAME_WORDS = Set.of(
            "obrigado", "obrigada", "ol\u00e1", "oi", "bom", "boa",
            "prezado", "prezada", "atenciosamente", "att", "segue",
            "sim", "n\u00e3o", "ok", "recebido", "certo", "perfeito"
    );

    public record InlineHeader(String author, String company, LocalDateTime date,
                               String bodyStart, String prefixBody) {}

    private final DateRecognizer dateRecognizer;

    public HeaderDetector(DateRecognizer dateRecognizer) {
        this.dateRecognizer = dateRecognizer;
    }

    public static boolean startsWithUpper(String word) {
        if (word == null || word.isEmpty()) return false;
        int cp = word.codePointAt(0);
        return Character.isUpperCase(cp);
    }

    public boolean isProperName(String text) {
        if (text == null || text.length() < 4) return false;
        String[] words = WS_SPLIT.split(text);
        if (words.length < 2) return false;
        int nameWords = 0;
        int totalWords = 0;
        for (String w : words) {
            if (w.isEmpty()) continue;
            totalWords++;
            boolean isCap = startsWithUpper(w);
            boolean isPreposition = PREPOSITION.matcher(w).matches();
            if (isCap || isPreposition) nameWords++;
        }



        return nameWords >= 2 && nameWords * 2 >= totalWords;
    }

    public String extractNameFromEnd(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] words = WS_SPLIT.split(text);

        List<String> nameWords = new ArrayList<>();
        for (int j = words.length - 1; j >= 0; j--) {
            String w = words[j];
            if (w.isEmpty()) continue;

            boolean endsWithPunct = ENDS_WITH_PUNCT.matcher(w).matches();
            boolean endsWithDot = w.endsWith(".") && !ABBREV_DOT_REAL.matcher(w).matches();
            if (endsWithPunct || endsWithDot) break;

            boolean isCap = startsWithUpper(w);
            boolean isPreposition = PREPOSITION_SHORT.matcher(w).matches();
            boolean isAbbrev = ABBREV_DOT_REAL.matcher(w).matches();

            if (isCap || isPreposition || isAbbrev) {
                nameWords.add(0, w);
            } else {
                break;
            }
        }

        while (!nameWords.isEmpty()
                && NON_NAME_WORDS.contains(nameWords.get(0).toLowerCase())) {
            nameWords.remove(0);
        }
        while (!nameWords.isEmpty()
                && PREPOSITION.matcher(nameWords.get(nameWords.size() - 1)).matches()) {
            nameWords.remove(nameWords.size() - 1);
        }

        if (nameWords.isEmpty()) return null;
        long capCount = nameWords.stream().filter(HeaderDetector::startsWithUpper).count();
        if (capCount < 2) return null;
        return String.join(" ", nameWords);
    }

    public InlineHeader tryInlineHeader(String line) {
        if (line == null || line.isEmpty()) return null;
        Matcher dm = dateRecognizer.matcher(line);
        if (!dm.find()) return null;

        String prefix = line.substring(0, dm.start()).trim();
        String afterDate = dm.end() < line.length() ? line.substring(dm.end()).trim() : "";

        if (prefix.isEmpty()) return null;

        String company = null;
        Matcher cm = COMPANY_SUFFIX.matcher(prefix);
        if (cm.find()) {
            company = cm.group(1);
            prefix = prefix.substring(0, cm.start()).trim();
        }

        String afterEmail = LEADING_EMAIL.matcher(prefix).replaceFirst("");

        String author;
        String prefixBody = "";
        boolean hasSentencePunct = HAS_SENTENCE_PUNCT.matcher(afterEmail).matches();
        if (afterEmail.length() <= MAX_AUTHOR_LINE_LENGTH && !hasSentencePunct) {
            author = afterEmail.trim();
        } else {
            author = extractNameFromEnd(afterEmail);
            if (author != null) {
                int authorIdx = afterEmail.lastIndexOf(author);
                if (authorIdx > 0) {
                    prefixBody = afterEmail.substring(0, authorIdx).trim();
                }
                if (log.isDebugEnabled() && hasSentencePunct
                        && afterEmail.length() <= MAX_AUTHOR_LINE_LENGTH) {
                    log.debug("Inline header divergence: simple-path='{}' extract-path='{}' prefixBody='{}'",
                            afterEmail.trim(), author, prefixBody);
                }
            }
        }

        if (author == null || author.length() < 3) return null;
        if (!startsWithUpper(author)) return null;

        String[] words = WS_SPLIT.split(author);
        if (words.length > 8) return null;
        if (company == null && words.length < 2) return null;

        int caps = 0;
        for (String w : words) {
            if (startsWithUpper(w)) caps++;
        }
        if (caps < 1) return null;
        if (company == null && caps < 2) return null;

        LocalDateTime date = dateRecognizer.parseDate(dm.group());
        return new InlineHeader(author, company, date, afterDate, prefixBody);
    }

    public int findDateLineWithin(List<String> lines, int from, int maxAhead) {
        int end = Math.min(lines.size(), from + maxAhead);
        for (int k = from; k < end; k++) {
            String s = lines.get(k);
            if (dateRecognizer.containsDate(s)) return k;
            if (!isHeaderNoise(s)) return -1;
        }
        return -1;
    }

    public boolean isHeaderNoise(String s) {
        if (s == null) return true;
        return HEADER_NOISE_BLANK.matcher(s).matches()
                || s.equals("|")
                || HEADER_NOISE_SEP.matcher(s).matches()
                || HEADER_NOISE_VIA_EMAIL.matcher(s).matches();
    }
}
