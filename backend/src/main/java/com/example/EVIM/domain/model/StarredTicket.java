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
public class StarredTicket {
    private Long id;
    private String userId;
    private String ticketKey;
    private LocalDateTime createdAt;
}
