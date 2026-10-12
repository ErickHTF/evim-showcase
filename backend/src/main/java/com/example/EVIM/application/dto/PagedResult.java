package com.example.EVIM.application.dto;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        String nextPageToken,
        int size
) {}
