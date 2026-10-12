package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.AttachmentJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.model.Email;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmailEntityMapper {

    public Email toDomain(EmailJpaEntity entity) {
        Long conversationId = entity.getConversation() != null
                ? entity.getConversation().getId()
                : null;

        List<Attachment> attachments = new ArrayList<>();
        if (entity.getAttachments() != null) {
            for (AttachmentJpaEntity a : entity.getAttachments()) {
                attachments.add(new Attachment(
                        a.getId(),
                        a.getFileName(),
                        a.getContentType(),
                        a.getContentBytes(),
                        entity.getId()
                ));
            }
        }

        return new Email(
                entity.getId(),
                entity.getSender(),
                entity.getSenderName(),
                entity.getReceiver(),
                entity.getSubject(),
                entity.getBody(),
                entity.getReceived(),
                entity.getStatus(),
                entity.getMessageId(),
                conversationId,
                attachments
        );
    }

    public EmailJpaEntity toEntity(Email domain, ConversationJpaEntity conversation) {
        EmailJpaEntity entity = new EmailJpaEntity();
        entity.setSender(domain.getSender());
        entity.setSenderName(domain.getSenderName());
        entity.setReceiver(domain.getReceiver());
        entity.setSubject(domain.getSubject());
        entity.setBody(domain.getBody());
        entity.setReceived(domain.getReceived());
        entity.setStatus(domain.getStatus());
        entity.setMessageId(domain.getMessageId());
        entity.setConversation(conversation);
        return entity;
    }
}
