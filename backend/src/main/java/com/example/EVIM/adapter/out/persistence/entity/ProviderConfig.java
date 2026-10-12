package com.example.EVIM.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Entity
@Table(name = "tb_system_config")
public class ProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false)
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;

    @Column(name = "description")
    private String description;

}

