package com.example.EVIM.domain.port.out;

import java.util.List;

public interface StarredTicketPort {

    List<String> findKeysByUserId(String userId);

    boolean exists(String userId, String ticketKey);

    void save(String userId, String ticketKey);

    void delete(String userId, String ticketKey);
}
