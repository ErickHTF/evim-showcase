package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.port.out.EmailRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EmailPersistenceAdapter implements EmailRepositoryPort {

    private final EmailJpaRepository repository;
    private final ConversationJpaRepository conversationRepository;
    private final EmailEntityMapper mapper;

    public EmailPersistenceAdapter(EmailJpaRepository repository,
                                   ConversationJpaRepository conversationRepository,
                                   EmailEntityMapper mapper) {
        this.repository             = repository;
        this.conversationRepository = conversationRepository;
        this.mapper                 = mapper;
    }

    @Override
    public Page<Email> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Email> findBySubjectContainingIgnoreCase(String subject) {
        return repository.findBySubjectContainingIgnoreCase(subject).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findConversationIdsBySubjectContaining(String subject) {
        return repository.findConversationIdsBySubjectContaining(subject);
    }

    @Override
    public List<Email> findByConversationId(Long conversationId) {
        return repository.findByConversationId(conversationId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Email> findTopByConversationIdOrderByReceivedDesc(Long conversationId) {
        return repository.findTopByConversationIdOrderByReceivedDesc(conversationId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Email> findByMessageId(String messageId) {
        return repository.findByMessageId(messageId).map(mapper::toDomain);
    }

    @Override
    public Email save(Email email) {
        ConversationJpaEntity conversation = email.getConversationId() != null
                ? conversationRepository.findById(email.getConversationId())
                        .orElseThrow(() -> new RuntimeException("Conversation not found: " + email.getConversationId()))
                : null;
        return mapper.toDomain(repository.save(mapper.toEntity(email, conversation)));
    }
}
