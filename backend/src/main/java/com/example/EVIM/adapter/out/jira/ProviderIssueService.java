package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.example.EVIM.adapter.out.jira.mapper.ProviderIssueMapper;
import com.example.EVIM.adapter.out.jira.mapper.ProviderPriorityMapper;
import com.example.EVIM.adapter.out.jira.util.JqlBuilder;
import com.example.EVIM.application.dto.FilterCountsDTO;
import com.example.EVIM.application.dto.PagedResult;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.application.dto.ProviderPriorityDTO;
import com.example.EVIM.application.dto.SprintDTO;
import com.example.EVIM.domain.port.out.ProviderIssuePort;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProviderIssueService implements ProviderIssuePort {

    private static final Logger log = LoggerFactory.getLogger(ProviderIssueService.class);

    private final ProviderApiClient apiClient;
    private final ProviderIssueRepository issueRepository;
    private final ProviderStatusRepository statusRepository;
    private final ProviderIssueMapper mapper;
    private final ProviderPriorityMapper priorityMapper;
    private final JqlBuilder jqlBuilder;

    public ProviderIssueService(ProviderApiClient apiClient,
                                ProviderIssueRepository issueRepository,
                                ProviderStatusRepository statusRepository,
                                ProviderIssueMapper mapper,
                                ProviderPriorityMapper priorityMapper,
                                JqlBuilder jqlBuilder) {
        this.apiClient        = apiClient;
        this.issueRepository  = issueRepository;
        this.statusRepository = statusRepository;
        this.mapper           = mapper;
        this.priorityMapper   = priorityMapper;
        this.jqlBuilder       = jqlBuilder;
    }

    public ProviderIssueDTO getCleanIssue(String issueKey) {
        try {
            JsonNode root = apiClient.parseResponse(apiClient.get("/rest/api/3/issue/" + issueKey));
            return mapper.map(issueKey, root.get("fields"));
        } catch (Exception e) {
            log.error("Error getting issue: {}", e.getMessage());
            return null;
        }
    }

    public List<ProviderPriorityDTO> getPriorities() {
        try {
            ResponseEntity<String> response = apiClient.get("/rest/api/3/priority");
            JsonNode root = apiClient.parseResponse(response);
            return priorityMapper.mapList(root);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar prioridades", e);
        }
    }

    @Override
    public PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId,
                                                      Boolean unassigned, Long conversationId,
                                                      Long sprintId, String nextPageToken, int size) {
        ProviderIssueRepository.JqlPage jqlPage =
                issueRepository.search(status, accountId, unassigned, conversationId, sprintId, nextPageToken, size);

        List<ProviderIssueDTO> content = jqlPage.issues().stream()
                .map(issue -> mapper.map(issue.path("key").asText(), issue.path("fields")))
                .collect(Collectors.toList());

        return new PagedResult<>(content, jqlPage.nextPageToken(), size);
    }

    @Override
    public PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId,
                                                      Boolean unassigned, Long conversationId,
                                                      Long sprintId, String text, String nextPageToken, int size) {
        ProviderIssueRepository.JqlPage jqlPage =
                issueRepository.search(status, accountId, unassigned, conversationId, sprintId, text, nextPageToken, size);

        List<ProviderIssueDTO> content = jqlPage.issues().stream()
                .map(issue -> mapper.map(issue.path("key").asText(), issue.path("fields")))
                .collect(Collectors.toList());

        return new PagedResult<>(content, jqlPage.nextPageToken(), size);
    }

    @Override
    public PagedResult<ProviderIssueDTO> searchIssues(String status, String accountId,
                                                      Boolean unassigned, Long conversationId,
                                                      Long sprintId, String text, Boolean vendorOnly, String nextPageToken, int size) {
        ProviderIssueRepository.JqlPage jqlPage =
                issueRepository.search(status, accountId, unassigned, conversationId, sprintId, text, vendorOnly, nextPageToken, size);

        List<ProviderIssueDTO> content = jqlPage.issues().stream()
                .map(issue -> mapper.map(issue.path("key").asText(), issue.path("fields")))
                .collect(Collectors.toList());

        return new PagedResult<>(content, jqlPage.nextPageToken(), size);
    }

    @Override
    public List<SprintDTO> getSprints() {
        return issueRepository.getSprints();
    }

    public List<String> getProjectStatuses() {
        return statusRepository.findProjectStatuses();
    }

    public JsonNode getRawIssue(String issueKey) {
        try {
            return apiClient.parseResponse(apiClient.get("/rest/api/3/issue/" + issueKey));
        } catch (Exception e) {
            log.error("Error getting raw issue: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ResponseEntity<byte[]> getAttachmentContent(String attachmentId) {
        try {
            return apiClient.getBytes("/rest/api/3/attachment/content/" + attachmentId + "?redirect=false");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public FilterCountsDTO getFilterCounts(String accountId, Long sprintId) {
        long all = safeCount(jqlBuilder.buildForCount(null, null, sprintId, true));

        Long mine = null;
        if (accountId != null && !accountId.isBlank()) {
            mine = safeCount(jqlBuilder.buildForCount(accountId, null, sprintId, true));
        }

        long unassigned = safeCount(jqlBuilder.buildForCount(null, true, sprintId, true));

        long noVendor = safeCount(jqlBuilder.buildForCount(null, null, sprintId, false));

        return new FilterCountsDTO(all, mine, unassigned, noVendor);
    }

    private long safeCount(String jql) {
        try {
            return issueRepository.countByJql(jql);
        } catch (Exception e) {
            log.warn("Filter count failed for jql='{}': {}", jql, e.getMessage());
            return 0L;
        }
    }
}
