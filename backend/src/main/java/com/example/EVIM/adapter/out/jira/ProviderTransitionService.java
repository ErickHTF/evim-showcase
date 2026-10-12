package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.application.dto.ProviderTransitionDTO;
import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.example.EVIM.domain.port.out.ProviderTransitionPort;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProviderTransitionService implements ProviderTransitionPort {

    private static final Logger log = LoggerFactory.getLogger(ProviderTransitionService.class);

    private final ProviderApiClient apiClient;

    public ProviderTransitionService(ProviderApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<ProviderTransitionDTO> getTransitions(String issueKey) {
        try {
            ResponseEntity<String> response = apiClient.get("/rest/api/3/issue/" + issueKey + "/transitions");
            JsonNode root = apiClient.parseResponse(response);
            return parseTransitions(root);
        } catch (Exception e) {
            log.error("Erro ao processar transições: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ProviderTransitionDTO> parseTransitions(JsonNode root) {
        List<ProviderTransitionDTO> transitionList = new ArrayList<>();
        JsonNode transitionsNode = root.path("transitions");
        if (transitionsNode.isArray()) {
            for (JsonNode node : transitionsNode) {
                transitionList.add(new ProviderTransitionDTO(
                        node.path("id").asText(),
                        node.path("name").asText(),
                        node.path("to").path("name").asText()
                ));
            }
        }
        return transitionList;
    }
}
