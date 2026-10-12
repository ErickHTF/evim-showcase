package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.ThreadMessageJpaEntity;
import com.example.EVIM.domain.model.ThreadMessage;
import org.springframework.stereotype.Component;

@Component
public class ThreadMessageEntityMapper {

    public ThreadMessage toDomain(ThreadMessageJpaEntity entity) {
        return new ThreadMessage(
                entity.getId(),
                entity.getEmail() != null ? entity.getEmail().getId() : null,
                entity.getConversation() != null ? entity.getConversation().getId() : null,
                entity.getAuthor(),
                entity.getCompany(),
                entity.getSentAt(),
                entity.getBody(),
                entity.getOrdinal(),
                entity.getInteractionKey()
        );
    }

    public ThreadMessageJpaEntity toEntity(ThreadMessage domain,
                                           EmailJpaEntity email,
                                           ConversationJpaEntity conversation) {
        ThreadMessageJpaEntity entity = new ThreadMessageJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(email);
        entity.setConversation(conversation);
        entity.setAuthor(domain.getAuthor());
        entity.setCompany(domain.getCompany());
        entity.setSentAt(domain.getSentAt());
        entity.setBody(domain.getBody());
        entity.setOrdinal(domain.getOrdinal());
        entity.setInteractionKey(domain.getInteractionKey());
        return entity;
    }
}
