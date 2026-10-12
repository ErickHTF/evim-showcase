package com.example.EVIM.adapter.in.web;

import com.example.EVIM.application.AttachmentService;
import com.example.EVIM.application.EmailSendService;
import com.example.EVIM.application.dto.AttachmentResponseDTO;
import com.example.EVIM.application.dto.EmailResponseDTO;
import com.example.EVIM.application.dto.SendEmailRequestDTO;
import com.example.EVIM.application.dto.ThreadMessageDTO;
import com.example.EVIM.application.EmailService;
import com.example.EVIM.domain.model.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/email")
public class EmailController {

    private final EmailService emailService;
    private final AttachmentService attachmentService;
    private final EmailSendService emailSendService;

    public EmailController(EmailService emailService,
                           AttachmentService attachmentService,
                           EmailSendService emailSendService) {
        this.emailService      = emailService;
        this.attachmentService = attachmentService;
        this.emailSendService  = emailSendService;
    }

    @GetMapping
    public ResponseEntity<Page<EmailResponseDTO>> getAllEmails(
            @PageableDefault(size = 10, sort = "received") Pageable pageable
    ) {
        return ResponseEntity.ok(emailService.findAllEmails(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmailResponseDTO>> searchEmailsBySubject(@RequestParam String subject) {
        return ResponseEntity.ok(emailService.findEmailsBySubject(subject));
    }

    @GetMapping("/conversation/{id}")
    public ResponseEntity<List<EmailResponseDTO>> getConversationEmails(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.findEmailsByConversationId(id));
    }

    @GetMapping("/by-ticket/{ticketId}")
    public ResponseEntity<List<EmailResponseDTO>> getEmailsByTicketId(@PathVariable Integer ticketId) {
        return ResponseEntity.ok(emailService.findEmailsByTicketId(ticketId));
    }

    @GetMapping("/by-ticket/{ticketId}/thread-messages")
    public ResponseEntity<List<ThreadMessageDTO>> getThreadMessagesByTicketId(@PathVariable Integer ticketId) {
        return ResponseEntity.ok(emailService.findThreadMessagesByTicketId(ticketId));
    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> sendEmail(@RequestPart("data") SendEmailRequestDTO dto,
                                          @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                          JwtAuthenticationToken auth) {
        emailSendService.send(dto, files, auth);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{emailId}/attachments")
    public ResponseEntity<List<AttachmentResponseDTO>> getAttachments(@PathVariable Long emailId) {
        return ResponseEntity.ok(attachmentService.findByEmailId(emailId));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentService.findById(attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(attachment.getContentBytes());
    }
}
