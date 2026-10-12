package com.example.EVIM.domain.service;

import com.example.EVIM.domain.port.out.ProviderConfigPort;

public class SenderClassificationService {

    private final ProviderConfigPort configPort;

    public SenderClassificationService(ProviderConfigPort configPort) {
        this.configPort = configPort;
    }

    public boolean isInternalSender(String email) {
        if (email == null) return false;
        if (!email.contains("@")) return true;
        return email.endsWith("@" + configPort.getInternalDomain());
    }
}
