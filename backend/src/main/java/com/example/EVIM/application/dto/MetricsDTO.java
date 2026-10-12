package com.example.EVIM.application.dto;

import java.util.List;

public record MetricsDTO(
        MetricsSummaryDTO summary,
        List<TicketMetricsDTO> tickets
) {}
