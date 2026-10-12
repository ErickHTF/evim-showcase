package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.AtlassianUserToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class AtlassianTokenService {

    private static final String TOKEN_URL = "https://auth.atlassian.com/oauth/token";

    private final AtlassianUserTokenRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${atlassian.client-id}")
    private String clientId;

    @Value("${atlassian.client-secret}")
    private String clientSecret;

    public AtlassianTokenService(AtlassianUserTokenRepository repository) {
        this.repository = repository;
    }

    public Optional<AtlassianUserToken> findBySub(String sub) {
        return repository.findById(sub);
    }

    public AtlassianUserToken saveOrUpdate(String sub, String accessToken, String refreshToken,
                                           String cloudId, Instant expiresAt) {
        AtlassianUserToken token = repository.findById(sub)
                .orElse(new AtlassianUserToken());
        token.setKeycloakSub(sub);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setCloudId(cloudId);
        token.setExpiresAt(expiresAt);
        return repository.save(token);
    }

    public void deleteBySub(String sub) {
        repository.deleteById(sub);
    }

    public AtlassianUserToken refresh(AtlassianUserToken token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "client_secret", clientSecret,
                "refresh_token", token.getRefreshToken()
        );

        ResponseEntity<String> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            String newAccessToken = node.path("access_token").asText();
            String newRefreshToken = node.has("refresh_token")
                    ? node.path("refresh_token").asText()
                    : token.getRefreshToken();
            int expiresIn = node.path("expires_in").asInt(3600);
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);

            return saveOrUpdate(token.getKeycloakSub(), newAccessToken, newRefreshToken,
                    token.getCloudId(), expiresAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh Atlassian token", e);
        }
    }
}
