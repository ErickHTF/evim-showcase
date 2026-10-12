package com.example.EVIM.adapter.out.persistence.entity;

import com.example.EVIM.domain.model.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "tb_email")
@Entity
public class EmailJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;

    @Column(name = "sender_name")
    private String senderName;

    private String receiver;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private LocalDateTime received;

    private Status status;

    @Column(name = "message_id", columnDefinition = "TEXT", unique = true)
    private String messageId;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private ConversationJpaEntity conversation;

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<AttachmentJpaEntity> attachments = new ArrayList<>();
}
