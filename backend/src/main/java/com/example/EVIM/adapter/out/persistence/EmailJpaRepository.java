package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailJpaRepository extends JpaRepository<EmailJpaEntity, Long> {

    List<EmailJpaEntity> findBySubjectContainingIgnoreCase(String subject);

    @Query("SELECT DISTINCT e.conversation.id FROM EmailJpaEntity e WHERE LOWER(e.subject) LIKE LOWER(CONCAT('%', :subject, '%')) AND e.conversation IS NOT NULL ORDER BY e.conversation.id ASC")
    List<Long> findConversationIdsBySubjectContaining(@Param("subject") String subject);

    List<EmailJpaEntity> findByConversationId(Long conversationId);

    Optional<EmailJpaEntity> findTopByConversationIdOrderByReceivedDesc(Long conversationId);

    Optional<EmailJpaEntity> findByMessageId(String messageId);

    @Query("SELECT e.conversation.id, e.sender FROM EmailJpaEntity e WHERE e.conversation.id IN :conversationIds AND e.received = (SELECT MAX(e2.received) FROM EmailJpaEntity e2 WHERE e2.conversation.id = e.conversation.id)")
    List<Object[]> findLastSenderByConversationIds(@Param("conversationIds") List<Long> conversationIds);

    @Query("SELECT e.id FROM EmailJpaEntity e ORDER BY e.id ASC")
    List<Long> findAllIdsOrdered();

    @Query("SELECT e.id FROM EmailJpaEntity e WHERE e.conversation.id IN :conversationIds ORDER BY e.id ASC")
    List<Long> findIdsByConversationIdsOrdered(@Param("conversationIds") List<Long> conversationIds);
}
