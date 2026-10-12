package com.example.EVIM.adapter.out.graph;

import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.port.out.EmailSendPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "graph.mock.enabled", havingValue = "true")
public class GraphMailSendMockAdapter implements EmailSendPort {

    @Override
    public void sendOnBehalf(String displayName, String to, String subject, String htmlBody, List<Attachment> attachments) {
    }
}
