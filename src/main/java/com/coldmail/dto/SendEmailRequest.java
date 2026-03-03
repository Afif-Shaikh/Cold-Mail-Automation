package com.coldmail.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SendEmailRequest {

    @NotNull(message = "Template ID is required")
    private Long templateId;

    // If empty, sends to all PENDING recipients
    private List<Long> recipientIds;

    // Optional resume ID, if null uses default resume
    private Long resumeId;

    // Whether to attach resume
    private boolean attachResume = true;
}