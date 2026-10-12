package com.example.EVIM.adapter.out.jira.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdfBuilder {

    public static Map<String, Object> buildTextWithLineBreaks(String text) {
        Map<String, Object> adfDocument = new HashMap<>();
        adfDocument.put("version", 1);
        adfDocument.put("type", "doc");

        List<Map<String, Object>> paragraphs = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            paragraphs.add(createParagraph(""));
        } else {
            String[] lines = text.split("\\r?\\n");

            for (String line : lines) {
                paragraphs.add(createParagraph(line));
            }
        }

        adfDocument.put("content", paragraphs);
        return adfDocument;
    }

    private static Map<String, Object> createParagraph(String text) {
        Map<String, Object> paragraphNode = new HashMap<>();
        paragraphNode.put("type", "paragraph");

        if (text != null && !text.isEmpty()) {
            Map<String, Object> textNode = new HashMap<>();
            textNode.put("type", "text");
            textNode.put("text", text);

            paragraphNode.put("content", List.of(textNode));
        } else {
            paragraphNode.put("content", new ArrayList<>());
        }

        return paragraphNode;
    }
}