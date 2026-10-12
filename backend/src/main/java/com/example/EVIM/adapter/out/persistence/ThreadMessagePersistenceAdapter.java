package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.ThreadMessageJpaEntity;
import com.example.EVIM.domain.model.ThreadMessage;
import com.example.EVIM.domain.port.out.ThreadMessageRepositoryPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ThreadMessagePersistenceAdapter implements ThreadMessageRepositoryPort {

    private final ThreadMessageJpaRepository repository;
    private final EmailJpaRepository emailRepository;
    private final ConversationJpaRepository conversationRepository;
    private final ThreadMessageEntityMapper mapper;

    public ThreadMessagePersistenceAdapter(ThreadMessageJpaRepository repository,
                                           EmailJpaRepository emailRepository,
                                           ConversationJpaRepository conversationRepository,
                                           ThreadMessageEntityMapper mapper) {
        this.repository = repository;
        this.emailRepository = emailRepository;
        this.conversationRepository = conversationRepository;
        this.mapper = mapper;
    }

    @Override
    public List<ThreadMessage> findByConversationId(Long conversationId) {
        return repository.findByConversationIdOrderByOrdinalAsc(conversationId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ThreadMessage save(ThreadMessage message) {



        EmailJpaEntity email = message.getEmailId() != null
                ? emailRepository.getReferenceById(message.getEmailId())
                : null;
        ConversationJpaEntity conversation = message.getConversationId() != null
                ? conversationRepository.getReferenceById(message.getConversationId())
                : null;
        return mapper.toDomain(repository.save(mapper.toEntity(message, email, conversation)));
    }

    @Override
    @Transactional
    public ThreadMessage upsertByInteractionKey(ThreadMessage message) {
        if (message.getConversationId() == null || message.getInteractionKey() == null) {
            return save(message);
        }

        Optional<ThreadMessageJpaEntity> existingOpt = repository
                .findByConversationIdAndInteractionKey(message.getConversationId(), message.getInteractionKey());

        if (existingOpt.isEmpty()) {
            return save(message);
        }

        ThreadMessageJpaEntity existing = existingOpt.get();
        EmailJpaEntity currentOwner = existing.getEmail();



        if (message.getEmailId() != null
                && (currentOwner == null || message.getEmailId() < currentOwner.getId())) {
            EmailJpaEntity newOwner = emailRepository.getReferenceById(message.getEmailId());
            existing.setEmail(newOwner);
            return mapper.toDomain(repository.save(existing));
        }

        return mapper.toDomain(existing);
    }

    @Override
    @Transactional
    public int deleteByEmailId(Long emailId) {
        if (emailId == null) return 0;
        return repository.deleteByEmailId(emailId);
    }
}
