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
public class SlaOwnershipLog {

    private Long id;
    private Long conversationId;
    private OwnerType ownerType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
