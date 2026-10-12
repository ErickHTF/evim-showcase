package com.example.EVIM.adapter.in.scheduler;

import com.example.EVIM.application.ProviderService;
import com.example.EVIM.application.SlaMetricsService;
import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SlaPollingScheduler {

    private final SlaMetricsService slaMetricsService;
    private final ProviderService providerService;
    private final ConversationRepositoryPort conversationRepository;

    public SlaPollingScheduler(SlaMetricsService slaMetricsService,
                               ProviderService providerService,
                               ConversationRepositoryPort conversationRepository) {
        this.slaMetricsService = slaMetricsService;
        this.providerService   = providerService;
        this.conversationRepository = conversationRepository;
    }

    @Scheduled(fixedDelayString = "${sla.poll.fixed-delay-ms:600000}")
    public void checkResolvedIssues() {
        List<Long> activeConversationIds = slaMetricsService.getActiveConversationIds();
        for (Long conversationId : activeConversationIds) {
            try {
                String jiraKey = conversationRepository.findById(conversationId)
                        .map(c -> c.getExternalBoardId())
                        .orElse(null);
                if (jiraKey == null || jiraKey.isBlank()) continue;

                ProviderIssueDTO issue = providerService.getCleanIssue(jiraKey);
                if (issue != null && isDone(issue.getStatus())) {
                    slaMetricsService.onIssueResolved(conversationId, LocalDateTime.now());
                }
            } catch (Exception ignored) {

            }
        }
    }

    private boolean isDone(String status) {
        if (status == null) return false;
        String l = status.toLowerCase();
        return l.contains("done") || l.contains("close") || l.contains("resolv") || l.contains("conclu");
    }
}
