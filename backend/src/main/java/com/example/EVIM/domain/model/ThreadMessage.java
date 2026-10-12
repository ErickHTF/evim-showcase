package com.example.EVIM.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThreadMessage {

    private Long id;
    private Long emailId;
    private Long conversationId;
    private String author;
    private String company;
    private LocalDateTime sentAt;
    private String body;
    private Integer ordinal;

    private String interactionKey;
}
