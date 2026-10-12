package com.example.EVIM.application;

import com.example.EVIM.adapter.out.jira.util.AdfBuilder;
import com.example.EVIM.application.dto.SendEmailRequestDTO;
import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.model.Status;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import com.example.EVIM.domain.port.out.EmailRepositoryPort;
import com.example.EVIM.domain.port.out.EmailSendPort;
import com.example.EVIM.domain.port.out.ProviderCommentPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmailSendService {

    private final EmailSendPort emailSendPort;
    private final ProviderCommentPort commentPort;
    private final EmailRepositoryPort emailRepository;
    private final ConversationRepositoryPort conversationRepository;
    private final SlaMetricsService slaMetricsService;
    private final ObjectMapper objectMapper;

    public EmailSendService(EmailSendPort emailSendPort,
                            ProviderCommentPort commentPort,
                            EmailRepositoryPort emailRepository,
                            ConversationRepositoryPort conversationRepository,
                            SlaMetricsService slaMetricsService,
                            ObjectMapper objectMapper) {
        this.emailSendPort = emailSendPort;
        this.commentPort = commentPort;
        this.emailRepository = emailRepository;
        this.conversationRepository = conversationRepository;
        this.slaMetricsService = slaMetricsService;
        this.objectMapper = objectMapper;
    }

    public void send(SendEmailRequestDTO dto, List<MultipartFile> files, JwtAuthenticationToken auth) {

        String userName = auth.getToken().getClaimAsString("name");
        if (userName == null || userName.isBlank()) {
            userName = auth.getToken().getClaimAsString("email");
        }

        String displayName = userName + " via ";

        String signature = "<br><br><hr><small><strong>" + userName + "</strong> via E-VIM</small>";
        String bodyWithSignature = dto.body() + signature;

        List<Attachment> attachments = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                try {
                    Attachment att = new Attachment();
                    att.setFileName(file.getOriginalFilename());
                    att.setContentType(file.getContentType());
                    att.setContentBytes(file.getBytes());
                    attachments.add(att);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read attachment: " + file.getOriginalFilename(), e);
                }
            }
        }

        emailSendPort.sendOnBehalf(displayName, dto.to(), dto.subject(), bodyWithSignature, attachments);

        Email outbound = new Email();
        outbound.setSender(displayName);
        outbound.setReceiver(dto.to());
        outbound.setSubject(dto.subject());
        outbound.setBody(bodyWithSignature);
        outbound.setReceived(LocalDateTime.now());
        outbound.setStatus(Status.Open);
        outbound.setAttachments(List.of());

        conversationRepository.findByTicketId(Integer.parseInt(
                dto.ticketId().replaceAll("\\D+", ""))
        ).ifPresent(conv -> {
            outbound.setConversationId(conv.getId());
            slaMetricsService.onInternalReply(conv.getId(), java.time.LocalDateTime.now());
        });

        emailRepository.save(outbound);

        if (dto.replicateToJira()) {
            String commentText = "E-mail enviado para " + dto.to() + " por " + userName + ":\n\n" + stripHtml(dto.body());
            Map<String, Object> adf = AdfBuilder.buildTextWithLineBreaks(commentText);
            try {
                String adfJson = objectMapper.writeValueAsString(adf);
                commentPort.addComment(dto.ticketId(), adfJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to build ADF for Jira comment", e);
            }
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").trim();
    }
}
