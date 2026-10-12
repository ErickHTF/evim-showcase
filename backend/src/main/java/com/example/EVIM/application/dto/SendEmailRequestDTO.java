package com.example.EVIM.application.dto;

public record SendEmailRequestDTO(
        String ticketId,
        String to,
        String subject,
        String body,
        boolean replicateToJira
) {}
