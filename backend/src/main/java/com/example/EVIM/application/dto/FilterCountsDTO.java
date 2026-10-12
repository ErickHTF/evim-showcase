package com.example.EVIM.application.dto;

public record FilterCountsDTO(
        long all,
        Long mine,
        long unassigned,
        long noVendor
) {}
