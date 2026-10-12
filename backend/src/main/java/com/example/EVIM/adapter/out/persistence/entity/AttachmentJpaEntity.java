package com.example.EVIM.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_attachment")
public class AttachmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Column(name = "content_bytes", columnDefinition = "BYTEA")
    private byte[] contentBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailJpaEntity email;
}
