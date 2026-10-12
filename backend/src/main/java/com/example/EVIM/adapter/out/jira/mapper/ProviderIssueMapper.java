package com.example.EVIM.adapter.out.jira.mapper;

import com.example.EVIM.application.dto.ProviderAttachmentDTO;
import com.example.EVIM.application.dto.ProviderCommentDTO;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import com.example.EVIM.adapter.out.jira.util.AdfParser;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderIssueMapper {

    private final ProviderConfigPort configPort;

    public ProviderIssueMapper(ProviderConfigPort configPort) {
        this.configPort = configPort;
    }

    public ProviderIssueDTO map(String issueKey, JsonNode fields) {
        ProviderIssueDTO dto = new ProviderIssueDTO();
        dto.setKey(issueKey);
        extractSummary(dto, fields);
        Map<String, String> mediaIdToAttachmentId = buildMediaIdMap(fields);
        extractDescription(dto, fields, mediaIdToAttachmentId);
        extractStatus(dto, fields);
        extractAssignee(dto, fields);
        extractPriority(dto, fields);
        extractReporter(dto, fields);
        extractComments(dto, fields, mediaIdToAttachmentId);
        extractVendorTicket(dto, fields);
        extractAttachments(dto, fields);
        return dto;
    }

    private void extractSummary(ProviderIssueDTO dto, JsonNode fields) {
        String summary = fields.get("summary").asText();
        dto.setSummary(summary.isEmpty() ? "No Title" : summary);
    }

    private void extractDescription(ProviderIssueDTO dto, JsonNode fields, Map<String, String> mediaIdMap) {
        JsonNode node = fields.get("description");
        if (node != null && !node.isNull()) {
            dto.setDescription(node.isObject() ? AdfParser.adfToPlainText(node) : node.asText());
            if (node.isObject()) {
                List<String> ids = new ArrayList<>();
                for (String filename : AdfParser.extractMediaFilenames(node)) {
                    String id = mediaIdMap.get(filename);
                    if (id != null) ids.add(id);
                }
                dto.setDescriptionAttachmentIds(ids);
            }
        } else {
            dto.setDescription("No description provided.");
        }
    }

    private void extractStatus(ProviderIssueDTO dto, JsonNode fields) {
        String status = fields.get("status").get("name").asText();
        dto.setStatus(status.isEmpty() ? "Unknown" : status);
    }

    private void extractAssignee(ProviderIssueDTO dto, JsonNode fields) {
        JsonNode node = fields.get("assignee");
        if (node != null && !node.isNull()) {
            dto.setAssignee(node.get("displayName").asText());
            dto.setAssigneeAccountId(node.get("accountId").asText());
            dto.setAssigneeAvatarUrl(node.path("avatarUrls").path("48x48").asText());
        } else {
            dto.setAssignee("Unassigned");
            dto.setAssigneeAccountId(null);
            dto.setAssigneeAvatarUrl(null);
        }
    }

    private void extractPriority(ProviderIssueDTO dto, JsonNode fields) {
        JsonNode node = fields.path("priority");
        if (!node.isMissingNode() && !node.isNull()) {
            dto.setPriorityId(node.path("id").asText());
            dto.setPriorityName(node.path("name").asText());
            dto.setPriorityIconUrl(node.path("iconUrl").asText());
        }
    }

    private void extractReporter(ProviderIssueDTO dto, JsonNode fields) {
        JsonNode node = fields.path("reporter");
        if (!node.isMissingNode() && !node.isNull()) {
            dto.setReporterName(node.path("displayName").asText());
            dto.setReporterAvatarUrl(node.path("avatarUrls").path("48x48").asText());
        }
    }

    private Map<String, String> buildMediaIdMap(JsonNode fields) {
        Map<String, String> map = new HashMap<>();
        JsonNode array = fields.path("attachment");
        if (array.isArray()) {
            for (JsonNode a : array) {
                String filename = a.path("filename").asText();
                String id = a.path("id").asText();
                if (!filename.isEmpty() && !id.isEmpty()) map.put(filename, id);
            }
        }
        return map;
    }

    private void extractComments(ProviderIssueDTO dto, JsonNode fields, Map<String, String> mediaIdToAttachmentId) {
        List<ProviderCommentDTO> comments = new ArrayList<>();
        JsonNode wrapper = fields.get("comment");
        if (wrapper != null && !wrapper.isNull()) {
            JsonNode array = wrapper.get("comments");
            if (array != null && array.isArray()) {
                for (JsonNode c : array) {
                    String author = extractAuthor(c);
                    String authorAvatarUrl = extractAuthorAvatar(c);
                    JsonNode bodyNode = c.get("body");
                    String body = "";
                    List<String> attachmentIds = new ArrayList<>();
                    if (bodyNode != null && !bodyNode.isNull()) {
                        body = bodyNode.isObject() ? AdfParser.adfToPlainText(bodyNode) : bodyNode.asText();
                        if (bodyNode.isObject()) {
                            for (String filename : AdfParser.extractMediaFilenames(bodyNode)) {
                                String numericId = mediaIdToAttachmentId.get(filename);
                                if (numericId != null) attachmentIds.add(numericId);
                            }
                        }
                    }
                    comments.add(new ProviderCommentDTO(author, authorAvatarUrl, body, attachmentIds));
                }
            }
        }
        java.util.Collections.reverse(comments);
        dto.setComments(comments);
    }

    private String extractAuthor(JsonNode comment) {
        JsonNode author = comment.get("author");
        if (author != null && !author.isNull()) {
            JsonNode name = author.get("displayName");
            if (name != null) return name.asText();
        }
        return "Unknown";
    }

    private String extractAuthorAvatar(JsonNode comment) {
        JsonNode author = comment.get("author");
        if (author != null && !author.isNull()) {
            JsonNode avatarUrls = author.get("avatarUrls");
            if (avatarUrls != null && !avatarUrls.isNull()) {
                JsonNode url = avatarUrls.get("48x48");
                if (url != null) return url.asText();
            }
        }
        return null;
    }

    private void extractVendorTicket(ProviderIssueDTO dto, JsonNode fields) {
        JsonNode node = fields.path(configPort.getVendorTicketField());
        if (!node.isMissingNode() && !node.isNull())
            dto.setVendorTicket(node.asText());
    }

    private void extractAttachments(ProviderIssueDTO dto, JsonNode fields) {
        List<ProviderAttachmentDTO> attachments = new ArrayList<>();
        JsonNode array = fields.path("attachment");
        if (array.isArray()) {
            for (JsonNode a : array) {
                attachments.add(new ProviderAttachmentDTO(
                        a.path("id").asText(),
                        a.path("filename").asText(),
                        a.path("mimeType").asText()
                ));
            }
        }
        dto.setAttachments(attachments);
    }
}
