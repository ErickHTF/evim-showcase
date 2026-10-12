package com.example.EVIM.application.dto;

import java.time.LocalDateTime;

public record ThreadMessageDTO(
        Long id,
        String author,
        String company,
        LocalDateTime sentAt,
        String body,
        Integer ordinal,
        boolean isVendor
) {}
