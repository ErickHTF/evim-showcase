package com.example.EVIM.domain.port.out;

import com.example.EVIM.application.dto.PagedResult;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.application.dto.ProviderPriorityDTO;
import com.example.EVIM.application.dto.SprintDTO;
import com.example.EVIM.application.dto.FilterCountsDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProviderIssuePort {
    ProviderIssueDTO getCleanIssue(String issueKey);
    PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId, Boolean unassigned, Long conversationId, Long sprintId, String nextPageToken, int size);
    PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId, Boolean unassigned, Long conversationId, Long sprintId, String text, String nextPageToken, int size);
    PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId, Boolean unassigned, Long conversationId, Long sprintId, String text, Boolean vendorOnly, String nextPageToken, int size);
    List<SprintDTO> getSprints();
    List<ProviderPriorityDTO> getPriorities();
    List<String> getProjectStatuses();
    JsonNode getRawIssue(String issueKey);
    ResponseEntity<byte[]> getAttachmentContent(String attachmentId);

    FilterCountsDTO getFilterCounts(String accountId, Long sprintId);
}
