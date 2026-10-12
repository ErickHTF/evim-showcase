package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.SlaOwnershipLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SlaOwnershipLogJpaRepository extends JpaRepository<SlaOwnershipLogJpaEntity, Long> {

    Optional<SlaOwnershipLogJpaEntity> findByConversationIdAndEndAtIsNull(Long conversationId);

    List<SlaOwnershipLogJpaEntity> findAllByConversationIdOrderByStartAtAsc(Long conversationId);

    @Query("SELECT DISTINCT l.conversationId FROM SlaOwnershipLogJpaEntity l")
    List<Long> findAllDistinctConversationIds();

    @Query("SELECT DISTINCT l.conversationId FROM SlaOwnershipLogJpaEntity l WHERE l.endAt IS NULL")
    List<Long> findAllActiveConversationIds();
}
