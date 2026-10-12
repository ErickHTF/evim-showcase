package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.AttachmentJpaEntity;
import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import com.example.EVIM.domain.model.Attachment;
import org.springframework.stereotype.Component;

@Component
public class AttachmentEntityMapper {

    public Attachment toDomain(AttachmentJpaEntity entity) {
        return new Attachment(
                entity.getId(),
                entity.getFileName(),
                entity.getContentType(),
                entity.getContentBytes(),
                entity.getEmail().getId()
        );
    }

    public AttachmentJpaEntity toEntity(Attachment domain, EmailJpaEntity emailEntity) {
        AttachmentJpaEntity entity = new AttachmentJpaEntity();
        entity.setFileName(domain.getFileName());
        entity.setContentType(domain.getContentType());
        entity.setContentBytes(domain.getContentBytes());
        entity.setEmail(emailEntity);
        return entity;
    }
}
