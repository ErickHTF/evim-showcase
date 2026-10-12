package com.example.EVIM.adapter.out.persistence.entity;

import com.example.EVIM.domain.model.OwnerType;
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
@Table(name = "sla_ownership_log")
public class SlaOwnershipLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private OwnerType ownerType;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;
}
