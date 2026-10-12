package com.example.EVIM.application;

import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import com.example.EVIM.domain.service.SenderClassificationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PendingInteractionService {

    private final ConversationRepositoryPort conversationRepository;
    private final SenderClassificationService senderClassificationService;

    public PendingInteractionService(ConversationRepositoryPort conversationRepository,
                                     SenderClassificationService senderClassificationService) {
        this.conversationRepository      = conversationRepository;
        this.senderClassificationService = senderClassificationService;
    }

    @Transactional
    public void updatePendingStatus(Long conversationId, String senderEmail) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        conversation.setPendingResponse(!senderClassificationService.isInternalSender(senderEmail));
        conversationRepository.save(conversation);
    }

    public Set<Integer> findPendingTicketIds() {
        return conversationRepository.findByPendingResponseTrue()
                .stream()
                .map(Conversation::getTicketId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}