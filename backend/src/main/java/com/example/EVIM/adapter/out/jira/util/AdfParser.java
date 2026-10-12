package com.example.EVIM.adapter.out.jira.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class AdfParser {

    public static List<String> extractMediaFilenames(JsonNode adfDocument) {
        List<String> filenames = new ArrayList<>();
        collectMediaFilenames(adfDocument, filenames);
        return filenames;
    }

    private static void collectMediaFilenames(JsonNode node, List<String> filenames) {
        if (node == null) return;
        if ("media".equals(node.path("type").asText())) {
            String alt = node.path("attrs").path("alt").asText();
            if (!alt.isEmpty()) filenames.add(alt);
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) collectMediaFilenames(child, filenames);
        }
    }

    public static String adfToPlainText(JsonNode adfDocument) {
        if (adfDocument == null || !adfDocument.has("content")) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        JsonNode content = adfDocument.get("content");

        if (content.isArray()) {
            for (JsonNode block : content) {
                result.append(parseBlock(block));
            }
        }

        return result.toString().trim();
    }

    private static String parseBlock(JsonNode block) {
        String type = block.has("type") ? block.get("type").asText() : "";

        switch (type) {
            case "paragraph":
                return parseParagraph(block) + "\n";
            case "heading":
                return parseHeading(block) + "\n";
            case "bulletList":
            case "orderedList":
                return parseList(block) + "\n";
            case "codeBlock":
                return parseCodeBlock(block) + "\n";
            default:
                return "";
        }
    }

    private static String parseParagraph(JsonNode paragraph) {
        if (!paragraph.has("content")) return "";

        StringBuilder text = new StringBuilder();
        for (JsonNode inline : paragraph.get("content")) {
            text.append(parseInline(inline));
        }
        return text.toString();
    }

    private static String parseInline(JsonNode inline) {
        String type = inline.has("type") ? inline.get("type").asText() : "";

        switch (type) {
            case "text":
                return inline.has("text") ? inline.get("text").asText() : "";

            case "mention":
                if (inline.has("attrs") && inline.get("attrs").has("text")) {
                    return inline.get("attrs").get("text").asText();
                }
                return "@menção";

            case "hardBreak":
                return "\n";

            case "emoji":
                if (inline.has("attrs") && inline.get("attrs").has("shortName")) {
                    return inline.get("attrs").get("shortName").asText();
                }
                return "";

            case "inlineCard":

                if (inline.has("attrs") && inline.get("attrs").has("url")) {
                    return inline.get("attrs").get("url").asText();
                }
                return "";

            default:
                return "";
        }
    }

    private static String parseHeading(JsonNode heading) {
        String text = parseParagraph(heading);
        int level = heading.has("attrs") && heading.get("attrs").has("level")
                ? heading.get("attrs").get("level").asInt()
                : 1;
        return "#".repeat(level) + " " + text;
    }

    private static String parseList(JsonNode list) {
        if (!list.has("content")) return "";

        StringBuilder result = new StringBuilder();
        int index = 1;
        boolean isOrdered = "orderedList".equals(list.get("type").asText());

        for (JsonNode item : list.get("content")) {
            if ("listItem".equals(item.get("type").asText())) {
                String prefix = isOrdered ? (index++ + ". ") : "• ";
                result.append(prefix).append(parseParagraph(item)).append("\n");
            }
        }
        return result.toString();
    }

    private static String parseCodeBlock(JsonNode codeBlock) {
        if (!codeBlock.has("content")) return "";

        StringBuilder code = new StringBuilder("```\n");
        for (JsonNode line : codeBlock.get("content")) {
            if (line.has("text")) {
                code.append(line.get("text").asText()).append("\n");
            }
        }
        code.append("```");
        return code.toString();
    }
}