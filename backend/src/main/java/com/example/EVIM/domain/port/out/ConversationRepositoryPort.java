package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ConversationRepositoryPort {

    Page<Conversation> findAllByOrderByLastUpdateDesc(Pageable pageable);

    Page<Conversation> findByTicketIdIn(List<Integer> ticketIds, Pageable pageable);

    Page<Conversation> findByFilter(Status status, Long ticketId, Pageable pageable);

    Optional<Conversation> findById(Long id);

    Optional<Conversation> findByTicketId(Integer ticketId);

    List<Conversation> findByPendingResponseTrue();

    Conversation save(Conversation conversation);
}
