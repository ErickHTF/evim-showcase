package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.example.EVIM.domain.port.out.ProviderCommentPort;

import java.util.Map;

@Service
public class ProviderCommentService implements ProviderCommentPort {

    private final ProviderApiClient apiClient;
    private final ObjectMapper objectMapper;

    public ProviderCommentService(ProviderApiClient apiClient, ObjectMapper objectMapper)
        {
            this.apiClient = apiClient;
            this.objectMapper = objectMapper;
        }

    public void addComment(String issueKey, String adfBody) {
        try {
            Object adfNode = objectMapper.readValue(adfBody, Object.class);
            apiClient.post("/rest/api/3/issue/" + issueKey + "/comment", Map.of("body", adfNode));
        } catch (Exception e) {
            throw new RuntimeException("Invalid ADF body", e);
        }
    }
}