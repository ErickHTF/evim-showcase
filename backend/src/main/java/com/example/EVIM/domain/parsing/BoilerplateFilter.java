package com.example.EVIM.domain.parsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BoilerplateFilter {

    private static final Logger log = LoggerFactory.getLogger(BoilerplateFilter.class);

    private static final Pattern NEWLINE_CRLF = Pattern.compile("\\r?\\n");
    private static final Pattern NEWLINE = Pattern.compile("\\n");
    private static final Pattern SHORT_CLOSING = Pattern.compile(
            "(?i)^(att|abs|abra[\u00e7c]os?|obrigad[oa]s?)\\s*[.,!]?$");

    public record BoilerplateRule(String name, Pattern pattern, int minLineLength, int priority)
            implements Comparable<BoilerplateRule> {

        public BoilerplateRule(String name, Pattern pattern, int minLineLength) {
            this(name, pattern, minLineLength, 0);
        }

        public BoilerplateRule {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
            if (pattern == null) throw new IllegalArgumentException("pattern required");
            if (minLineLength < 0) throw new IllegalArgumentException("minLineLength >= 0");
        }

        @Override
        public int compareTo(BoilerplateRule other) {
            int c = Integer.compare(other.priority(), this.priority());
            if (c != 0) return c;
            c = Integer.compare(other.minLineLength(), this.minLineLength());
            if (c != 0) return c;
            return Integer.compare(other.pattern().pattern().length(), this.pattern().pattern().length());
        }
    }

    public static final BoilerplateRule[] DEFAULT_RULES = {
            new BoilerplateRule("vendor-portal.do-not-write-below",
                    Pattern.compile("^##.*N[\u00e3a]o escreva abaixo desta linha.*##$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.portal-header",




                    Pattern.compile("^[_\\-=*\u2500-\u257f\\s]*Portal de Atendimento\\b.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.solicitacao-numero",
                    Pattern.compile("^Solicita[\u00e7c][\u00e3a]o\\s+n[\u00ba\u00b0o]?[:.]?\\s*\\d+\\.?$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.data-abertura",
                    Pattern.compile("^Data de Abertura\\s*[:.].*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.ultimo-status",
                    Pattern.compile("^[\u00daU]ltimo status\\s*[:.].*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.solicitacao-atualizada",






                    Pattern.compile("^Sua solicita[\u00e7c][\u00e3a]o[^\\n]*\\d[^\\n]*foi\\s+(atualizada|recebida|encerrada|finalizada)\\b.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.notify-updates",
                    Pattern.compile("^Voc[\u00eae] ser[\u00e1a] notificado das atualiza[\u00e7c][\u00f5o]es deste atendimento.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.interact-instructions",
                    Pattern.compile("^Se necess[\u00e1a]rio interagir na sua solicita[\u00e7c][\u00e3a]o.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.cc-warning-pt",
                    Pattern.compile("^Voc[\u00eae] est[\u00e1a] registrado como CC.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.cc-warning-short",
                    Pattern.compile("^Se estiver copiado \\(em CC\\).*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("external.tag-pt",

                    Pattern.compile("^\\[EXTERNO\\].*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("external.banner-pt",
                    Pattern.compile("^Esta mensagem foi enviada por um remetente externo.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("portal.reply-instruction",
                    Pattern.compile("^Responda este email para adicionar.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("portal.add-comments",
                    Pattern.compile("^Para adicionar outros coment[\u00e1a]rios,?\\s*responda a este email.*$", Pattern.CASE_INSENSITIVE), 0),




            new BoilerplateRule("vendor-portal.csat-question",
                    Pattern.compile("^Queremos saber a sua opini[\u00e3a]o sobre o nosso atendimento.*$",
                            Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.csat-instruction",
                    Pattern.compile("^Atribua seu n[\u00edi]vel de satisfa[\u00e7c][\u00e3a]o.*$",
                            Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.csat-thanks",
                    Pattern.compile("^A sua opini[\u00e3a]o ser[\u00e1a] valorizada.*$",
                            Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.csat-acknowledgement",
                    Pattern.compile("^Agradecemos sua contribui[\u00e7c][\u00e3a]o\\.?$",
                            Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("vendor-portal.csat-subject-reminder",
                    Pattern.compile("^Este [\u00e9e] um lembrete do assunto desta solicita[\u00e7c][\u00e3a]o:?$",
                            Pattern.CASE_INSENSITIVE), 0),




            new BoilerplateRule("vendor-portal.csat-score-line",
                    Pattern.compile("^\\s*\\[?(?:[0-9]|10)\\]?(?:\\([^)]*\\))?\\s*[.,]?\\s*$"), 0),
            new BoilerplateRule("portal.via-email",
                    Pattern.compile("^via email$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("decorative.separator",
                    Pattern.compile("^[_\\-=*\u2500-\u257f]{3,}$"), 0),
            new BoilerplateRule("signature.sent-from-mobile-en",
                    Pattern.compile("^sent\\s+from\\s+my\\s+(iphone|ipad|samsung|android).*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("signature.sent-from-mobile-pt",
                    Pattern.compile("^enviado\\s+do\\s+meu\\s+(iphone|ipad|celular|dispositivo).*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("forward.original-message",
                    Pattern.compile("^-{2,}\\s*(mensagem\\s+original|original\\s+message).*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("forward.from-line",
                    Pattern.compile("^de:\\s+.*@.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("forward.to-line",
                    Pattern.compile("^para:\\s+.*@.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("forward.sent-line",
                    Pattern.compile("^enviado:\\s+\\d.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("forward.subject-line",
                    Pattern.compile("^assunto:\\s+.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("portal.click-here",
                    Pattern.compile("^clique aqui\\s*\\.?$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("signature.atenciosamente",
                    Pattern.compile("^Atenciosamente[.,]?$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("portal.attachment-label",
                    Pattern.compile("^Anexo\\(s\\)$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("contact.phone-br",
                    Pattern.compile("^\\d{4}[-.]\\d{4}.*$"), 0),
            new BoilerplateRule("portal.tracking-id",
                    Pattern.compile("^\\[[A-Z0-9]{4,}-[A-Z0-9]{4,}\\]$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("standalone.email-address",
                    Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("standalone.filename",
                    Pattern.compile("^\\S{1,80}\\.(docx?|xlsx?|pdf|zip|rar|png|jpe?g|gif|webp|log|csv|txt)$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("signature.short-closing",
                    Pattern.compile("^(att|abs|abra[\u00e7c]os?|obrigad[oa]s?)\\s*[.,!]?$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("external.banner-en",


                    Pattern.compile("^\\[EXTERNAL\\]\\s*This\\s+Message\\s+Is\\s+From\\s+an\\s+External\\s+Sender.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("external.guidance-en",
                    Pattern.compile("^do\\s+not\\s+follow\\s+guidance,\\s+click\\s+links,\\s+or\\s+open\\s+attachments.*$", Pattern.CASE_INSENSITIVE), 0),
            new BoilerplateRule("proofpoint.banner-marker",
                    Pattern.compile("ZjQcmQRYFpfptBanner(Start|End)"), 0),

            new BoilerplateRule("disclaimer.generic",
                    Pattern.compile("(?i)\\bdisclaimer\\b"), 80),
            new BoilerplateRule("confidentiality.notice-pt",
                    Pattern.compile("(?i)aviso\\s+de\\s+confidencialidade"), 60),
            new BoilerplateRule("confidentiality.message-pt",
                    Pattern.compile("(?i)esta\\s+mensagem.*confidencial"), 60),
            new BoilerplateRule("confidentiality.recipient-pt",
                    Pattern.compile("(?i)se\\s+voc[\u00eae]\\s+n[\u00e3a]o\\s+[\u00e9e]\\s+o\\s+destinat[\u00e1a]rio"), 60),
            new BoilerplateRule("security.no-instructions-pt",
                    Pattern.compile("(?i)n[\u00e3a]o\\s+siga\\s+instru[\u00e7c][\u00f5o]es.*clique\\s+em\\s+links"), 40),
            new BoilerplateRule("security.validate-before-action-pt",
                    Pattern.compile("(?i)sempre\\s+valide\\s+antes\\s+de\\s+qualquer\\s+a[\u00e7c][\u00e3a]o"), 40),
            new BoilerplateRule("attachment.size-limit",
                    Pattern.compile("(?i)\\blimite\\s+de\\s+\\d+\\s*mb\\b"), 30),
            new BoilerplateRule("attachment.cant-view",
                    Pattern.compile("(?i)n[\u00e3a]o\\s+conseguir\\s+visualizar\\s+o\\s+anexo"), 40),
            new BoilerplateRule("portal.support-request-id",
                    Pattern.compile("(?i)solicita[c\u00e7][a\u00e3]o\\s+de\\s+suporte\\s+\\(\\d+\\)"), 50),
    };

    private final BoilerplateRule[] rules;
    private final java.util.function.Consumer<String> onRuleMatched;

    public BoilerplateFilter() { this(DEFAULT_RULES); }

    public BoilerplateFilter(BoilerplateRule[] rules) {
        this(rules, null);
    }

    public BoilerplateFilter(BoilerplateRule[] rules, java.util.function.Consumer<String> onRuleMatched) {
        this.rules = rules.clone();
        java.util.Arrays.sort(this.rules);
        this.onRuleMatched = onRuleMatched;
    }

    public boolean isBoilerplate(String line) {
        return matchedRule(line) != null;
    }

    public BoilerplateRule matchedRule(String line) {
        if (line == null) return null;
        int len = line.length();
        for (BoilerplateRule rule : rules) {
            if (len < rule.minLineLength()) continue;
            if (rule.pattern().matcher(line).find()) {
                if (log.isTraceEnabled()) {
                    log.trace("Boilerplate matched rule={} line='{}'", rule.name(), line);
                }
                if (onRuleMatched != null) {
                    onRuleMatched.accept(rule.name());
                }
                return rule;
            }
        }
        return null;
    }

    public String stripFromText(String plainText, java.util.function.Predicate<String> isProperName) {
        if (plainText == null || plainText.isBlank()) return "";
        String fixed = com.example.EVIM.domain.service.TextNormalizer.fixDoubleEncodedUtf8(plainText);
        String[] lines = NEWLINE_CRLF.split(fixed);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (sb.length() > 0 && !sb.toString().endsWith("\n\n")) {
                    sb.append("\n");
                }
                continue;
            }
            if (isBoilerplate(trimmed)) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(trimmed);
        }
        String result = sb.toString().trim();
        String[] resultLines = NEWLINE.split(result);
        int end = resultLines.length;
        while (end > 1) {
            String last = resultLines[end - 1].trim();
            if (last.isEmpty() || isProperName.test(last)
                    || SHORT_CLOSING.matcher(last).matches()) {
                end--;
            } else {
                break;
            }
        }
        if (end < resultLines.length) {
            StringBuilder trimmed = new StringBuilder();
            for (int i = 0; i < end; i++) {
                if (i > 0) trimmed.append("\n");
                trimmed.append(resultLines[i]);
            }
            return trimmed.toString().trim();
        }
        return result;
    }
}
