package com.example.EVIM.adapter.out.graph;

import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.model.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;

@Component
public class GraphEmailMapper {

    public Email toDomain(Message message) {
        Email email = new Email();
        email.setSender(extractSender(message));
        email.setSenderName(extractSenderName(message));
        email.setReceiver(extractFirstRecipient(message));
        email.setSubject(message.getSubject());
        email.setBody(message.getBody() != null ? message.getBody().getContent() : null);
        email.setReceived(message.getReceivedDateTime() != null
                ? LocalDateTime.ofInstant(message.getReceivedDateTime().toInstant(), ZoneOffset.UTC)
                : LocalDateTime.now());
        email.setStatus(Status.Open);
        email.setMessageId(message.getId());
        email.setAttachments(new ArrayList<>());
        return email;
    }

    public Attachment toAttachmentDomain(FileAttachment fa) {
        Attachment attachment = new Attachment();
        attachment.setFileName(fa.getName());
        attachment.setContentType(fa.getContentType());
        attachment.setContentBytes(fa.getContentBytes());
        return attachment;
    }

    private String extractSender(Message message) {
        if (message.getSender() == null) return null;
        if (message.getSender().getEmailAddress() == null) return null;
        return message.getSender().getEmailAddress().getAddress();
    }

    private String extractSenderName(Message message) {
        if (message.getSender() == null) return null;
        if (message.getSender().getEmailAddress() == null) return null;
        return message.getSender().getEmailAddress().getName();
    }

    private String extractFirstRecipient(Message message) {
        if (message.getToRecipients() == null || message.getToRecipients().isEmpty()) return null;
        var first = message.getToRecipients().get(0);
        if (first.getEmailAddress() == null) return null;
        return first.getEmailAddress().getAddress();
    }

    public Email fromJsonNode(JsonNode node) {
        Email email = new Email();
        email.setMessageId(node.has("id") ? node.get("id").asText() : null);
        email.setSubject(node.has("subject") ? node.get("subject").asText() : null);

        if (node.has("body") && node.get("body").has("content")) {
            email.setBody(node.get("body").get("content").asText());
        }

        if (node.has("receivedDateTime") && !node.get("receivedDateTime").isNull()) {
            OffsetDateTime odt = OffsetDateTime.parse(node.get("receivedDateTime").asText());
            email.setReceived(LocalDateTime.ofInstant(odt.toInstant(), ZoneOffset.UTC));
        } else {
            email.setReceived(LocalDateTime.now());
        }

        if (node.has("sender") && node.get("sender").has("emailAddress")) {
            JsonNode addr = node.get("sender").get("emailAddress");
            email.setSender(addr.has("address") ? addr.get("address").asText() : null);
            email.setSenderName(addr.has("name") ? addr.get("name").asText() : null);
        }

        if (node.has("toRecipients") && node.get("toRecipients").isArray() && !node.get("toRecipients").isEmpty()) {
            JsonNode firstTo = node.get("toRecipients").get(0);
            if (firstTo.has("emailAddress") && firstTo.get("emailAddress").has("address")) {
                email.setReceiver(firstTo.get("emailAddress").get("address").asText());
            }
        }

        email.setStatus(Status.Open);
        email.setAttachments(new ArrayList<>());
        return email;
    }

    public Attachment attachmentFromJson(JsonNode node) {
        Attachment attachment = new Attachment();
        attachment.setFileName(node.has("name") ? node.get("name").asText() : null);
        attachment.setContentType(node.has("contentType") ? node.get("contentType").asText() : null);
        if (node.has("contentBytes")) {
            attachment.setContentBytes(Base64.getDecoder().decode(node.get("contentBytes").asText()));
        }
        return attachment;
    }
}
