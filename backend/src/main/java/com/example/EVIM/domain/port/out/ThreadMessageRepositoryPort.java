package com.example.EVIM.domain.port.out;

import com.example.EVIM.domain.model.ThreadMessage;

import java.util.List;

public interface ThreadMessageRepositoryPort {

    List<ThreadMessage> findByConversationId(Long conversationId);

    ThreadMessage save(ThreadMessage message);


    ThreadMessage upsertByInteractionKey(ThreadMessage message);


    int deleteByEmailId(Long emailId);
}
