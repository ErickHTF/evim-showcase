package com.example.EVIM.application;

import com.example.EVIM.application.dto.ConversationResponseDTO;
import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.Status;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepositoryPort conversationRepository;
    private final SlaMetricsService slaMetricsService;

    public ConversationService(ConversationRepositoryPort conversationRepository,
                               SlaMetricsService slaMetricsService) {
        this.conversationRepository = conversationRepository;
        this.slaMetricsService      = slaMetricsService;
    }

    public Page<ConversationResponseDTO> listAllConversations(Pageable pageable) {
        return conversationRepository.findAllByOrderByLastUpdateDesc(pageable)
                .map(ConversationResponseDTO::from);
    }

    public Page<ConversationResponseDTO> filterConversations(Status status, Long ticketId, Pageable pageable) {
        return conversationRepository.findByFilter(status, ticketId, pageable)
                .map(ConversationResponseDTO::from);
    }

    @Transactional
    public void updateExternalBoardId(Long id, String externalBoardId) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + id));
        conversation.setExternalBoardId(externalBoardId);
        conversationRepository.save(conversation);
        boolean pendingVendor = Boolean.TRUE.equals(conversation.getPendingResponse());
        slaMetricsService.onJiraLinked(conversation.getId(), pendingVendor, java.time.LocalDateTime.now());
    }
}
