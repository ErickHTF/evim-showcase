package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.example.EVIM.application.dto.PatchCardDTO;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import com.example.EVIM.domain.port.out.ProviderIssueWritePort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProviderIssueUpdateService implements ProviderIssueWritePort {

    private final ProviderApiClient apiClient;
    private final ProviderConfigPort configPort;

    public ProviderIssueUpdateService(ProviderApiClient apiClient, ProviderConfigPort configPort) {
        this.apiClient  = apiClient;
        this.configPort = configPort;
    }

    @Override
    public void patchIssue(String issueKey, PatchCardDTO dto) {
        Map<String, Object> fields = new HashMap<>();
        if (dto.summary()      != null) fields.put("summary", dto.summary());
        if (dto.vendorTicket() != null) fields.put(configPort.getVendorTicketField(), dto.vendorTicket());
        if (dto.priorityId()   != null) fields.put("priority", Map.of("id", dto.priorityId()));
        if (dto.accountId()    != null) {
            fields.put("assignee", dto.accountId().isBlank()
                    ? null
                    : Map.of("accountId", dto.accountId()));
        }

        if (!fields.isEmpty()) {
            apiClient.put("/rest/api/3/issue/" + issueKey, Map.of("fields", fields));
        }

        if (dto.transitionId() != null && !dto.transitionId().isBlank()) {
            apiClient.post("/rest/api/3/issue/" + issueKey + "/transitions",
                    Map.of("transition", Map.of("id", dto.transitionId())));
        }
    }
}
