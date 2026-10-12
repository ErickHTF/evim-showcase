package com.example.EVIM.domain.port.out;

public interface ProviderConfigPort {
    String getInternalDomain();
    String getBaseUrl();
    String getProjectKey();
    String getVendorTicketField();
    long getBoardId();
    boolean isSprintsEnabled();
}
