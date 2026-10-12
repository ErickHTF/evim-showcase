package com.example.EVIM.adapter.out.jira;

import com.example.EVIM.application.dto.ProviderUserDTO;
import com.example.EVIM.adapter.out.jira.client.ProviderApiClient;
import com.example.EVIM.adapter.out.jira.mapper.ProviderUserMapper;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.example.EVIM.domain.port.out.ProviderUserPort;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

@Service
public class ProviderUserService implements ProviderUserPort {

    private static final Logger log = LoggerFactory.getLogger(ProviderUserService.class);

    private final ProviderApiClient apiClient;
    private final ProviderConfigPort configPort;

    public ProviderUserService(ProviderApiClient apiClient, @Lazy ProviderConfigPort configPort) {
        this.apiClient = apiClient;
        this.configPort = configPort;
    }

    public List<ProviderUserDTO> getAssignableUsersByProject(String projectKey) {
        try {
            String endpoint = "/rest/api/3/user/assignable/search?project=" + projectKey;
            ResponseEntity<String> response = apiClient.get(endpoint);
            JsonNode usersArray = apiClient.parseResponse(response);
            List<ProviderUserDTO> users = ProviderUserMapper.mapUsersArray(usersArray);
            return users;
        } catch (Exception e) {
            log.error("Error finding users by project: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ProviderUserDTO getCurrentUser() {
        try {
            ResponseEntity<String> response = apiClient.get("/rest/api/3/myself");
            JsonNode userNode = apiClient.parseResponse(response);
            return ProviderUserMapper.mapSingleUser(userNode);
        } catch (Exception e) {
            log.error("Erro ao buscar usuário atual: {}", e.getMessage());
            return null;
        }
    }

    public List<ProviderUserDTO> getAssignableUsersDefaultProject() {
        return getAssignableUsersByProject(configPort.getProjectKey());
    }
}