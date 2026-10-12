package com.example.EVIM.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "atlassian_user_token")
@Getter
@Setter
@NoArgsConstructor
public class AtlassianUserToken {

    @Id
    private String keycloakSub;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private String cloudId;

    private Instant expiresAt;
}
