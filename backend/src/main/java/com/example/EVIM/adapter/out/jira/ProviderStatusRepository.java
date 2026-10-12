package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProviderStatusRepository {

    private final ProviderApiClient apiClient;
    private final ProviderConfigPort configPort;
    private final Map<String, String> statusCache = new ConcurrentHashMap<>();

    public ProviderStatusRepository(ProviderApiClient apiClient,
                                    @Lazy ProviderConfigPort configPort) {
        this.apiClient     = apiClient;
        this.configPort = configPort;
    }

    public Map<String, String> getStatusMap() {
        if (!statusCache.isEmpty()) return statusCache;

        try {
            JsonNode root = apiClient.parseResponse(
                    apiClient.get("/rest/api/3/project/" + configPort.getProjectKey() + "/statuses")
            );

            if (root.isArray()) {
                for (JsonNode issueType : root) {
                    for (JsonNode status : issueType.path("statuses")) {
                        String name = status.path("name").asText();
                        String id   = status.path("id").asText();
                        if (!name.isEmpty())
                            statusCache.putIfAbsent(name, id);
                    }
                }
            }
            return statusCache;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar status do projeto", e);
        }
    }

    public List<String> findProjectStatuses() {
        return new ArrayList<>(getStatusMap().keySet());
    }

    public void invalidateCache() {
        statusCache.clear();
    }
}