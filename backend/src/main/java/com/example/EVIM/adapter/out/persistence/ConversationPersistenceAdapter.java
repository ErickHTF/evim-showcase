package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.Status;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ConversationPersistenceAdapter implements ConversationRepositoryPort {

    private final ConversationJpaRepository repository;
    private final ConversationEntityMapper mapper;

    public ConversationPersistenceAdapter(ConversationJpaRepository repository,
                                          ConversationEntityMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    @Override
    public Page<Conversation> findAllByOrderByLastUpdateDesc(Pageable pageable) {
        return repository.findAllByOrderByLastUpdateDesc(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Conversation> findByTicketIdIn(List<Integer> ticketIds, Pageable pageable) {
        return repository.findByTicketIdIn(ticketIds, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Conversation> findByFilter(Status status, Long ticketId, Pageable pageable) {
        return repository.findAll(ConversationSpecification.getFilter(status, ticketId), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Conversation> findByTicketId(Integer ticketId) {
        return repository.findByTicketId(ticketId).map(mapper::toDomain);
    }

    @Override
    public List<Conversation> findByPendingResponseTrue() {
        return repository.findByPendingResponseTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity entity = conversation.getId() != null
                ? repository.findById(conversation.getId()).orElse(new ConversationJpaEntity())
                : new ConversationJpaEntity();
        mapper.updateEntity(entity, conversation);
        return mapper.toDomain(repository.save(entity));
    }
}
