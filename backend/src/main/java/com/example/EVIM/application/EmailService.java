package com.example.EVIM.application;

import com.example.EVIM.application.dto.AttachmentResponseDTO;
import com.example.EVIM.application.dto.EmailResponseDTO;
import com.example.EVIM.application.dto.ThreadMessageDTO;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.model.ThreadMessage;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import com.example.EVIM.domain.port.out.EmailRepositoryPort;
import com.example.EVIM.domain.port.out.ThreadMessageRepositoryPort;
import com.example.EVIM.domain.service.HtmlSanitizer;
import com.example.EVIM.domain.service.PortalEmailThreadParser;
import com.example.EVIM.domain.service.SenderClassificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final EmailRepositoryPort emailRepository;
    private final ConversationRepositoryPort conversationRepository;
    private final ThreadMessageRepositoryPort threadMessageRepository;
    private final SenderClassificationService senderClassificationService;

    public EmailService(EmailRepositoryPort emailRepository,
                        ConversationRepositoryPort conversationRepository,
                        ThreadMessageRepositoryPort threadMessageRepository,
                        SenderClassificationService senderClassificationService) {
        this.emailRepository          = emailRepository;
        this.conversationRepository   = conversationRepository;
        this.threadMessageRepository  = threadMessageRepository;
        this.senderClassificationService = senderClassificationService;
    }

    public Page<EmailResponseDTO> findAllEmails(Pageable pageable) {
        return emailRepository.findAll(pageable).map(this::convertToDTO);
    }

    public List<EmailResponseDTO> findEmailsBySubject(String subject) {
        return emailRepository.findBySubjectContainingIgnoreCase(subject).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<EmailResponseDTO> findEmailsByConversationId(Long conversationId) {
        return emailRepository.findByConversationId(conversationId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<EmailResponseDTO> findEmailsByTicketId(Integer ticketId) {
        return conversationRepository.findByTicketId(ticketId)
                .map(conv -> findEmailsByConversationId(conv.getId()))
                .orElse(List.of());
    }

    public List<ThreadMessageDTO> findThreadMessagesByTicketId(Integer ticketId) {
        return conversationRepository.findByTicketId(ticketId)
                .map(conv -> {



                    java.util.Map<Long, String> senderByEmailId = emailRepository.findByConversationId(conv.getId()).stream()
                            .filter(e -> e.getId() != null)
                            .collect(Collectors.toMap(Email::getId, e -> e.getSender() == null ? "" : e.getSender(), (a, b) -> a));
                    return threadMessageRepository.findByConversationId(conv.getId()).stream()
                            .map(tm -> convertToThreadDTO(tm, senderByEmailId))
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    private EmailResponseDTO convertToDTO(Email email) {
        List<AttachmentResponseDTO> attachments = email.getAttachments() == null
                ? List.of()
                : email.getAttachments().stream()
                        .map(a -> new AttachmentResponseDTO(a.getId(), a.getFileName(),
                                a.getContentType(), a.getEmailId()))
                        .toList();

        boolean isVendor = !senderClassificationService.isInternalSender(email.getSender());
        String safeBody = HtmlSanitizer.sanitize(email.getBody());

        return new EmailResponseDTO(
                email.getId(), email.getSender(), email.getSenderName(), email.getReceiver(),
                email.getSubject(), safeBody, email.getReceived(),
                email.getStatus(), isVendor, attachments
        );
    }

    private ThreadMessageDTO convertToThreadDTO(ThreadMessage tm, java.util.Map<Long, String> senderByEmailId) {
        boolean isVendor = classifyThreadMessage(tm, senderByEmailId);
        String cleanBody = PortalEmailThreadParser.stripBoilerplate(tm.getBody());
        String htmlBody = HtmlSanitizer.plainTextToHtml(cleanBody);

        return new ThreadMessageDTO(
                tm.getId(), tm.getAuthor(), tm.getCompany(),
                tm.getSentAt(), htmlBody, tm.getOrdinal(), isVendor
        );
    }

    private boolean classifyThreadMessage(ThreadMessage tm, java.util.Map<Long, String> senderByEmailId) {

        if (tm.getCompany() != null && !tm.getCompany().isBlank()) return true;

        String author = tm.getAuthor();
        if (author != null && author.contains("@")) {
            return !senderClassificationService.isInternalSender(author);
        }

        String parentSender = tm.getEmailId() == null ? null : senderByEmailId.get(tm.getEmailId());
        if (parentSender != null && !parentSender.isBlank()) {
            return !senderClassificationService.isInternalSender(parentSender);
        }

        return true;
    }
}
