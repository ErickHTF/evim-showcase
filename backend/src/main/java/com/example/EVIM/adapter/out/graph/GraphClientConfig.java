package com.example.EVIM.adapter.out.graph;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "graph.mock.enabled", havingValue = "false", matchIfMissing = true)
public class GraphClientConfig {

    @Value("${graph.tenant-id}")
    private String tenantId;

    @Value("${graph.client-id}")
    private String clientId;

    @Value("${graph.client-secret}")
    private String clientSecret;

    @Bean
    public ClientSecretCredential azureCredential() {
        return new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    @Bean
    public GraphServiceClient graphServiceClient(ClientSecretCredential credential) {
        return new GraphServiceClient(credential, new String[]{"https://graph.microsoft.com/.default"});
    }
}
