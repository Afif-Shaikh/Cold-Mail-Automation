package com.coldmail.controller;

import com.coldmail.dto.ApiResponse;
import com.coldmail.dto.SendEmailRequest;
import com.coldmail.model.EmailLog;
import com.coldmail.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<EmailLog>>> sendEmails(
            @Valid @RequestBody SendEmailRequest request) {
        List<EmailLog> logs = emailService.sendBulkEmails(
                request.getTemplateId(),
                request.getRecipientIds()
        );
        return ResponseEntity.ok(ApiResponse.success("Emails processed", logs));
    }

    @PostMapping("/send/{recipientId}")
    public ResponseEntity<ApiResponse<EmailLog>> sendSingleEmail(
            @PathVariable Long recipientId,
            @RequestParam Long templateId) {
        EmailLog log = emailService.sendEmail(recipientId, templateId);
        return ResponseEntity.ok(ApiResponse.success("Email sent", log));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<EmailLog>>> getAllLogs() {
        return ResponseEntity.ok(ApiResponse.success(emailService.getAllEmailLogs()));
    }

    @GetMapping("/logs/recipient/{recipientId}")
    public ResponseEntity<ApiResponse<List<EmailLog>>> getLogsByRecipient(
            @PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(emailService.getEmailLogs(recipientId)));
    }
}