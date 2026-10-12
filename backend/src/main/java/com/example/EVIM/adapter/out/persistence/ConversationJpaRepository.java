package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ConversationJpaRepository extends
        JpaRepository<ConversationJpaEntity, Long>,
        JpaSpecificationExecutor<ConversationJpaEntity> {

    Page<ConversationJpaEntity> findAllByOrderByLastUpdateDesc(Pageable pageable);

    Page<ConversationJpaEntity> findByTicketIdIn(List<Integer> ticketIds, Pageable pageable);

    Optional<ConversationJpaEntity> findByTicketId(Integer ticketId);

    List<ConversationJpaEntity> findByPendingResponseTrue();
}
