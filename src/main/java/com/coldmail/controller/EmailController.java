package com.coldmail.controller;

import com.coldmail.dto.ApiResponse;
import com.coldmail.dto.SendEmailRequest;
import com.coldmail.model.EmailLog;
import com.coldmail.service.ResendEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final ResendEmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<EmailLog>>> sendEmails(
            @Valid @RequestBody SendEmailRequest request) {

        Long resumeId = request.isAttachResume() ? request.getResumeId() : null;

        List<EmailLog> logs = emailService.sendBulkEmails(
                request.getTemplateId(),
                request.getRecipientIds(),
                resumeId
        );
        return ResponseEntity.ok(ApiResponse.success("Emails processed", logs));
    }

    @PostMapping("/send/{recipientId}")
    public ResponseEntity<ApiResponse<EmailLog>> sendSingleEmail(
            @PathVariable Long recipientId,
            @RequestParam Long templateId,
            @RequestParam(required = false) Long resumeId) {
        EmailLog log = emailService.sendEmail(recipientId, templateId, resumeId);
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

    // NEW: Retry single failed email
    @PostMapping("/retry/{logId}")
    public ResponseEntity<ApiResponse<EmailLog>> retryEmail(@PathVariable Long logId) {
        EmailLog log = emailService.retryEmail(logId);
        return ResponseEntity.ok(ApiResponse.success("Email retry processed", log));
    }

    // NEW: Retry all failed emails
    @PostMapping("/retry-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryAllFailed() {
        List<EmailLog> logs = emailService.retryAllFailed();
        long successCount = logs.stream().filter(l -> l.getStatus() == EmailLog.EmailStatus.SUCCESS).count();
        long failedCount = logs.size() - successCount;

        return ResponseEntity.ok(ApiResponse.success(
                "Retry complete",
                Map.of(
                        "total", logs.size(),
                        "success", successCount,
                        "failed", failedCount
                )
        ));
    }

    // NEW: Get failed count
    @GetMapping("/failed-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFailedCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", emailService.getFailedCount())
        ));
    }
}