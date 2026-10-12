package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.Attachment;

import java.util.List;

public interface EmailSendPort {
    void sendOnBehalf(String displayName, String to, String subject, String htmlBody, List<Attachment> attachments);
}
