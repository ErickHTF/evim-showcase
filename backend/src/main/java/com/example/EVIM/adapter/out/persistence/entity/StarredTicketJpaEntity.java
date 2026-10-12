package com.example.EVIM.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "starred_ticket", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "ticket_key"})
})
public class StarredTicketJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "ticket_key", nullable = false, length = 50)
    private String ticketKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
