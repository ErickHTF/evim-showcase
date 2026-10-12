package com.example.EVIM.application.metrics;

public record ParseDiagnostic(
        Outcome outcome,
        String senderDomain,
        int messageCount,
        int unparsedDateCount,
        String fallbackReason
) {
    public enum Outcome { SUCCESS, FALLBACK, FAILED }

    public static ParseDiagnostic success(String domain, int messageCount, int unparsedDates) {
        return new ParseDiagnostic(Outcome.SUCCESS, domain, messageCount, unparsedDates, null);
    }

    public static ParseDiagnostic fallback(String domain, String reason) {
        return new ParseDiagnostic(Outcome.FALLBACK, domain, 1, 0, reason);
    }

    public static ParseDiagnostic failed(String domain, String reason) {
        return new ParseDiagnostic(Outcome.FAILED, domain, 0, 0, reason);
    }
}
