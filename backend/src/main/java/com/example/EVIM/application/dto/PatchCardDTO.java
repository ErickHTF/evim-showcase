package com.example.EVIM.application.dto;

public record PatchCardDTO(
        String summary,
        String vendorTicket,
        String priorityId,
        String accountId,
        String transitionId
) {}
