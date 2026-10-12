package com.example.EVIM.domain.port.out;

import com.example.EVIM.application.dto.ProviderTransitionDTO;

import java.util.List;

public interface ProviderTransitionPort {
    List<ProviderTransitionDTO> getTransitions(String issueKey);
}
