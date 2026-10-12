package com.example.EVIM.adapter.out.graph;

import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.port.out.EmailFetchPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "graph.mock.enabled", havingValue = "true")
public class GraphMailFetchMockAdapter implements EmailFetchPort {

    @Override
    public int fetchNewEmails(Consumer<List<Email>> pageConsumer) {
        return 0;
    }

    @Override
    public void markAsRead(String messageId) {
    }
}
