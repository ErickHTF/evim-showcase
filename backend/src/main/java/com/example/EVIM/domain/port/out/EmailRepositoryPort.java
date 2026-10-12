package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EmailRepositoryPort {

    Page<Email> findAll(Pageable pageable);

    List<Email> findBySubjectContainingIgnoreCase(String subject);

    List<Long> findConversationIdsBySubjectContaining(String subject);

    List<Email> findByConversationId(Long conversationId);

    Optional<Email> findTopByConversationIdOrderByReceivedDesc(Long conversationId);

    Optional<Email> findByMessageId(String messageId);

    Email save(Email email);
}
