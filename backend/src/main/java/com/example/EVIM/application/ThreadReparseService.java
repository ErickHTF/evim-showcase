package com.example.EVIM.application;

import com.example.EVIM.adapter.out.persistence.EmailJpaRepository;
import com.example.EVIM.adapter.out.persistence.entity.EmailJpaEntity;
import com.example.EVIM.application.metrics.ParseDiagnostic;
import com.example.EVIM.application.metrics.ParserMetrics;
import com.example.EVIM.domain.model.ThreadMessage;
import com.example.EVIM.domain.port.out.ThreadMessageRepositoryPort;
import com.example.EVIM.domain.service.PortalEmailThreadParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ThreadReparseService {

    private static final Logger log = LoggerFactory.getLogger(ThreadReparseService.class);
    private static final int MAX_BODY_LENGTH = 500_000;

    private final EmailJpaRepository emailRepository;
    private final ThreadMessageRepositoryPort threadMessageRepository;
    private final ParserMetrics parserMetrics;
    private final ThreadMessageBuilder threadMessageBuilder;

    public ThreadReparseService(EmailJpaRepository emailRepository,
                                ThreadMessageRepositoryPort threadMessageRepository,
                                ParserMetrics parserMetrics,
                                ThreadMessageBuilder threadMessageBuilder) {
        this.emailRepository = emailRepository;
        this.threadMessageRepository = threadMessageRepository;
        this.parserMetrics = parserMetrics;
        this.threadMessageBuilder = threadMessageBuilder;
    }

    public List<Long> findAllEmailIds() {
        return emailRepository.findAllIdsOrdered();
    }


    public List<Long> resolveScopedEmailIds(java.util.Collection<Long> conversationIds,
                                            java.util.Collection<Long> emailIds) {
        boolean hasConv = conversationIds != null && !conversationIds.isEmpty();
        boolean hasMail = emailIds != null && !emailIds.isEmpty();
        if (!hasConv && !hasMail) return findAllEmailIds();

        java.util.LinkedHashSet<Long> ids = new java.util.LinkedHashSet<>();
        if (hasConv) {
            ids.addAll(emailRepository.findIdsByConversationIdsOrdered(
                    new java.util.ArrayList<>(conversationIds)));
        }
        if (hasMail) {
            for (Long id : emailIds) if (id != null) ids.add(id);
        }
        java.util.List<Long> sorted = new java.util.ArrayList<>(ids);
        java.util.Collections.sort(sorted);
        return sorted;
    }


    public void reparseChunk(List<Long> emailIds, Counters counters) {
        if (emailIds == null || emailIds.isEmpty()) return;

        List<EmailJpaEntity> emails = emailRepository.findAllById(emailIds);
        for (EmailJpaEntity email : emails) {
            counters.processed.incrementAndGet();
            try {
                reparseOne(email.getId(), counters);
            } catch (org.springframework.dao.DataIntegrityViolationException dive) {


                try {
                    reparseOne(email.getId(), counters);
                } catch (Exception retryEx) {
                    log.warn("Reparse retry failed for email {}: {}", email.getId(), retryEx.getMessage());
                    counters.failed.incrementAndGet();
                }
            } catch (Exception e) {
                log.warn("Failed to reparse email {}: {}", email.getId(), e.getMessage());
                counters.failed.incrementAndGet();
            }
        }
    }

    @Transactional
    public void reparseOne(Long emailId, Counters counters) {
        EmailJpaEntity email = emailRepository.findById(emailId).orElse(null);
        if (email == null) {
            counters.failed.incrementAndGet();
            return;
        }
        processOne(email, counters);
    }

    private void processOne(EmailJpaEntity email, Counters counters) {
        String domain = ParserMetrics.extractDomain(email.getSender());

        if (email.getBody() != null && email.getBody().length() > MAX_BODY_LENGTH) {
            log.debug("Skipping email {} - body too large ({})", email.getId(), email.getBody().length());
            counters.failed.incrementAndGet();
            parserMetrics.record(ParseDiagnostic.failed(domain, "body-too-large"));
            return;
        }

        ThreadMessageBuilder.Build build = threadMessageBuilder.assemble(
                new ThreadMessageBuilder.Source(
                        email.getBody(),
                        email.getSender(),
                        email.getSenderName(),
                        null ));
        parserMetrics.record(build.diagnostic());

        List<PortalEmailThreadParser.ParsedMessage> parsed = build.messages();
        if (parsed.isEmpty()) {
            counters.failed.incrementAndGet();
            return;
        }

        boolean isFallback = build.diagnostic().outcome() == ParseDiagnostic.Outcome.FALLBACK;
        Long conversationId = email.getConversation() != null ? email.getConversation().getId() : null;





        threadMessageRepository.deleteByEmailId(email.getId());







        int ordinal = 0;
        for (PortalEmailThreadParser.ParsedMessage msg : parsed) {
            ThreadMessage tm = new ThreadMessage();
            tm.setEmailId(email.getId());
            tm.setConversationId(conversationId);
            tm.setAuthor(msg.author());
            tm.setCompany(msg.company());
            tm.setSentAt(msg.date());
            tm.setBody(msg.body());
            tm.setOrdinal(ordinal++);
            tm.setInteractionKey(ThreadInteractionKey.of(msg.author(), msg.date(), msg.body()));
            threadMessageRepository.upsertByInteractionKey(tm);
        }

        if (isFallback) {
            counters.fallback.incrementAndGet();
        } else {
            counters.newlyParsed.addAndGet(parsed.size());
        }
    }

    public static final class Counters {
        public final java.util.concurrent.atomic.AtomicInteger processed = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger updated = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger fallback = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger failed = new java.util.concurrent.atomic.AtomicInteger();
        public final java.util.concurrent.atomic.AtomicInteger newlyParsed = new java.util.concurrent.atomic.AtomicInteger();
    }
}
