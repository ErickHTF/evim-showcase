package com.example.EVIM.application;

import com.example.EVIM.application.dto.PagedResult;
import com.example.EVIM.application.dto.PatchCardDTO;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.application.dto.ProviderPriorityDTO;
import com.example.EVIM.application.dto.ProviderTransitionDTO;
import com.example.EVIM.application.dto.ProviderUserDTO;
import com.example.EVIM.application.dto.SprintDTO;
import com.example.EVIM.application.dto.FilterCountsDTO;
import com.example.EVIM.domain.port.out.ProviderCommentPort;
import com.example.EVIM.domain.port.out.ProviderIssuePort;
import com.example.EVIM.domain.port.out.ProviderIssueWritePort;
import com.example.EVIM.domain.port.out.ProviderTransitionPort;
import com.example.EVIM.domain.port.out.ProviderUserPort;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProviderService {

    private final ProviderIssuePort issuePort;
    private final ProviderIssueWritePort issueWritePort;
    private final ProviderUserPort userPort;
    private final ProviderCommentPort commentPort;
    private final ProviderTransitionPort transitionPort;
    private final PendingInteractionService pendingInteractionService;
    private final SlaMetricsService slaMetricsService;

    public ProviderService(
            ProviderIssuePort issuePort,
            ProviderIssueWritePort issueWritePort,
            ProviderUserPort userPort,
            ProviderCommentPort commentPort,
            ProviderTransitionPort transitionPort,
            PendingInteractionService pendingInteractionService,
            SlaMetricsService slaMetricsService
    ) {
        this.issuePort                 = issuePort;
        this.issueWritePort            = issueWritePort;
        this.userPort                  = userPort;
        this.commentPort               = commentPort;
        this.transitionPort            = transitionPort;
        this.pendingInteractionService = pendingInteractionService;
        this.slaMetricsService         = slaMetricsService;
    }

    public ProviderIssueDTO getCleanIssue(String issueKey) {
        return issuePort.getCleanIssue(issueKey);
    }

    public List<ProviderPriorityDTO> getPriorities() {
        return issuePort.getPriorities();
    }

    public List<ProviderTransitionDTO> getTransitions(String issueKey) {
        return transitionPort.getTransitions(issueKey);
    }

    public List<SprintDTO> getSprints() {
        return issuePort.getSprints();
    }

    public PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId,
                                                      Boolean unassigned, Long conversationId,
                                                      Long sprintId, Boolean pendingOnly,
                                                      String text, Boolean vendorOnly, String nextPageToken, int size) {
        PagedResult<ProviderIssueDTO> result =
                issuePort.searchIssues(status, accountId, unassigned, conversationId, sprintId, text, vendorOnly, nextPageToken, size);

        if (Boolean.TRUE.equals(pendingOnly)) {
            Set<Integer> pendingIds = pendingInteractionService.findPendingTicketIds();
            List<ProviderIssueDTO> filtered = result.content().stream()
                    .filter(dto -> dto.getVendorTicket() != null &&
                            pendingIds.contains(Integer.parseInt(dto.getVendorTicket())))
                    .collect(Collectors.toList());
            return new PagedResult<>(filtered, null, size);
        }
        return result;
    }

    public List<String> getProjectStatuses() {
        return issuePort.getProjectStatuses();
    }

    public List<ProviderUserDTO> getAssignableUsersDefaultProject() {
        return userPort.getAssignableUsersDefaultProject();
    }

    public List<ProviderUserDTO> getAssignableUsersByProject(String projectKey) {
        return userPort.getAssignableUsersByProject(projectKey);
    }

    public ProviderUserDTO getCurrentUser() {
        return userPort.getCurrentUser();
    }

    public JsonNode getRawIssue(String issueKey) {
        return issuePort.getRawIssue(issueKey);
    }

    public void addComment(String issueKey, String adfBody) {
        commentPort.addComment(issueKey, adfBody);
    }

    public void patchIssue(String issueKey, PatchCardDTO dto) {
        issueWritePort.patchIssue(issueKey, dto);
    }

    public ResponseEntity<byte[]> getAttachmentContent(String attachmentId) {
        return issuePort.getAttachmentContent(attachmentId);
    }

    public FilterCountsDTO getFilterCounts(String accountId, Long sprintId) {
        return issuePort.getFilterCounts(accountId, sprintId);
    }
}
