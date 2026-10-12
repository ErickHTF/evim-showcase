package com.example.EVIM.application.config;

import com.example.EVIM.domain.port.out.ProviderConfigPort;
import com.example.EVIM.domain.service.SenderClassificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public SenderClassificationService senderClassificationService(ProviderConfigPort configPort) {
        return new SenderClassificationService(configPort);
    }
}
