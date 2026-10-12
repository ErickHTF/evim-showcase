package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ThreadMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ThreadMessageJpaRepository extends JpaRepository<ThreadMessageJpaEntity, Long> {

    List<ThreadMessageJpaEntity> findByConversationIdOrderByOrdinalAsc(Long conversationId);

    Optional<ThreadMessageJpaEntity> findByConversationIdAndInteractionKey(Long conversationId, String interactionKey);

    @Modifying
    @Query("delete from ThreadMessageJpaEntity tm where tm.email.id = :emailId")
    int deleteByEmailId(Long emailId);
}
