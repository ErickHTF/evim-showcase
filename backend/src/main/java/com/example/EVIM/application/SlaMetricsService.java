package com.example.EVIM.application;

import com.example.EVIM.adapter.out.jira.ProviderIssueRepository;
import com.example.EVIM.application.dto.MetricsDTO;
import com.example.EVIM.application.dto.MetricsSummaryDTO;
import com.example.EVIM.application.dto.TicketMetricsDTO;
import com.example.EVIM.domain.model.Conversation;
import com.example.EVIM.domain.model.OwnerType;
import com.example.EVIM.domain.model.SlaOwnershipLog;
import com.example.EVIM.domain.port.out.ConversationRepositoryPort;
import com.example.EVIM.domain.port.out.SlaOwnershipLogPort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SlaMetricsService {

    private final SlaOwnershipLogPort port;
    private final ProviderIssueRepository issueRepository;
    private final ConversationRepositoryPort conversationRepository;

    public SlaMetricsService(SlaOwnershipLogPort port,
                             ProviderIssueRepository issueRepository,
                             ConversationRepositoryPort conversationRepository) {
        this.port = port;
        this.issueRepository = issueRepository;
        this.conversationRepository = conversationRepository;
    }


    @Transactional
    public void onJiraLinked(Long conversationId, boolean pendingVendorResponse, LocalDateTime when) {
        if (conversationId == null) return;
        boolean alreadyTracked = !port.findAllByConversationId(conversationId).isEmpty();
        if (alreadyTracked) return;

        OwnerType initial = pendingVendorResponse ? OwnerType.VENDOR : OwnerType.INTERNAL;
        port.save(new SlaOwnershipLog(null, conversationId, initial, ts(when), null));
    }


    @Transactional
    public void onVendorEmailReceived(Long conversationId, LocalDateTime when) {
        if (conversationId == null) return;
        LocalDateTime ts = ts(when);
        Optional<SlaOwnershipLog> activeOpt = port.findActiveByConversationId(conversationId);
        if (activeOpt.isEmpty()) {
            port.save(new SlaOwnershipLog(null, conversationId, OwnerType.INTERNAL, ts, null));
            return;
        }
        SlaOwnershipLog active = activeOpt.get();
        if (active.getOwnerType() == OwnerType.VENDOR) {
            active.setEndAt(ts);
            port.save(active);
            port.save(new SlaOwnershipLog(null, conversationId, OwnerType.INTERNAL, ts, null));
        }
    }


    @Transactional
    public void onInternalReply(Long conversationId, LocalDateTime when) {
        if (conversationId == null) return;
        LocalDateTime ts = ts(when);
        Optional<SlaOwnershipLog> activeOpt = port.findActiveByConversationId(conversationId);
        if (activeOpt.isEmpty()) {
            port.save(new SlaOwnershipLog(null, conversationId, OwnerType.VENDOR, ts, null));
            return;
        }
        SlaOwnershipLog active = activeOpt.get();
        if (active.getOwnerType() == OwnerType.INTERNAL) {
            active.setEndAt(ts);
            port.save(active);
            port.save(new SlaOwnershipLog(null, conversationId, OwnerType.VENDOR, ts, null));
        }
    }


    @Transactional
    public void onIssueResolved(Long conversationId, LocalDateTime when) {
        if (conversationId == null) return;
        port.findActiveByConversationId(conversationId).ifPresent(active -> {
            active.setEndAt(ts(when));
            port.save(active);
        });
    }


    public List<Long> getActiveConversationIds() {
        return port.findAllActiveConversationIds();
    }

    public MetricsDTO getMetrics(Long sprintId) {
        List<Long> conversationIds = port.findAllDistinctConversationIds();

        Map<Long, Conversation> byId = new HashMap<>();
        for (Long id : conversationIds) {
            conversationRepository.findById(id).ifPresent(c -> byId.put(id, c));
        }

        if (sprintId != null) {
            Set<String> sprintKeys = issueRepository
                    .findByJql("sprint = " + sprintId + " ORDER BY created DESC", null, 200)
                    .issues().stream()
                    .map(n -> n.path("key").asText())
                    .collect(Collectors.toSet());
            conversationIds = conversationIds.stream()
                    .filter(id -> {
                        Conversation c = byId.get(id);
                        return c != null && c.getExternalBoardId() != null
                                && sprintKeys.contains(c.getExternalBoardId());
                    })
                    .collect(Collectors.toList());
        }

        List<TicketMetricsDTO> tickets = conversationIds.stream()
                .map(id -> computeTicketMetrics(id, byId.get(id)))
                .toList();
        return new MetricsDTO(computeSummary(tickets), tickets);
    }

    private TicketMetricsDTO computeTicketMetrics(Long conversationId, Conversation conv) {
        List<SlaOwnershipLog> logs = port.findAllByConversationId(conversationId);

        long vendorTimeSeconds = logs.stream()
                .filter(l -> l.getOwnerType() == OwnerType.VENDOR)
                .mapToLong(l -> {
                    LocalDateTime end = l.getEndAt() != null ? l.getEndAt() : LocalDateTime.now();
                    return Duration.between(l.getStartAt(), end).getSeconds();
                })
                .sum();

        Long firstResponseSeconds = logs.stream()
                .filter(l -> l.getOwnerType() == OwnerType.VENDOR && l.getEndAt() != null)
                .min(Comparator.comparing(SlaOwnershipLog::getStartAt))
                .map(l -> Duration.between(l.getStartAt(), l.getEndAt()).getSeconds())
                .orElse(null);

        int pingPongScore = logs.size();

        String currentOwner = logs.stream()
                .filter(l -> l.getEndAt() == null)
                .map(l -> l.getOwnerType().name())
                .findFirst()
                .orElse("CLOSED");

        LocalDateTime trackingSince = logs.stream()
                .min(Comparator.comparing(SlaOwnershipLog::getStartAt))
                .map(SlaOwnershipLog::getStartAt)
                .orElse(null);

        String jiraKey = conv != null ? conv.getExternalBoardId() : null;
        Integer ticketId = conv != null ? conv.getTicketId() : null;
        String subject = conv != null ? conv.getSubject() : null;

        return new TicketMetricsDTO(conversationId, jiraKey, ticketId, subject,
                currentOwner, vendorTimeSeconds, firstResponseSeconds,
                pingPongScore, trackingSince);
    }

    private MetricsSummaryDTO computeSummary(List<TicketMetricsDTO> tickets) {
        long avgVendorTime = (long) tickets.stream()
                .mapToLong(TicketMetricsDTO::vendorTimeSeconds)
                .average()
                .orElse(0);

        long avgFirstResponse = (long) tickets.stream()
                .filter(t -> t.firstResponseSeconds() != null)
                .mapToLong(TicketMetricsDTO::firstResponseSeconds)
                .average()
                .orElse(0);

        int waitingForVendor = (int) tickets.stream()
                .filter(t -> "VENDOR".equals(t.currentOwner()))
                .count();

        int waitingForInternal = (int) tickets.stream()
                .filter(t -> "INTERNAL".equals(t.currentOwner()))
                .count();

        return new MetricsSummaryDTO(tickets.size(), waitingForVendor, waitingForInternal,
                avgVendorTime, avgFirstResponse);
    }

    private static LocalDateTime ts(LocalDateTime when) {
        return when != null ? when : LocalDateTime.now();
    }
}
