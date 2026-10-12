package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.port.out.AttachmentRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AttachmentPersistenceAdapter implements AttachmentRepositoryPort {

    private final AttachmentJpaRepository repository;
    private final EmailJpaRepository emailRepository;
    private final AttachmentEntityMapper mapper;

    public AttachmentPersistenceAdapter(AttachmentJpaRepository repository,
                                        EmailJpaRepository emailRepository,
                                        AttachmentEntityMapper mapper) {
        this.repository      = repository;
        this.emailRepository = emailRepository;
        this.mapper          = mapper;
    }

    @Override
    public Attachment save(Attachment domain) {
        EmailJpaEntity emailEntity = emailRepository.findById(domain.getEmailId())
                .orElseThrow(() -> new RuntimeException("Email not found: " + domain.getEmailId()));
        return mapper.toDomain(repository.save(mapper.toEntity(domain, emailEntity)));
    }

    @Override
    public List<Attachment> findByEmailId(Long emailId) {
        return repository.findByEmailId(emailId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Attachment> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
