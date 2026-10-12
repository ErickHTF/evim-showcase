package com.example.EVIM.adapter.in.web;

import com.example.EVIM.adapter.out.persistence.AtlassianTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth/atlassian")
public class AtlassianOAuthController {

    private static final String AUTH_URL      = "https://auth.atlassian.com/authorize";
    private static final String TOKEN_URL     = "https://auth.atlassian.com/oauth/token";
    private static final String RESOURCES_URL = "https://api.atlassian.com/oauth/token/accessible-resources";
    private static final String JIRA_API_BASE = "https://api.atlassian.com/ex/jira/";
    private static final String SCOPE = "read:jira-work write:jira-work read:jira-user read:board-scope:jira-software read:sprint:jira-software offline_access";

    @Value("${atlassian.client-id}")
    private String clientId;

    @Value("${atlassian.client-secret}")
    private String clientSecret;

    @Value("${atlassian.redirect-uri}")
    private String redirectUri;

    @Value("${atlassian.frontend-url}")
    private String frontendUrl;

    private final AtlassianTokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();


    private final ConcurrentHashMap<String, StateEntry> pendingStates = new ConcurrentHashMap<>();

    private record StateEntry(String sub, Instant expiresAt) {}

    public AtlassianOAuthController(AtlassianTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiate(@AuthenticationPrincipal Jwt jwt) {
        String sub = jwt.getSubject();
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, new StateEntry(sub, Instant.now().plusSeconds(300)));

        String redirectUrl = AUTH_URL
                + "?audience=api.atlassian.com"
                + "&client_id=" + clientId
                + "&scope=" + SCOPE.replace(" ", "%20")
                + "&redirect_uri=" + redirectUri
                + "&state=" + state
                + "&response_type=code"
                + "&prompt=consent";

        return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        StateEntry entry = pendingStates.remove(state);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State inválido ou expirado");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
        );

        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        try {
            JsonNode tokenNode = objectMapper.readTree(tokenResponse.getBody());
            String accessToken  = tokenNode.path("access_token").asText();
            String refreshToken = tokenNode.path("refresh_token").asText();
            int expiresIn       = tokenNode.path("expires_in").asInt(3600);
            Instant expiresAt   = Instant.now().plusSeconds(expiresIn);

            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.set("Authorization", "Bearer " + accessToken);
            authHeaders.set("Accept", "application/json");

            ResponseEntity<String> resourcesResponse = restTemplate.exchange(
                    RESOURCES_URL, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

            JsonNode resources = objectMapper.readTree(resourcesResponse.getBody());
            if (!resources.isArray() || resources.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma instância Jira encontrada");
            }
            String cloudId = resources.get(0).path("id").asText();

            tokenService.saveOrUpdate(entry.sub(), accessToken, refreshToken, cloudId, expiresAt);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar callback Atlassian", e);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "?atlassian=linked"))
                .build();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal Jwt jwt) {
        return tokenService.findBySub(jwt.getSubject())
                .map(t -> {
                    HttpHeaders authHeaders = new HttpHeaders();
                    authHeaders.set("Authorization", "Bearer " + t.getAccessToken());
                    authHeaders.set("Accept", "application/json");

                    try {
                        String myselfUrl = JIRA_API_BASE + t.getCloudId() + "/rest/api/3/myself";
                        ResponseEntity<String> meResponse = restTemplate.exchange(
                                myselfUrl, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
                        JsonNode me = objectMapper.readTree(meResponse.getBody());
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "linked",       true,
                                "displayName",  me.path("displayName").asText(""),
                                "avatarUrl",    me.path("avatarUrls").path("48x48").asText(""),
                                "email",        me.path("emailAddress").asText(""),
                                "accountId",    me.path("accountId").asText("")
                        ));
                    } catch (Exception e) {
                        return ResponseEntity.ok(Map.<String, Object>of("linked", true));
                    }
                })
                .orElse(ResponseEntity.ok(Map.of("linked", false)));
    }

    @DeleteMapping
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal Jwt jwt) {
        tokenService.deleteBySub(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
