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
public class Conversation {

    private Long id;
    private Integer ticketId;
    private String subject;
    private Status status;
    private Priority priority;
    private LocalDateTime lastUpdate;
    private Boolean pendingResponse;
    private String externalBoardId;
}