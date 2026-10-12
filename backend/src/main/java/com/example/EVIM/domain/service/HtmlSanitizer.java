package com.example.EVIM.domain.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "div", "span",
                    "strong", "b", "em", "i", "u",
                    "a", "ul", "ol", "li",
                    "code", "pre", "blockquote",
                    "table", "thead", "tbody", "tr", "td", "th",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "img", "hr")
            .allowAttributes("href").onElements("a")
            .allowAttributes("target").onElements("a")




            .allowStandardUrlProtocols()
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            .allowAttributes("style").globally()
            .requireRelNofollowOnLinks()
            .toFactory();

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return "";
        String fixed = TextNormalizer.fixDoubleEncodedUtf8(html);
        return POLICY.sanitize(fixed);
    }

    public static String plainTextToHtml(String text) {
        if (text == null || text.isBlank()) return "";



        String prepared = text.replaceAll(
                "<a\\s[^>]*href=[\"']([^\"']+)[\"'][^>]*>([^<]+)</a>",
                "[$2]($1)");

        String escaped = prepared
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");


        escaped = escaped.replaceAll("\\[img\\]\\([^)]+\\)", "");





        java.util.regex.Pattern md = java.util.regex.Pattern.compile(
                "\\[((?:\\\\.|[^\\]\\\\])+)\\]\\(((?:\\\\.|[^)\\\\])+)\\)");
        java.util.regex.Matcher mdMatcher = md.matcher(escaped);
        StringBuilder mdOut = new StringBuilder();
        while (mdMatcher.find()) {
            String linkText = unescapeBackslashes(mdMatcher.group(1));
            String href = com.example.EVIM.domain.parsing.UrlUnwrapper.unwrap(
                    unescapeBackslashes(mdMatcher.group(2)));
            String replacement = "<a href=\"" + href.replace("\"", "&quot;")
                    + "\" target=\"_blank\" rel=\"nofollow noreferrer\">"
                    + linkText + "</a>";
            mdMatcher.appendReplacement(mdOut, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        mdMatcher.appendTail(mdOut);
        escaped = mdOut.toString();

        java.util.regex.Pattern bareUrl = java.util.regex.Pattern.compile(
                "(?<![\"'>])(https?://[^\\s<\"']+)");
        java.util.regex.Matcher bareMatcher = bareUrl.matcher(escaped);
        StringBuilder bareOut = new StringBuilder();
        while (bareMatcher.find()) {
            String href = com.example.EVIM.domain.parsing.UrlUnwrapper.unwrap(bareMatcher.group(1));
            String replacement = "<a href=\"" + href.replace("\"", "&quot;")
                    + "\" target=\"_blank\" rel=\"nofollow noreferrer\">" + href + "</a>";
            bareMatcher.appendReplacement(bareOut, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        bareMatcher.appendTail(bareOut);
        escaped = bareOut.toString();

        escaped = escaped.replace("\n", "<br>");

        return escaped;
    }

    private static String unescapeBackslashes(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                out.append(s.charAt(++i));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
