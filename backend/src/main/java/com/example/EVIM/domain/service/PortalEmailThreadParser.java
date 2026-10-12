package com.example.EVIM.domain.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.example.EVIM.domain.parsing.BoilerplateFilter;
import com.example.EVIM.domain.parsing.DateRecognizer;
import com.example.EVIM.domain.parsing.HeaderDetector;
import com.example.EVIM.domain.parsing.HtmlToPlainTextConverter;
import com.example.EVIM.domain.parsing.ThreadSplitter;

public class PortalEmailThreadParser {


    private static final DateRecognizer DATE_RECOGNIZER = new DateRecognizer();
    private static final BoilerplateFilter BOILERPLATE_FILTER = new BoilerplateFilter();
    private static final HeaderDetector HEADER_DETECTOR = new HeaderDetector(DATE_RECOGNIZER);
    private static final HtmlToPlainTextConverter HTML_CONVERTER =
            new HtmlToPlainTextConverter(HEADER_DETECTOR::isProperName, HtmlSanitizer::sanitize);
    private static final ThreadSplitter THREAD_SPLITTER =
            new ThreadSplitter(HEADER_DETECTOR, DATE_RECOGNIZER, BOILERPLATE_FILTER);


    public record ParsedMessage(String author, String company, LocalDateTime date, String body) {}

    public record ParseResult(List<ParsedMessage> messages, String plainText) {
        public boolean isEmpty() { return messages.isEmpty(); }
    }



    public static List<ParsedMessage> parse(String htmlBody) {
        return parseDetailed(htmlBody).messages();
    }

    public static ParseResult parseDetailed(String htmlBody) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return new ParseResult(Collections.emptyList(), "");
        }
        String plainText = HTML_CONVERTER.convert(htmlBody);
        List<ParsedMessage> messages = THREAD_SPLITTER.split(plainText);
        return new ParseResult(messages, plainText);
    }

    public static String htmlToPlainText(String html) {
        return HTML_CONVERTER.convert(html);
    }

    public static String stripBoilerplate(String plainText) {
        return BOILERPLATE_FILTER.stripFromText(plainText, HEADER_DETECTOR::isProperName);
    }
}
