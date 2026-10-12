package com.example.EVIM.application.dto;

import com.example.EVIM.domain.model.Status;

import java.time.LocalDateTime;
import java.util.List;

public record EmailResponseDTO(
        Long id,
        String sender,
        String senderName,
        String receiver,
        String subject,
        String body,
        LocalDateTime received,
        Status status,
        boolean isVendor,
        List<AttachmentResponseDTO> attachments
) {}
