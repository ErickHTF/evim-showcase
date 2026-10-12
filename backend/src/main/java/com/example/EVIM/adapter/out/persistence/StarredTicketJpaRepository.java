package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.StarredTicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StarredTicketJpaRepository extends JpaRepository<StarredTicketJpaEntity, Long> {

    List<StarredTicketJpaEntity> findAllByUserId(String userId);

    Optional<StarredTicketJpaEntity> findByUserIdAndTicketKey(String userId, String ticketKey);

    boolean existsByUserIdAndTicketKey(String userId, String ticketKey);

    void deleteByUserIdAndTicketKey(String userId, String ticketKey);
}
