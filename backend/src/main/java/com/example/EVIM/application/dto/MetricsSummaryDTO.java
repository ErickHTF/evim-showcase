package com.example.EVIM.application.dto;

public record MetricsSummaryDTO(
        int totalTracked,
        int waitingForVendor,
        int waitingForInternal,
        long avgVendorTimeSeconds,
        long avgFirstResponseSeconds
) {}
