package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.example.EVIM.application.dto.SprintDTO;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import com.example.EVIM.adapter.out.jira.util.JqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

@Repository
public class ProviderIssueRepository {

    private final ProviderApiClient apiClient;
    private final ProviderConfigPort configPort;
    private final JqlBuilder jqlBuilder;

    public ProviderIssueRepository(ProviderApiClient apiClient,
                                   @Lazy ProviderConfigPort configPort,
                                   JqlBuilder jqlBuilder) {
        this.apiClient  = apiClient;
        this.configPort = configPort;
        this.jqlBuilder = jqlBuilder;
    }

    public record JqlPage(List<JsonNode> issues, String nextPageToken) {}

    private String[] issueFields() {
        return new String[]{
                "summary", "status", "assignee", "priority",
                "reporter", "description", "comment", "attachment", "created", "updated",
                configPort.getVendorTicketField()
        };
    }

    public JqlPage findByJql(String jql, String nextPageToken, int maxResults) {
        Map<String, Object> body = new HashMap<>();
        body.put("jql", jql);
        body.put("maxResults", maxResults);
        body.put("fields", issueFields());
        if (nextPageToken != null) body.put("nextPageToken", nextPageToken);

        try {
            JsonNode root   = apiClient.parseResponse(apiClient.post("/rest/api/3/search/jql", body));
            JsonNode issues = root.get("issues");
            String token    = root.path("nextPageToken").isNull() ? null
                                : root.path("nextPageToken").asText(null);

            if (issues == null || !issues.isArray()) return new JqlPage(Collections.emptyList(), null);

            List<JsonNode> results = new ArrayList<>();
            issues.forEach(results::add);
            return new JqlPage(results, token);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar JQL: " + jql, e);
        }
    }

    public JqlPage search(String status, String accountId, Boolean unassigned,
                          Long conversationId, Long sprintId, String nextPageToken, int size) {
        return findByJql(jqlBuilder.build(status, accountId, unassigned, conversationId, sprintId), nextPageToken, size);
    }

    public JqlPage search(String status, String accountId, Boolean unassigned,
                          Long conversationId, Long sprintId, String text, String nextPageToken, int size) {
        return findByJql(jqlBuilder.build(status, accountId, unassigned, conversationId, sprintId, text), nextPageToken, size);
    }

    public JqlPage search(String status, String accountId, Boolean unassigned,
                          Long conversationId, Long sprintId, String text, Boolean vendorOnly, String nextPageToken, int size) {
        return findByJql(jqlBuilder.build(status, accountId, unassigned, conversationId, sprintId, text, vendorOnly), nextPageToken, size);
    }

    public long countByJql(String jql) {
        Map<String, Object> body = new HashMap<>();
        body.put("jql", jql);
        try {
            JsonNode root = apiClient.parseResponse(
                    apiClient.post("/rest/api/3/search/approximate-count", body));
            return root.path("count").asLong(0L);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar JQL: " + jql, e);
        }
    }

    public List<SprintDTO> getSprints() {
        if (!configPort.isSprintsEnabled()) return Collections.emptyList();
        long boardId = configPort.getBoardId();
        if (boardId == 0) return Collections.emptyList();
        try {
            JsonNode root = apiClient.parseResponse(
                    apiClient.get("/rest/agile/1.0/board/" + boardId + "/sprint?maxResults=50"));
            List<SprintDTO> result = new ArrayList<>();
            JsonNode values = root.path("values");
            if (values.isArray()) {
                values.forEach(s -> result.add(new SprintDTO(
                        s.path("id").asLong(),
                        s.path("name").asText(),
                        s.path("state").asText()
                )));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
