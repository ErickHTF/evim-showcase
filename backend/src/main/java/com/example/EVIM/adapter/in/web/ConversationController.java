package com.example.EVIM.adapter.in.web;

import com.example.EVIM.application.dto.ConversationResponseDTO;
import com.example.EVIM.application.dto.LastInteractionDTO;
import com.example.EVIM.domain.model.Status;
import com.example.EVIM.application.ConversationInteractionService;
import com.example.EVIM.application.ConversationProviderQueryService;
import com.example.EVIM.application.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationProviderQueryService providerQueryService;
    private final ConversationInteractionService interactionService;

    public ConversationController(ConversationService conversationService,
                                  ConversationProviderQueryService providerQueryService,
                                  ConversationInteractionService interactionService) {
        this.conversationService  = conversationService;
        this.providerQueryService = providerQueryService;
        this.interactionService   = interactionService;
    }

    @GetMapping
    public ResponseEntity<Page<ConversationResponseDTO>> listConversations(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Long ticketId,
            @PageableDefault(size = 10, sort = "lastUpdate",
                    direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(conversationService.filterConversations(status, ticketId, pageable));
    }

    @PatchMapping("/{id}/link-jira")
    public ResponseEntity<Void> linkJira(@PathVariable Long id, @RequestBody String jiraId) {
        conversationService.updateExternalBoardId(id, jiraId.replace("\"", "").trim());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-provider-assignee/{accountId}")
    public ResponseEntity<Page<ConversationResponseDTO>> getConversationsByJiraAssignee(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(providerQueryService.findByProviderAssignee(accountId, page, size));
    }

    @GetMapping("/by-provider-unassigned")
    public ResponseEntity<Page<ConversationResponseDTO>> getConversationsByJiraUnassigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(providerQueryService.findByProviderUnassigned(page, size));
    }

    @GetMapping("/ticket/{ticketId}/last-interaction")
    public ResponseEntity<LastInteractionDTO> getLastInteraction(@PathVariable Integer ticketId) {
        LastInteractionDTO dto = interactionService.getLastInteraction(ticketId);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }
}