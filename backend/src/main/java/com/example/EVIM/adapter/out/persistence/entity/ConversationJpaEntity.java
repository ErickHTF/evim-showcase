package com.example.EVIM.adapter.out.persistence.entity;

import com.example.EVIM.domain.model.Priority;
import com.example.EVIM.domain.model.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tb_conversation")
@Entity
public class ConversationJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer ticketId;

    private String subject;

    private Status status;

    private Priority priority;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column(name = "pending_response")
    private Boolean pendingResponse = false;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<EmailJpaEntity> emails;

    @Column(name = "board_id")
    private String externalBoardId;
}
