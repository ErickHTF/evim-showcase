package com.example.EVIM.application.dto;

public record AttachmentResponseDTO(
        Long id,
        String fileName,
        String contentType,
        Long emailId
) {}
