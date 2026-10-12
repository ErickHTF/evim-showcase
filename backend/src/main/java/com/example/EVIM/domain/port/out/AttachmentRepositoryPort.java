package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.Attachment;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepositoryPort {
    Attachment save(Attachment attachment);
    List<Attachment> findByEmailId(Long emailId);
    Optional<Attachment> findById(Long id);
}
