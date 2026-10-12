package com.example.EVIM.domain.port.out;

import com.example.EVIM.application.dto.ProviderUserDTO;

import java.util.List;

public interface ProviderUserPort {
    List<ProviderUserDTO> getAssignableUsersByProject(String projectKey);
    List<ProviderUserDTO> getAssignableUsersDefaultProject();
    ProviderUserDTO getCurrentUser();
}
