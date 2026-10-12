package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.AttachmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentJpaRepository extends JpaRepository<AttachmentJpaEntity, Long> {
    List<AttachmentJpaEntity> findByEmailId(Long emailId);
}
