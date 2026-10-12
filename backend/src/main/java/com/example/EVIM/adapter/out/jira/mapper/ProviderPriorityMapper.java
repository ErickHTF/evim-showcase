package com.example.EVIM.adapter.out.jira.mapper;

import com.example.EVIM.application.dto.ProviderPriorityDTO;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderPriorityMapper {

    public List<ProviderPriorityDTO> mapList(JsonNode root) {
        List<ProviderPriorityDTO> priorities = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode p : root) {
                priorities.add(new ProviderPriorityDTO(
                        p.path("id").asText(),
                        p.path("name").asText(),
                        p.path("iconUrl").asText()
                ));
            }
        }
        return priorities;
    }
}
