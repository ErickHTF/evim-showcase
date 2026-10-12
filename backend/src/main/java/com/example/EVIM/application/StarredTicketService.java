package com.example.EVIM.application;

import com.example.EVIM.application.dto.ProviderIssueDTO;
import com.example.EVIM.domain.port.out.ProviderIssuePort;
import com.example.EVIM.domain.port.out.StarredTicketPort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class StarredTicketService {

    private final StarredTicketPort starredTicketPort;
    private final ProviderIssuePort issuePort;

    public StarredTicketService(StarredTicketPort starredTicketPort, ProviderIssuePort issuePort) {
        this.starredTicketPort = starredTicketPort;
        this.issuePort = issuePort;
    }

    public List<String> getStarredKeys(String userId) {
        return starredTicketPort.findKeysByUserId(userId);
    }

    public List<ProviderIssueDTO> getStarredTickets(String userId) {
        List<String> keys = starredTicketPort.findKeysByUserId(userId);
        if (keys.isEmpty()) return Collections.emptyList();

        return keys.stream()
                .map(key -> {
                    try {
                        return issuePort.getCleanIssue(key);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public void star(String userId, String ticketKey) {
        starredTicketPort.save(userId, ticketKey);
    }

    public void unstar(String userId, String ticketKey) {
        starredTicketPort.delete(userId, ticketKey);
    }

    public boolean isStarred(String userId, String ticketKey) {
        return starredTicketPort.exists(userId, ticketKey);
    }
}
