package com.example.EVIM.domain.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class SignatureStripper {

    private static final Pattern NEWLINE = Pattern.compile("\\r?\\n");


    private static final Pattern EMAIL_LINE = Pattern.compile(
            "^[\\p{Punct}\\s]*[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}[\\p{Punct}\\s]*$",
            Pattern.CASE_INSENSITIVE);


    private static final Pattern PHONE_LINE = Pattern.compile(
            "^[\\p{Punct}\\s]*"
                    + "(?:\\+?\\d{1,3}[\\s.()-]*)?"
                    + "(?:\\(?\\d{2,4}\\)?[\\s.()-]*)?"
                    + "\\d{3,5}[\\s.()-]+\\d{3,5}"
                    + "(?:[\\s.()/-]+(?:R[:.]\\s*\\d+|ramal\\s*\\d+|ext\\.?\\s*\\d+|"
                    + "(?:\\+?\\d{1,3}[\\s.()-]*)?\\(?\\d{2,4}\\)?[\\s.()-]*\\d{3,5}[\\s.()-]+\\d{3,5}))*"
                    + "[\\s.()/-]*"
                    + "(?:op[c\u00e7][o\u00f5]es?\\s*\\d+(?:\\s*e\\s*\\d+)?\\.?)?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);


    private static final Pattern URL_LINE = Pattern.compile(
            "^\\s*(?:https?://|www\\.)\\S+\\s*$", Pattern.CASE_INSENSITIVE);


    private static final Pattern CLOSING_DELIMITER = Pattern.compile(
            "^(?:atenciosamente|atte|att|abs|abra[\u00e7c]os?|obrigad[oa]s?|"
                    + "regards|best\\s+regards|sincerely|cheers|kind\\s+regards|thanks)"
                    + "\\s*[.,!]?\\s*$",
            Pattern.CASE_INSENSITIVE);


    private static final Pattern OPENING_GREETING = Pattern.compile(
            "^(?:ol[\u00e1a]|oi|prezad[oa]s?|caro|cara|bom\\s+dia|boa\\s+tarde|boa\\s+noite|"
                    + "hello|hi|dear|good\\s+(?:morning|afternoon|evening))\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);


    private static final Pattern BARE_GREETING = Pattern.compile(
            "^(?:ol[\u00e1a]|oi|prezad[oa]s?|caro|cara|bom\\s+dia|boa\\s+tarde|boa\\s+noite|"
                    + "hello|hi|dear|good\\s+(?:morning|afternoon|evening))"
                    + "(?:\\s+[\\p{L}'\\-]+){0,3}\\s*[,.!]?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);


    private static final Pattern SEPARATOR_LINE = Pattern.compile(
            "^[\\-_=*\u2500-\u257f]{3,}\\s*$");


    private static final Pattern ALLCAPS_NAME = Pattern.compile(
            "^[\\p{Lu}][\\p{Lu}'\\-]+"
                    + "(?:[\\s/\\-]+[\\p{Lu}][\\p{Lu}'\\-]+){1,}"
                    + "\\s*$",
            Pattern.UNICODE_CHARACTER_CLASS);


    private static final Pattern DEPT_LINE = Pattern.compile(
            "^(?:sustenta[\u00e7c][\u00e3a]o|financial|core\\s+banking|"
                    + "cr[\u00e9e]dito|d[\u00edi]gital|consultor[ai]?(?:\\s+\\w+)*|"
                    + "analista(?:\\s+\\w+)*|gerente(?:\\s+\\w+)*|coordenador[a]?(?:\\s+\\w+)*|"
                    + "engineer(?:ing)?|developer|manager|director|head\\s+of)\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);


    private static final Pattern SLOGAN_LINE = Pattern.compile(
            "^[\\p{Lu}][\\p{Lu}\\s'\\-]{8,}\\s*[.!]?\\s*$",
            Pattern.UNICODE_CHARACTER_CLASS);


    private static final Pattern TITLECASE_NAME = Pattern.compile(
            "^[\\p{Lu}][\\p{Ll}\u00c0-\u017f'\\-]{1,}"
                    + "(?:\\s+(?:de|da|do|dos|das|e|di|van|von|"
                    + "[\\p{Lu}][\\p{Ll}\u00c0-\u017f'\\-]{1,})){1,4}\\s*$",
            Pattern.UNICODE_CHARACTER_CLASS);

    private enum LineKind { SIGNATURE, WEAK_SIGNATURE, DELIMITER, BLANK, CONTENT }

    private SignatureStripper() {}


    public static String strip(String body) {
        if (body == null || body.isBlank()) return body;

        String[] raw = NEWLINE.split(body);
        List<String> lines = new ArrayList<>(raw.length);
        for (String l : raw) lines.add(l == null ? "" : l);

        int end = lines.size();
        while (end > 0 && lines.get(end - 1).trim().isEmpty()) end--;
        if (end == 0) return body;

        int cut = end;
        boolean strongSignal = false;
        int blankRun = 0;
        for (int i = end - 1; i >= 0; i--) {
            String t = lines.get(i).trim();
            LineKind kind = classify(t);
            if (kind == LineKind.CONTENT) break;
            if (kind == LineKind.WEAK_SIGNATURE && !strongSignal) break;
            if (kind == LineKind.BLANK) {
                blankRun++;
                if (blankRun >= 2) break;
                continue;
            }
            blankRun = 0;
            if (kind == LineKind.SIGNATURE) strongSignal = true;
            else if (kind == LineKind.DELIMITER && CLOSING_DELIMITER.matcher(t).matches()) {
                strongSignal = true;
            }
            cut = i;
        }

        if (!strongSignal || cut >= end) return body;



        while (cut > 0 && lines.get(cut - 1).trim().isEmpty()) cut--;

        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < cut; i++) {
            if (kept.length() > 0) kept.append('\n');
            kept.append(lines.get(i));
        }
        String result = kept.toString().stripTrailing();
        if (result.isBlank()) return body;

        boolean hasRealContent = false;
        for (String l : NEWLINE.split(result)) {
            String t = l.trim();
            if (t.isEmpty()) continue;



            if (BARE_GREETING.matcher(t).matches()) continue;
            if (classify(t) == LineKind.CONTENT) { hasRealContent = true; break; }
        }
        if (!hasRealContent) return body;
        return result;
    }


    public static boolean isEffectivelyEmpty(String body) {
        if (body == null || body.isBlank()) return true;
        boolean strongSignal = false;
        for (String line : NEWLINE.split(body)) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (BARE_GREETING.matcher(t).matches()) continue;
            LineKind k = classify(t);
            if (k == LineKind.CONTENT) return false;
            if (k == LineKind.SIGNATURE) strongSignal = true;
            else if (k == LineKind.DELIMITER && CLOSING_DELIMITER.matcher(t).matches()) {
                strongSignal = true;
            }
        }



        return strongSignal;
    }

    private static LineKind classify(String trimmed) {
        if (trimmed.isEmpty()) return LineKind.BLANK;
        if (CLOSING_DELIMITER.matcher(trimmed).matches()) return LineKind.DELIMITER;
        if (SEPARATOR_LINE.matcher(trimmed).matches()) return LineKind.DELIMITER;
        if (EMAIL_LINE.matcher(trimmed).matches()) return LineKind.SIGNATURE;
        if (URL_LINE.matcher(trimmed).matches()) return LineKind.SIGNATURE;
        if (PHONE_LINE.matcher(trimmed).matches()) return LineKind.SIGNATURE;
        if (ALLCAPS_NAME.matcher(trimmed).matches()) return LineKind.SIGNATURE;
        if (SLOGAN_LINE.matcher(trimmed).matches()) return LineKind.SIGNATURE;
        if (DEPT_LINE.matcher(trimmed).matches()) return LineKind.SIGNATURE;
        if (TITLECASE_NAME.matcher(trimmed).matches()) return LineKind.WEAK_SIGNATURE;
        return LineKind.CONTENT;
    }
}
