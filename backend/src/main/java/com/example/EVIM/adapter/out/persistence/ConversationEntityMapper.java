package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.domain.model.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationEntityMapper {

    public Conversation toDomain(ConversationJpaEntity entity) {
        return new Conversation(
                entity.getId(),
                entity.getTicketId(),
                entity.getSubject(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getLastUpdate(),
                entity.getPendingResponse(),
                entity.getExternalBoardId()
        );
    }

    public void updateEntity(ConversationJpaEntity entity, Conversation domain) {
        entity.setTicketId(domain.getTicketId());
        entity.setSubject(domain.getSubject());
        entity.setStatus(domain.getStatus());
        entity.setPriority(domain.getPriority());
        entity.setLastUpdate(domain.getLastUpdate());
        entity.setPendingResponse(domain.getPendingResponse());
        entity.setExternalBoardId(domain.getExternalBoardId());
    }
}
