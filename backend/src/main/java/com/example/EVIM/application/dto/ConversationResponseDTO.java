package com.example.EVIM.application.dto;

import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.Priority;
import com.example.EVIM.domain.model.Status;

import java.time.LocalDateTime;

public record ConversationResponseDTO (
        Long id,
        Integer ticketId,
        String subject,
        Status status,
        Priority priority,
        LocalDateTime lastUpdate,
        String jiraId
){
    public static ConversationResponseDTO from(Conversation conversation) {
        return new ConversationResponseDTO(
                conversation.getId(),
                conversation.getTicketId(),
                conversation.getSubject(),
                conversation.getStatus(),
                conversation.getPriority(),
                conversation.getLastUpdate(),
                conversation.getExternalBoardId()
        );
    }
}
