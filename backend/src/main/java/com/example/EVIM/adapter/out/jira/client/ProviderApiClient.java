package com.example.EVIM.adapter.out.jira.client;

import com.example.EVIM.application.auth.CredentialsContext;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class ProviderApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProviderAuthenticationProvider authProvider;
    private final ProviderConfigPort configPort;

    public ProviderApiClient(ProviderAuthenticationProvider authProvider,
                             @Lazy ProviderConfigPort configPort) {
        this.restTemplate  = new RestTemplate();
        this.objectMapper  = new ObjectMapper();
        this.authProvider  = authProvider;
        this.configPort = configPort;
    }

    private String baseUrl() {
        CredentialsContext.JiraCredentials cred = CredentialsContext.get();
        return cred != null ? cred.baseUrl() : configPort.getBaseUrl();
    }

    public ResponseEntity<String> get(String endpoint) {
        HttpEntity<String> request = new HttpEntity<>(authProvider.createAuthHeaders());
        return restTemplate.exchange(baseUrl() + endpoint, HttpMethod.GET, request, String.class);
    }

    public ResponseEntity<String> put(String endpoint, Map<String, Object> body) {
        HttpHeaders headers = authProvider.createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(baseUrl() + endpoint, HttpMethod.PUT, request, String.class);
    }

    public ResponseEntity<String> post(String endpoint, Object body) {
        HttpHeaders headers = authProvider.createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(baseUrl() + endpoint, HttpMethod.POST, request, String.class);
    }

    public ResponseEntity<byte[]> getBytes(String endpoint) {
        HttpEntity<String> request = new HttpEntity<>(authProvider.createAuthHeaders());
        return restTemplate.exchange(baseUrl() + endpoint, HttpMethod.GET, request, byte[].class);
    }

    public JsonNode parseResponse(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Jira response", e);
        }
    }
}