package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.StarredTicketJpaEntity;
import com.example.EVIM.domain.port.out.StarredTicketPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StarredTicketPersistenceAdapter implements StarredTicketPort {

    private final StarredTicketJpaRepository repository;

    public StarredTicketPersistenceAdapter(StarredTicketJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> findKeysByUserId(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(StarredTicketJpaEntity::getTicketKey)
                .toList();
    }

    @Override
    public boolean exists(String userId, String ticketKey) {
        return repository.existsByUserIdAndTicketKey(userId, ticketKey);
    }

    @Override
    public void save(String userId, String ticketKey) {
        if (!repository.existsByUserIdAndTicketKey(userId, ticketKey)) {
            StarredTicketJpaEntity entity = new StarredTicketJpaEntity();
            entity.setUserId(userId);
            entity.setTicketKey(ticketKey);
            entity.setCreatedAt(LocalDateTime.now());
            repository.save(entity);
        }
    }

    @Override
    @Transactional
    public void delete(String userId, String ticketKey) {
        repository.deleteByUserIdAndTicketKey(userId, ticketKey);
    }
}
