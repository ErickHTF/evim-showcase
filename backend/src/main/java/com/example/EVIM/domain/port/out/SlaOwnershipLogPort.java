package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.SlaOwnershipLog;

import java.util.List;
import java.util.Optional;

public interface SlaOwnershipLogPort {

    Optional<SlaOwnershipLog> findActiveByConversationId(Long conversationId);

    List<SlaOwnershipLog> findAllByConversationId(Long conversationId);

    List<Long> findAllDistinctConversationIds();

    List<Long> findAllActiveConversationIds();

    SlaOwnershipLog save(SlaOwnershipLog log);
}
