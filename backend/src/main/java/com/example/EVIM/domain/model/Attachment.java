package com.example.EVIM.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private Long id;
    private String fileName;
    private String contentType;
    private byte[] contentBytes;
    private Long emailId;
}
