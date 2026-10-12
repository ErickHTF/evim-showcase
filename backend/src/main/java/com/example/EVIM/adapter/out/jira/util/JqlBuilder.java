package com.example.EVIM.adapter.out.jira.util;

import com.example.EVIM.adapter.out.jira.ProviderStatusRepository;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JqlBuilder {

    private final ProviderConfigPort configPort;
    private final ProviderStatusRepository statusRepository;

    @Value("${jira.filter.sprint-only:false}")
    private boolean sprintOnly;

    public JqlBuilder(@Lazy ProviderConfigPort configPort,
                      ProviderStatusRepository statusRepository) {
        this.configPort       = configPort;
        this.statusRepository = statusRepository;
    }

    public String build(String status, String accountId, Boolean unassigned, Long conversationId, Long sprintId) {
        return build(status, accountId, unassigned, conversationId, sprintId, null);
    }

    public String build(String status, String accountId, Boolean unassigned, Long conversationId, Long sprintId, String text) {
        return build(status, accountId, unassigned, conversationId, sprintId, text, true);
    }

    public String build(String status, String accountId, Boolean unassigned, Long conversationId, Long sprintId, String text, Boolean vendorOnly) {
        StringBuilder jql = new StringBuilder("project = ").append(configPort.getProjectKey());

        String vendorField = configPort.getVendorTicketField();
        if (vendorField != null && !vendorField.isBlank()) {
            String fieldNumber = vendorField.replace("customfield_", "");
            if (Boolean.FALSE.equals(vendorOnly)) {
                jql.append(" AND cf[").append(fieldNumber).append("] is EMPTY");
            } else if (!Boolean.FALSE.equals(vendorOnly)) {
                jql.append(" AND cf[").append(fieldNumber).append("] is not EMPTY");
            }
        }

        if (sprintId != null) jql.append(" AND sprint = ").append(sprintId);
        else if (sprintOnly) jql.append(" AND sprint in openSprints()");
        appendStatus(jql, status);
        appendAssignee(jql, accountId, unassigned);
        appendConversationId(jql, conversationId);
        appendText(jql, text);

        return jql.append(" ORDER BY created DESC").toString();
    }

    public String buildForCount(String accountId, Boolean unassigned, Long sprintId, Boolean vendorOnly) {
        StringBuilder jql = new StringBuilder("project = ").append(configPort.getProjectKey());

        String vendorField = configPort.getVendorTicketField();
        if (vendorField != null && !vendorField.isBlank()) {
            String fieldNumber = vendorField.replace("customfield_", "");
            if (Boolean.FALSE.equals(vendorOnly)) {
                jql.append(" AND cf[").append(fieldNumber).append("] is EMPTY");
            } else if (Boolean.TRUE.equals(vendorOnly)) {
                jql.append(" AND cf[").append(fieldNumber).append("] is not EMPTY");
            }

        }

        if (sprintId != null) jql.append(" AND sprint = ").append(sprintId);
        else if (sprintOnly) jql.append(" AND sprint in openSprints()");
        appendAssignee(jql, accountId, unassigned);

        return jql.toString();
    }

    private void appendStatus(StringBuilder jql, String status) {
        if (status == null || status.isEmpty()) return;

        Map<String, String> statusMap = statusRepository.getStatusMap();
        String[] statuses = status.split(",");

        String inClause = Arrays.stream(statuses)
            .map(String::trim)
            .map(s -> {
                String id = statusMap.get(s);
                return (id != null && !id.isEmpty()) ? id : s;
            })
            .map(s -> "\"" + s + "\"")
            .collect(Collectors.joining(","));

        jql.append(" AND status in (").append(inClause).append(")");
    }

    private void appendAssignee(StringBuilder jql, String accountId, Boolean unassigned) {
        if (accountId != null && !accountId.isEmpty())
            jql.append(" AND assignee = \"").append(accountId).append("\"");
        else if (Boolean.TRUE.equals(unassigned))
            jql.append(" AND assignee is EMPTY");
    }

    private void appendConversationId(StringBuilder jql, Long conversationId) {
        if (conversationId == null) return;

        String fieldNumber = configPort.getVendorTicketField().replace("customfield_", "");
        jql.append(" AND cf[").append(fieldNumber).append("] ~ \"").append(conversationId).append("\"");
    }

    private void appendText(StringBuilder jql, String text) {
        if (text == null || text.isBlank()) return;
        String sanitized = text.replace("\"", "\\\"");
        jql.append(" AND text ~ \"").append(sanitized).append("\"");
    }
}
