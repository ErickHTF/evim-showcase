package com.example.EVIM.domain.parsing;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateRecognizer {

    private static final Logger log = LoggerFactory.getLogger(DateRecognizer.class);

    public static final Pattern DATE_LINE = Pattern.compile(

            "(?:\\d{1,2}\\s+de\\s+\\p{L}{3,9}\\.?\\s+de\\s+\\d{4},?\\s*\\d{1,2}:\\d{2}(?:\\s*[A-Z]{2,5})?)"

            + "|(?:\\p{L}{3,9}\\.?\\s+\\d{1,2},?\\s+\\d{4},?\\s+\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?(?:\\s*[A-Z]{2,5})?)"

            + "|(?:\\d{1,2}\\s+\\p{L}{3,9}\\.?\\s+\\d{4},?\\s+\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?(?:\\s*[A-Z]{2,5})?)"

            + "|(?:\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4}[\\sT,]+\\d{1,2}:\\d{2}(?::\\d{2})?)"

            + "|(?:\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:?\\d{2})?)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern DATE_PT = Pattern.compile(
            "(\\d{1,2})\\s+de\\s+(\\p{L}{3,9})\\.?\\s+de\\s+(\\d{4}),?\\s*(\\d{1,2}):(\\d{2})",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DATE_EN_MONTH_FIRST = Pattern.compile(
            "(\\p{L}{3,9})\\.?\\s+(\\d{1,2}),?\\s+(\\d{4}),?\\s+(\\d{1,2}):(\\d{2})(?:\\s*([AaPp][Mm]))?",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DATE_EN_DAY_FIRST = Pattern.compile(
            "(\\d{1,2})\\s+(\\p{L}{3,9})\\.?\\s+(\\d{4}),?\\s+(\\d{1,2}):(\\d{2})(?:\\s*([AaPp][Mm]))?",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern DATE_NUMERIC = Pattern.compile(
            "(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})[\\sT,]+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern DATE_ISO = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})[T\\s](\\d{2}):(\\d{2})(?::(\\d{2}))?");

    private static final Map<String, Integer> MONTHS = buildMonths();

    private static Map<String, Integer> buildMonths() {
        Map<String, Integer> m = new HashMap<>();
        addMonth(m, 1, "jan", "janeiro");
        addMonth(m, 2, "fev", "fevereiro");
        addMonth(m, 3, "mar", "marco", "mar\u00e7o");
        addMonth(m, 4, "abr", "abril");
        addMonth(m, 5, "mai", "maio");
        addMonth(m, 6, "jun", "junho");
        addMonth(m, 7, "jul", "julho");
        addMonth(m, 8, "ago", "agosto");
        addMonth(m, 9, "set", "setembro");
        addMonth(m, 10, "out", "outubro");
        addMonth(m, 11, "nov", "novembro");
        addMonth(m, 12, "dez", "dezembro");
        addMonth(m, 1, "jan", "january");
        addMonth(m, 2, "feb", "february");
        addMonth(m, 3, "mar", "march");
        addMonth(m, 4, "apr", "april");
        addMonth(m, 5, "may");
        addMonth(m, 6, "jun", "june");
        addMonth(m, 7, "jul", "july");
        addMonth(m, 8, "aug", "august");
        addMonth(m, 9, "sep", "sept", "september");
        addMonth(m, 10, "oct", "october");
        addMonth(m, 11, "nov", "november");
        addMonth(m, 12, "dec", "december");
        addMonth(m, 1, "ene", "enero");
        addMonth(m, 2, "feb", "febrero");
        addMonth(m, 4, "abr", "abril");
        addMonth(m, 8, "ago", "agosto");
        addMonth(m, 12, "dic", "diciembre");
        return Map.copyOf(m);
    }

    private static void addMonth(Map<String, Integer> m, int idx, String... names) {
        for (String n : names) m.putIfAbsent(n, idx);
    }

    public boolean containsDate(String text) {
        return text != null && DATE_LINE.matcher(text).find();
    }

    public boolean isExactlyDate(String text) {
        return text != null && DATE_LINE.matcher(text).matches();
    }

    public Matcher matcher(String text) {
        return DATE_LINE.matcher(text);
    }

    public LocalDateTime parseDate(String dateText) {
        if (dateText == null || dateText.isBlank()) return null;
        String text = dateText.trim();

        Matcher m = DATE_PT.matcher(text);
        if (m.find()) {
            return buildDate(m.group(3), monthFromName(m.group(2)),
                    m.group(1), m.group(4), m.group(5), null);
        }
        m = DATE_EN_MONTH_FIRST.matcher(text);
        if (m.find()) {
            return buildDate(m.group(3), monthFromName(m.group(1)),
                    m.group(2), m.group(4), m.group(5), m.group(6));
        }
        m = DATE_EN_DAY_FIRST.matcher(text);
        if (m.find()) {
            return buildDate(m.group(3), monthFromName(m.group(2)),
                    m.group(1), m.group(4), m.group(5), m.group(6));
        }
        m = DATE_ISO.matcher(text);
        if (m.find()) {
            return buildDate(m.group(1), tryParseInt(m.group(2)),
                    m.group(3), m.group(4), m.group(5), null);
        }
        m = DATE_NUMERIC.matcher(text);
        if (m.find()) {
            return buildDate(m.group(3), tryParseInt(m.group(2)),
                    m.group(1), m.group(4), m.group(5), null);
        }
        log.debug("Unparseable date line: {}", text);
        return null;
    }

    private static Integer monthFromName(String name) {
        if (name == null) return null;
        String key = name.toLowerCase().replaceAll("\\.$", "");
        return MONTHS.get(key);
    }

    private static Integer tryParseInt(String s) {
        if (s == null) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDateTime buildDate(String yearStr, Integer month, String dayStr,
                                           String hourStr, String minuteStr, String ampm) {
        if (month == null) {
            log.debug("Unknown month in date parse, dropping");
            return null;
        }
        try {
            int year = Integer.parseInt(yearStr);
            int day = Integer.parseInt(dayStr);
            int hour = Integer.parseInt(hourStr);
            int minute = Integer.parseInt(minuteStr);
            if (ampm != null) {
                String ap = ampm.toLowerCase();
                if (ap.equals("pm") && hour < 12) hour += 12;
                else if (ap.equals("am") && hour == 12) hour = 0;
            }
            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (NumberFormatException | java.time.DateTimeException e) {
            log.debug("Invalid date components y={} m={} d={} h={} min={}: {}",
                    yearStr, month, dayStr, hourStr, minuteStr, e.getMessage());
            return null;
        }
    }
}
