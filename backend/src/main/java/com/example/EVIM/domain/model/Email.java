package com.example.EVIM.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    private Long id;
    private String sender;
    private String senderName;
    private String receiver;
    private String subject;
    private String body;
    private LocalDateTime received;
    private Status status;
    private String messageId;
    private Long conversationId;
    private List<Attachment> attachments = new ArrayList<>();
}