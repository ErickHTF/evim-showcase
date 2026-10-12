package com.example.EVIM.domain.parsing;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public final class HtmlToPlainTextConverter {

    private static final Pattern WS_RUN = Pattern.compile("\\s+");

    private final Predicate<String> isProperName;
    private final UnaryOperator<String> sanitizer;

    public HtmlToPlainTextConverter(Predicate<String> isProperName, UnaryOperator<String> sanitizer) {
        this.isProperName = isProperName;
        this.sanitizer = sanitizer;
    }

    public String convert(String html) {
        if (html == null || html.isBlank()) return "";

        String prepared = html;
        if (looksDoubleEntityEncoded(prepared)) {
            prepared = org.jsoup.parser.Parser.unescapeEntities(prepared, false);
        }









        prepared = removeHiddenElements(prepared);

        prepared = sanitizer.apply(prepared);

        Document doc = Jsoup.parse(prepared);
        doc.select("style, script, head").remove();

        for (Element img : doc.select("img[alt]")) {
            String alt = img.attr("alt").trim();
            if (!alt.isEmpty() && alt.length() <= 60 && isProperName.test(alt)) {
                img.replaceWith(new TextNode("\n" + alt + "\n"));
            }
        }
        for (Element a : doc.select("a[href]")) {
            String href = UrlUnwrapper.unwrap(a.attr("href"));
            String text = a.text();
            if (!href.isBlank() && !text.isBlank()) {
                if (text.indexOf(']') >= 0 || href.indexOf(')') >= 0) {
                    a.text(text + " (" + href + ")");
                } else {
                    a.text("[" + text + "](" + href + ")");
                }
            }
        }
        doc.select("img").remove();

        StringBuilder sb = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode tn) {
                    String text = WS_RUN.matcher(tn.getWholeText()).replaceAll(" ");
                    if (!text.isBlank()) sb.append(text);
                } else if (node instanceof Element el) {
                    String tag = el.tagName();
                    if (isBlockTag(tag) && sb.length() > 0 && !sb.toString().endsWith("\n")) {
                        sb.append("\n");
                    }
                    if ("br".equals(tag)) sb.append("\n");
                }
            }
            @Override
            public void tail(Node node, int depth) {
                if (node instanceof Element el) {
                    String tag = el.tagName();
                    if (isBlockTag(tag) && sb.length() > 0) {
                        if (!sb.toString().endsWith("\n")) sb.append("\n");
                        if (!sb.toString().endsWith("\n\n")) sb.append("\n");
                    }
                }
            }
        }, doc.body());

        return sb.toString();
    }

    private static boolean looksDoubleEntityEncoded(String s) {
        if (s == null) return false;


        if (s.contains("&lt;a ") || s.contains("&lt;a&gt;")
                || s.contains("&lt;div") || s.contains("&lt;p")) {
            int real = countOccurrences(s, "<");
            return real == 0 || countOccurrences(s, "&lt;") > real;
        }
        int encoded = countOccurrences(s, "&lt;");
        if (encoded < 2) return false;
        int real = countOccurrences(s, "<");
        return encoded > real;
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) { n++; idx += needle.length(); }
        return n;
    }


    static String removeHiddenElements(String html) {
        if (html == null || html.isBlank()) return html;
        Document doc = Jsoup.parse(html);


        doc.select("[style*=\"display:none\"], [style*=\"display: none\"], "
                + "[style*=\"visibility:hidden\"], [style*=\"visibility: hidden\"], "
                + "[style*=\"opacity:0\"], [style*=\"opacity: 0\"], "
                + "[style*=\"max-height:0\"], [style*=\"max-height: 0\"], "
                + "[style*=\"font-size:1px\"], [style*=\"font-size: 1px\"], "
                + "[hidden], [aria-hidden=true]").remove();
        return doc.outerHtml();
    }

    private static boolean isBlockTag(String tag) {
        return switch (tag) {
            case "div", "p", "tr", "li", "h1", "h2", "h3", "h4", "h5", "h6",
                 "blockquote", "table", "thead", "tbody", "section", "article",
                 "header", "footer", "ul", "ol", "pre" -> true;
            default -> false;
        };
    }
}
