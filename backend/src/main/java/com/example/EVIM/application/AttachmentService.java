package com.example.EVIM.application;

import com.example.EVIM.application.dto.AttachmentResponseDTO;
import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.port.out.AttachmentRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttachmentService {

    private final AttachmentRepositoryPort attachmentRepository;

    public AttachmentService(AttachmentRepositoryPort attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public List<AttachmentResponseDTO> findByEmailId(Long emailId) {
        return attachmentRepository.findByEmailId(emailId).stream()
                .map(a -> new AttachmentResponseDTO(a.getId(), a.getFileName(),
                        a.getContentType(), a.getEmailId()))
                .toList();
    }

    public Attachment findById(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + id));
    }
}
