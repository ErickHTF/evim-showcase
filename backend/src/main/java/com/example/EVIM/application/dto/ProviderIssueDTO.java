package com.example.EVIM.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProviderIssueDTO {

    private String key;
    private String summary;
    private String status;
    private String assignee;
    private String assigneeAccountId;
    private String assigneeAvatarUrl;
    private String description;
    private List<ProviderCommentDTO> comments;
    private String priorityName;
    private String priorityIconUrl;
    private String reporterName;
    private String reporterAvatarUrl;
    private String vendorTicket;
    private String priorityId;
    private List<String> descriptionAttachmentIds;
    private List<ProviderAttachmentDTO> attachments;
}