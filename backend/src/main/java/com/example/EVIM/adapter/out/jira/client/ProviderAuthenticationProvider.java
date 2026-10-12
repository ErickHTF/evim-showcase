package com.example.EVIM.adapter.out.jira.client;

import com.example.EVIM.application.auth.CredentialsContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class ProviderAuthenticationProvider {

    @Value("${jira.username:}")
    private String jiraUsername;

    @Value("${jira.api-token:}")
    private String jiraApiToken;

    public HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        CredentialsContext.JiraCredentials cred = CredentialsContext.get();
        if (cred != null) {
            headers.set("Authorization", cred.authHeader());
        } else {
            String base64Auth = Base64.getEncoder()
                    .encodeToString((jiraUsername + ":" + jiraApiToken).getBytes());
            headers.set("Authorization", "Basic " + base64Auth);
        }
        headers.set("Accept", "application/json");
        return headers;
    }
}
