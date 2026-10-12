package com.example.EVIM.adapter.in.web;

import com.example.EVIM.application.dto.PagedResult;
import com.example.EVIM.application.dto.PatchCardDTO;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.application.dto.ProviderPriorityDTO;
import com.example.EVIM.application.dto.ProviderTransitionDTO;
import com.example.EVIM.application.dto.ProviderUserDTO;
import com.example.EVIM.application.dto.SprintDTO;
import com.example.EVIM.application.dto.FilterCountsDTO;
import com.example.EVIM.application.ProviderService;
import com.example.EVIM.application.StarredTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/card")
public class TaskController {

    private final ProviderService providerService;
    private final StarredTicketService starredTicketService;

    public TaskController(ProviderService providerService, StarredTicketService starredTicketService) {
        this.providerService = providerService;
        this.starredTicketService = starredTicketService;
    }

    private String getCurrentUserId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSubject();
    }

    @GetMapping("/{issueKey}")
    public ResponseEntity<ProviderIssueDTO> getIssueCard(@PathVariable String issueKey) {
        ProviderIssueDTO cardData = providerService.getCleanIssue(issueKey);
        if (cardData != null) return ResponseEntity.ok(cardData);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/users/assignable/project/{projectKey}")
    public ResponseEntity<List<ProviderUserDTO>> getAssignableUsersByProject(@PathVariable String projectKey) {
        return ResponseEntity.ok(providerService.getAssignableUsersByProject(projectKey));
    }

    @GetMapping("/users/assignable/default")
    public ResponseEntity<List<ProviderUserDTO>> getAssignableUsersDefaultProject() {
        return ResponseEntity.ok(providerService.getAssignableUsersDefaultProject());
    }

    @PatchMapping("/{issueKey}")
    public ResponseEntity<Void> patchCard(@PathVariable String issueKey,
                                          @RequestBody PatchCardDTO dto) {
        providerService.patchIssue(issueKey, dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/comment")
    public ResponseEntity<Void> addComment(@PathVariable String issueKey, @RequestBody String adfJson) {
        providerService.addComment(issueKey, adfJson);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{issueKey}/transitions")
    public ResponseEntity<List<ProviderTransitionDTO>> getAvailableTransitions(@PathVariable String issueKey) {
        List<ProviderTransitionDTO> transitions = providerService.getTransitions(issueKey);
        if (transitions.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(transitions);
    }

    @GetMapping("/sprints")
    public ResponseEntity<List<SprintDTO>> getSprints() {
        return ResponseEntity.ok(providerService.getSprints());
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResult<ProviderIssueDTO>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false) Boolean pendingOnly,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Boolean vendorOnly,
            @RequestParam(required = false) String nextPageToken,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(providerService.searchIssues(status, accountId, unassigned, conversationId, sprintId, pendingOnly, text, vendorOnly, nextPageToken, size));
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getProjectStatuses() {
        return ResponseEntity.ok(providerService.getProjectStatuses());
    }

    @GetMapping("/counts")
    public ResponseEntity<FilterCountsDTO> getFilterCounts(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) Long sprintId) {
        return ResponseEntity.ok(providerService.getFilterCounts(accountId, sprintId));
    }

    @GetMapping("/priorities")
    public ResponseEntity<List<ProviderPriorityDTO>> getPriorities() {
        return ResponseEntity.ok(providerService.getPriorities());
    }

    @GetMapping("/{issueKey}/raw")
    public ResponseEntity<String> getRawIssue(@PathVariable String issueKey) {
        com.fasterxml.jackson.databind.JsonNode raw = providerService.getRawIssue(issueKey);
        if (raw == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(raw.toString());
    }

    @GetMapping("/attachment/{attachmentId}/content")
    public ResponseEntity<byte[]> getAttachmentContent(@PathVariable String attachmentId) {
        ResponseEntity<byte[]> jiraResponse = providerService.getAttachmentContent(attachmentId);
        return ResponseEntity.status(jiraResponse.getStatusCode())
                .headers(h -> {
                    jiraResponse.getHeaders().forEach((name, values) -> {
                        if (!name.equalsIgnoreCase("Content-Disposition")) h.addAll(name, values);
                    });
                    h.set("Content-Disposition", "inline");
                })
                .body(jiraResponse.getBody());
    }

    @GetMapping("/starred")
    public ResponseEntity<List<ProviderIssueDTO>> getStarredTickets() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(starredTicketService.getStarredTickets(userId));
    }

    @GetMapping("/starred/keys")
    public ResponseEntity<List<String>> getStarredKeys() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(starredTicketService.getStarredKeys(userId));
    }

    @PutMapping("/{issueKey}/star")
    public ResponseEntity<Void> starTicket(@PathVariable String issueKey) {
        String userId = getCurrentUserId();
        starredTicketService.star(userId, issueKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/star")
    public ResponseEntity<Void> unstarTicket(@PathVariable String issueKey) {
        String userId = getCurrentUserId();
        starredTicketService.unstar(userId, issueKey);
        return ResponseEntity.noContent().build();
    }
}
