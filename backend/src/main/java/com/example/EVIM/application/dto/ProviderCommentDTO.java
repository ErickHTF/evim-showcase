package com.example.EVIM.application.dto;

import java.util.List;

public record ProviderCommentDTO(String author, String authorAvatarUrl, String body, List<String> attachmentIds) {}
