package com.example.EVIM.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(
    name = "tb_thread_message",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_thread_message_conv_interaction",
        columnNames = {"conversation_id", "interaction_key"}
    )
)
@Entity
public class ThreadMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "email_id")
    private EmailJpaEntity email;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private ConversationJpaEntity conversation;

    private String author;

    private String company;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Integer ordinal;

    @Column(name = "interaction_key", length = 80)
    private String interactionKey;
}
