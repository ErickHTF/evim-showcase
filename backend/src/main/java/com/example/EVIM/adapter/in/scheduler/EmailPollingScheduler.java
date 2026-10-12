package com.example.EVIM.adapter.in.scheduler;

import com.example.EVIM.application.EmailIngestionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailPollingScheduler {

    private final EmailIngestionService ingestionService;

    public EmailPollingScheduler(EmailIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(fixedDelayString = "${graph.poll.fixed-delay-ms:60000}")
    public void poll() {
        ingestionService.ingestNewEmails();
    }
}
