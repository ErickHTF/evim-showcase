package com.example.EVIM.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ProviderUserDTO {

    private String accountId;
    private String name;
    private String displayName;
    private Map<String, String> avatarUrls;
}
