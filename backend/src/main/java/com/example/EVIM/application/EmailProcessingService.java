package com.example.EVIM.application;

import com.example.EVIM.application.metrics.ParseDiagnostic;
import com.example.EVIM.application.metrics.ParserMetrics;
import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.model.Status;
import com.example.EVIM.domain.model.ThreadMessage;
import com.example.EVIM.domain.port.out.AttachmentRepositoryPort;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import com.example.EVIM.domain.port.out.EmailRepositoryPort;
import com.example.EVIM.domain.port.out.ThreadMessageRepositoryPort;
import com.example.EVIM.domain.service.PortalEmailThreadParser;
import com.example.EVIM.domain.service.SenderClassificationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EmailProcessingService.class);

    private static final Pattern SUBJECT_TICKET_PATTERN  = Pattern.compile("(?:ticket|chamado|solicitação|solicita[çc][aã]o|#|nº)\\s*(\\d{8})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CC_TICKET_PATTERN       = Pattern.compile("\\((\\d{8})\\)");
    private static final Pattern REQUEST_TICKET_PATTERN  = Pattern.compile("(?i)solicita[çc][aã]o\\s+n[^\\d]*(\\d{8})");

    private final EmailRepositoryPort emailRepository;
    private final AttachmentRepositoryPort attachmentRepository;
    private final ConversationRepositoryPort conversationRepository;
    private final ThreadMessageRepositoryPort threadMessageRepository;
    private final SenderClassificationService senderClassificationService;
    private final SlaMetricsService slaMetricsService;
    private final ParserMetrics parserMetrics;
    private final ThreadMessageBuilder threadMessageBuilder;

    public EmailProcessingService(EmailRepositoryPort emailRepository,
                                  AttachmentRepositoryPort attachmentRepository,
                                  ConversationRepositoryPort conversationRepository,
                                  ThreadMessageRepositoryPort threadMessageRepository,
                                  SenderClassificationService senderClassificationService,
                                  SlaMetricsService slaMetricsService,
                                  ParserMetrics parserMetrics,
                                  ThreadMessageBuilder threadMessageBuilder) {
        this.emailRepository              = emailRepository;
        this.attachmentRepository         = attachmentRepository;
        this.conversationRepository       = conversationRepository;
        this.threadMessageRepository      = threadMessageRepository;
        this.senderClassificationService  = senderClassificationService;
        this.slaMetricsService            = slaMetricsService;
        this.parserMetrics                = parserMetrics;
        this.threadMessageBuilder         = threadMessageBuilder;
    }

    @Transactional
    public boolean processSingleEmail(Email email) {
        if (emailRepository.findByMessageId(email.getMessageId()).isPresent()) {
            return false;
        }



        Integer ticketId = null;
        String vendorTicket = extractVendorTicket(email.getSubject(), email.getBody());
        if (vendorTicket != null) {
            try {
                ticketId = Integer.parseInt(vendorTicket);
            } catch (NumberFormatException ignored) {

            }
        }

        Conversation conversation = resolveConversation(email, ticketId);

        if (conversation.getTicketId() == null && ticketId != null) {
            conversation.setTicketId(ticketId);
        }

        LocalDateTime received = email.getReceived() != null ? email.getReceived() : LocalDateTime.now();
        conversation.setLastUpdate(received);
        conversation.setPendingResponse(!senderClassificationService.isInternalSender(email.getSender()));
        conversationRepository.save(conversation);

        email.setConversationId(conversation.getId());
        List<Attachment> attachments = email.getAttachments();
        email.setAttachments(List.of());

        Email saved = emailRepository.save(email);

        for (Attachment attachment : attachments) {
            attachment.setEmailId(saved.getId());
            attachmentRepository.save(attachment);
        }

        parseThreadMessages(saved, conversation);

        boolean isVendorEmail = !senderClassificationService.isInternalSender(email.getSender());
        if (isVendorEmail) {
            slaMetricsService.onVendorEmailReceived(conversation.getId(), email.getReceived());
        }

        return true;
    }

    private Conversation resolveConversation(Email email, Integer ticketId) {




        if (ticketId != null) {
            Optional<Conversation> byTicket = conversationRepository.findByTicketId(ticketId);
            if (byTicket.isPresent()) {
                return byTicket.get();
            }
        }


        String normalizedSubject = normalizeSubject(email.getSubject());
        if (!normalizedSubject.isEmpty()) {
            List<Long> conversationIds = emailRepository.findConversationIdsBySubjectContaining(normalizedSubject);
            if (!conversationIds.isEmpty()) {
                return conversationRepository.findById(conversationIds.get(0))
                        .orElseGet(() -> createConversation(email));
            }
        }
        return createConversation(email);
    }

    private Conversation createConversation(Email email) {
        Conversation conv = new Conversation();
        conv.setSubject(email.getSubject());
        conv.setStatus(Status.Open);
        conv.setLastUpdate(email.getReceived() != null ? email.getReceived() : LocalDateTime.now());
        conv.setPendingResponse(true);
        return conversationRepository.save(conv);
    }

    private String extractVendorTicket(String subject, String body) {
        if (subject != null) {
            Matcher m = SUBJECT_TICKET_PATTERN.matcher(subject);
            if (m.find()) return m.group(1);
        }

        if (body != null) {
            String plainBody = body.replaceAll("<[^>]+>", " ");

            Matcher mCc = CC_TICKET_PATTERN.matcher(plainBody);
            if (mCc.find()) return mCc.group(1);

            Matcher mReq = REQUEST_TICKET_PATTERN.matcher(plainBody);
            if (mReq.find()) return mReq.group(1);
        }

        return null;
    }

    private String normalizeSubject(String subject) {
        if (subject == null) return "";
        String result = subject.trim();
        String prev;
        do {
            prev = result;

            result = result.replaceFirst("(?i)^(Re|Res|Resp|Fwd?|Enc|ENC|AW|RV)\\s*:\\s*", "");


            result = result.replaceFirst("^\\s*[\\[(][^\\])]{0,80}[\\])]\\s*", "");


            result = result.replaceFirst(
                    "(?i)^(Pesquisa de satisfação|Pesquisa de satisfacao|Solicitação pendente|Solicitacao pendente|Solicitação Recebida|Solicitacao Recebida|Solicitação)\\s*(n[ºo°.:]?)?\\s*\\d*\\s*[-:–]?\\s*",
                    "");

            result = result.replaceFirst("^[\\s\\-:–(]*\\d{6,}[\\s\\-:–)]+", "");
            result = result.trim();
        } while (!result.equals(prev));
        return result;
    }

    private void parseThreadMessages(Email saved, Conversation conversation) {
        ThreadMessageBuilder.Build build = threadMessageBuilder.assemble(
                new ThreadMessageBuilder.Source(
                        saved.getBody(),
                        saved.getSender(),
                        saved.getSenderName(),
                        saved.getReceived()));

        parserMetrics.record(build.diagnostic());
        List<PortalEmailThreadParser.ParsedMessage> subMessages = build.messages();

        if (subMessages.isEmpty()) {
            return;
        }

        if (build.diagnostic().outcome() == ParseDiagnostic.Outcome.FALLBACK) {
            log.debug("Parser fallback: stored full body as single thread message for email id={}", saved.getId());
        } else {
            log.info("Parsed {} thread messages from email id={} for conversation={}",
                    subMessages.size(), saved.getId(), conversation.getId());
        }






        int writtenOrdinal = 0;
        for (PortalEmailThreadParser.ParsedMessage msg : subMessages) {
            ThreadMessage tm = new ThreadMessage();
            tm.setEmailId(saved.getId());
            tm.setConversationId(conversation.getId());
            tm.setAuthor(msg.author());
            tm.setCompany(msg.company());
            tm.setSentAt(msg.date());
            tm.setBody(msg.body());
            tm.setOrdinal(writtenOrdinal++);
            tm.setInteractionKey(ThreadInteractionKey.of(msg.author(), msg.date(), msg.body()));
            threadMessageRepository.upsertByInteractionKey(tm);
        }
    }

}
