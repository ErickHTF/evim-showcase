package com.example.EVIM.application;

import com.example.EVIM.application.dto.ConversationResponseDTO;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationProviderQueryService {

    private final ProviderService providerService;
    private final ConversationRepositoryPort conversationRepository;

    public ConversationProviderQueryService(ProviderService providerService,
                                            ConversationRepositoryPort conversationRepository) {
        this.providerService = providerService;
        this.conversationRepository = conversationRepository;
    }

    public Page<ConversationResponseDTO> findByProviderAssignee(String accountId, int page, int size) {
        List<ProviderIssueDTO> tickets = providerService.searchIssues(null, accountId, false, null, null, null, null, null, null, 200).content();
        return findConversationsByTicketIds(tickets, page, size);
    }

    public Page<ConversationResponseDTO> findByProviderUnassigned(int page, int size) {
        List<ProviderIssueDTO> tickets = providerService.searchIssues(null, null, true, null, null, null, null, null, null, 200).content();
        return findConversationsByTicketIds(tickets, page, size);
    }

    private Page<ConversationResponseDTO> findConversationsByTicketIds(List<ProviderIssueDTO> tickets, int page, int size) {
        List<Integer> ticketIds = extractTicketIds(tickets);
        if (ticketIds.isEmpty()) return Page.empty();
        Pageable pageable = PageRequest.of(page, size);
        return conversationRepository.findByTicketIdIn(ticketIds, pageable).map(ConversationResponseDTO::from);
    }

    private List<Integer> extractTicketIds(List<ProviderIssueDTO> tickets) {
        return tickets.stream()
                .filter(t -> t.getVendorTicket() != null && !t.getVendorTicket().isBlank())
                .map(t -> {
                    try {
                        return Integer.parseInt(t.getVendorTicket());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }
}