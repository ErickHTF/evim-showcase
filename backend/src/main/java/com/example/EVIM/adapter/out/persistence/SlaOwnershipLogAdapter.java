package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.SlaOwnershipLogJpaEntity;
import com.example.EVIM.domain.model.SlaOwnershipLog;
import com.example.EVIM.domain.port.out.SlaOwnershipLogPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SlaOwnershipLogAdapter implements SlaOwnershipLogPort {

    private final SlaOwnershipLogJpaRepository repository;

    public SlaOwnershipLogAdapter(SlaOwnershipLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SlaOwnershipLog> findActiveByConversationId(Long conversationId) {
        return repository.findByConversationIdAndEndAtIsNull(conversationId).map(this::toDomain);
    }

    @Override
    public List<SlaOwnershipLog> findAllByConversationId(Long conversationId) {
        return repository.findAllByConversationIdOrderByStartAtAsc(conversationId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findAllDistinctConversationIds() {
        return repository.findAllDistinctConversationIds();
    }

    @Override
    public List<Long> findAllActiveConversationIds() {
        return repository.findAllActiveConversationIds();
    }

    @Override
    public SlaOwnershipLog save(SlaOwnershipLog log) {
        SlaOwnershipLogJpaEntity entity = log.getId() != null
                ? repository.findById(log.getId()).orElse(new SlaOwnershipLogJpaEntity())
                : new SlaOwnershipLogJpaEntity();
        entity.setConversationId(log.getConversationId());
        entity.setOwnerType(log.getOwnerType());
        entity.setStartAt(log.getStartAt());
        entity.setEndAt(log.getEndAt());
        return toDomain(repository.save(entity));
    }

    private SlaOwnershipLog toDomain(SlaOwnershipLogJpaEntity e) {
        return new SlaOwnershipLog(e.getId(), e.getConversationId(), e.getOwnerType(), e.getStartAt(), e.getEndAt());
    }
}
