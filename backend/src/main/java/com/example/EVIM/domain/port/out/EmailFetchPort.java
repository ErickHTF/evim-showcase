package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.Email;

import java.util.List;
import java.util.function.Consumer;

public interface EmailFetchPort {
    int fetchNewEmails(Consumer<List<Email>> pageConsumer);

    void markAsRead(String messageId);
}
