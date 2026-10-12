package com.example.EVIM.domain.port.out;

import com.example.EVIM.application.dto.PatchCardDTO;

public interface ProviderIssueWritePort {
    void patchIssue(String issueKey, PatchCardDTO dto);
}
