package com.example.EVIM.adapter.out.jira.mapper;

import com.example.EVIM.application.dto.ProviderUserDTO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class ProviderUserMapper {

    public static List<ProviderUserDTO> mapUsersArray(JsonNode usersJsonArray) {
        List<ProviderUserDTO> users = new ArrayList<>();

        if (usersJsonArray == null || !usersJsonArray.isArray()) {
            return users;
        }

        for (JsonNode userNode : usersJsonArray) {
            users.add(mapSingleUser(userNode));
        }

        return users;
    }

    public static ProviderUserDTO mapSingleUser(JsonNode userNode) {
        ProviderUserDTO dto = new ProviderUserDTO();

        if (userNode == null || userNode.isNull()) {
            return dto;
        }

        dto.setAccountId(extractTextSafe(userNode, "accountId"));
        dto.setDisplayName(extractTextSafe(userNode, "displayName"));
        dto.setName(extractTextSafe(userNode, "name"));

        Map<String, String> avatarUrls = extractAvatarUrls(userNode);
        if (!avatarUrls.isEmpty()) {
            dto.setAvatarUrls(avatarUrls);
        }

        return dto;
    }

    public static Map<String, String> extractAvatarUrls(JsonNode userNode) {
        Map<String, String> avatarUrls = new HashMap<>();

        if (userNode == null || !userNode.has("avatarUrls")) {
            return avatarUrls;
        }

        JsonNode avatarUrlsNode = userNode.get("avatarUrls");

        if (avatarUrlsNode != null && !avatarUrlsNode.isNull() && avatarUrlsNode.isObject()) {
            Iterator<String> fieldNames = avatarUrlsNode.fieldNames();
            while (fieldNames.hasNext()) {
                String size = fieldNames.next();
                JsonNode urlNode = avatarUrlsNode.get(size);
                if (urlNode != null) {
                    avatarUrls.put(size, urlNode.asText());
                }
            }
        }

        return avatarUrls;
    }

    private static String extractTextSafe(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) return null;
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) return null;
        return fieldNode.asText();
    }
}
