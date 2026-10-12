package com.example.EVIM.application;

import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.port.out.EmailFetchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EmailIngestionService.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final EmailFetchPort emailFetchPort;
    private final EmailProcessingService emailProcessingService;

    public EmailIngestionService(EmailFetchPort emailFetchPort,
                                 EmailProcessingService emailProcessingService) {
        this.emailFetchPort          = emailFetchPort;
        this.emailProcessingService  = emailProcessingService;
    }

    public void ingestNewEmails() {
        if (!running.compareAndSet(false, true)) {
            log.info("Ingest already running, skipping");
            return;
        }

        log.info("Ingest starting — fetching emails from Graph API...");
        AtomicInteger processed = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        try {
            int totalFetched = emailFetchPort.fetchNewEmails(page -> {
                for (Email email : page) {
                    try {
                        boolean saved = emailProcessingService.processSingleEmail(email);
                        if (saved) {
                            processed.incrementAndGet();
                            emailFetchPort.markAsRead(email.getMessageId());
                        } else {
                            skipped.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        log.error("Failed to process email messageId={}: {}",
                                email.getMessageId(), e.getMessage());
                    }
                }
                int total = processed.get() + skipped.get() + errors.get();
                log.info("Ingest progress: {} processed, {} skipped, {} errors (total handled: {})",
                        processed.get(), skipped.get(), errors.get(), total);
            });

            log.info("Ingest complete: fetched={} processed={} skipped={} errors={}",
                    totalFetched, processed.get(), skipped.get(), errors.get());
        } catch (Throwable e) {
            log.error("Ingest FAILED ({}): {}", e.getClass().getName(), e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }
}
