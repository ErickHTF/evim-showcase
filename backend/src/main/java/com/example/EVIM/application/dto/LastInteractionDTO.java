package com.example.EVIM.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LastInteractionDTO {
    private String sender;
    private LocalDateTime received;
    private boolean isVendor;
}