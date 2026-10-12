package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, Long> {
    Optional<ProviderConfig> findByKey(String key);
}
