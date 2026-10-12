package com.example.EVIM.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.EVIM.application.metrics.ParseDiagnostic;
import com.example.EVIM.domain.service.PortalEmailThreadParser;
import com.example.EVIM.domain.service.PortalEmailThreadParser.ParseResult;
import com.example.EVIM.domain.service.PortalEmailThreadParser.ParsedMessage;

@Component
public class ThreadMessageBuilder {

    public record Build(List<ParsedMessage> messages, ParseDiagnostic diagnostic) {}

    public record Source(String body, String senderAddress, String senderDisplayName,
                         LocalDateTime received) {}

    public Build assemble(Source source) {
        String domain = com.example.EVIM.application.metrics.ParserMetrics
                .extractDomain(source.senderAddress());

        if (source.body() == null) {
            return new Build(Collections.emptyList(),
                    ParseDiagnostic.failed(domain, "null-body"));
        }

        ParseResult result = PortalEmailThreadParser.parseDetailed(source.body());
        List<ParsedMessage> messages = result.messages();

        if (messages.isEmpty()) {
            String plainBody = PortalEmailThreadParser.stripBoilerplate(result.plainText());
            plainBody = com.example.EVIM.domain.parsing.SignatureStripper.strip(plainBody);
            if (plainBody == null || plainBody.isBlank()) {
                return new Build(Collections.emptyList(),
                        ParseDiagnostic.failed(domain, "blank-after-strip"));
            }




            if (com.example.EVIM.domain.parsing.SignatureStripper.isEffectivelyEmpty(plainBody)) {
                return new Build(Collections.emptyList(),
                        ParseDiagnostic.failed(domain, "phantom-empty"));
            }
            String author = (source.senderDisplayName() != null && !source.senderDisplayName().isBlank())
                    ? source.senderDisplayName() : source.senderAddress();
            ParsedMessage single = new ParsedMessage(author, null, source.received(), plainBody);
            return new Build(List.of(single),
                    ParseDiagnostic.fallback(domain, "empty-parse"));
        }

        int unparsedDates = 0;
        for (ParsedMessage m : messages) {
            if (m.date() == null) unparsedDates++;
        }
        return new Build(new ArrayList<>(messages),
                ParseDiagnostic.success(domain, messages.size(), unparsedDates));
    }
}
