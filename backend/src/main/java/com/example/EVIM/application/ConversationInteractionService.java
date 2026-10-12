package com.example.EVIM.application;

import com.example.EVIM.application.dto.LastInteractionDTO;
import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import com.example.EVIM.domain.port.out.EmailRepositoryPort;
import com.example.EVIM.domain.service.SenderClassificationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationInteractionService {

    private final ConversationRepositoryPort conversationRepository;
    private final EmailRepositoryPort emailRepository;
    private final SenderClassificationService senderClassificationService;

    public ConversationInteractionService(ConversationRepositoryPort conversationRepository,
                                          EmailRepositoryPort emailRepository,
                                          SenderClassificationService senderClassificationService) {
        this.conversationRepository      = conversationRepository;
        this.emailRepository             = emailRepository;
        this.senderClassificationService = senderClassificationService;
    }

    public LastInteractionDTO getLastInteraction(Integer ticketId) {
        Conversation conversation = conversationRepository.findByTicketId(ticketId).orElse(null);
        if (conversation == null) return null;

        Email lastEmail = emailRepository
                .findTopByConversationIdOrderByReceivedDesc(conversation.getId())
                .orElse(null);
        if (lastEmail == null) return null;

        boolean isExternalSender = !senderClassificationService.isInternalSender(lastEmail.getSender());
        return new LastInteractionDTO(lastEmail.getSender(), lastEmail.getReceived(), isExternalSender);
    }
}