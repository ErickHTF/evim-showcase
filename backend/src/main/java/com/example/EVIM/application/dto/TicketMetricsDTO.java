package com.example.EVIM.application.dto;

import java.time.LocalDateTime;

public record TicketMetricsDTO(
        Long conversationId,
        String jiraKey,
        Integer ticketId,
        String subject,
        String currentOwner,
        long vendorTimeSeconds,
        Long firstResponseSeconds,
        int pingPongScore,
        LocalDateTime trackingSince
) {}
