package com.coldmail.service;

import com.coldmail.model.EmailLog;
import com.coldmail.model.EmailLog.EmailStatus;
import com.coldmail.model.EmailTemplate;
import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.model.Resume;
import com.coldmail.repository.EmailLogRepository;
import com.coldmail.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService {

    private final EmailTemplateService templateService;
    private final RecipientRepository recipientRepository;
    private final EmailLogRepository emailLogRepository;
    private final ResumeService resumeService;

    @Value("${email.api-key:}")
    private String apiKey;

    @Value("${email.from-email:noreply@example.com}")
    private String fromEmail;

    @Value("${email.from-name:Cold Mail App}")
    private String fromName;

    @Value("${email.provider:brevo}")
    private String emailProvider;

    // ==================== PUBLIC METHODS ====================

    public EmailLog sendEmail(Long recipientId, Long templateId) {
        return sendEmail(recipientId, templateId, null);
    }

    public EmailLog sendEmail(Long recipientId, Long templateId, Long resumeId) {
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        EmailTemplate template = templateService.getTemplateById(templateId);

        Resume resume = null;
        if (resumeId != null) {
            resume = resumeService.getResumeById(resumeId).orElse(null);
        } else {
            resume = resumeService.getDefaultResume().orElse(null);
        }

        return sendEmailToRecipient(recipient, template, resume);
    }

    public List<EmailLog> sendBulkEmails(Long templateId, List<Long> recipientIds) {
        return sendBulkEmails(templateId, recipientIds, null);
    }

    public List<EmailLog> sendBulkEmails(Long templateId, List<Long> recipientIds, Long resumeId) {
        EmailTemplate template = templateService.getTemplateById(templateId);

        List<Recipient> recipients;
        if (recipientIds == null || recipientIds.isEmpty()) {
            recipients = recipientRepository.findByStatus(RecipientStatus.PENDING);
        } else {
            recipients = recipientRepository.findAllById(recipientIds);
        }

        Resume resume = null;
        if (resumeId != null) {
            resume = resumeService.getResumeById(resumeId).orElse(null);
        } else {
            resume = resumeService.getDefaultResume().orElse(null);
        }

        final Resume finalResume = resume;
        return recipients.stream()
                .map(recipient -> sendEmailToRecipient(recipient, template, finalResume))
                .toList();
    }

    public EmailLog retryEmail(Long emailLogId) {
        EmailLog oldLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new RuntimeException("Email log not found"));

        if (oldLog.getStatus() != EmailStatus.FAILED) {
            throw new RuntimeException("Can only retry failed emails");
        }

        Recipient recipient = oldLog.getRecipient();
        EmailTemplate template = oldLog.getTemplate();

        recipient.setStatus(RecipientStatus.PENDING);
        recipientRepository.save(recipient);

        Resume resume = resumeService.getDefaultResume().orElse(null);

        return sendEmailToRecipient(recipient, template, resume);
    }

    public List<EmailLog> retryAllFailed() {
        List<EmailLog> failedLogs = emailLogRepository.findByStatus(EmailStatus.FAILED);

        return failedLogs.stream()
                .map(log -> {
                    try {
                        return retryEmail(log.getId());
                    } catch (Exception e) {
                        log.setErrorMessage("Retry failed: " + e.getMessage());
                        return log;
                    }
                })
                .toList();
    }

    public long getFailedCount() {
        return emailLogRepository.findByStatus(EmailStatus.FAILED).size();
    }

    public List<EmailLog> getEmailLogs(Long recipientId) {
        return emailLogRepository.findByRecipientId(recipientId);
    }

    public List<EmailLog> getAllEmailLogs() {
        return emailLogRepository.findAll();
    }

    // ==================== PRIVATE METHODS ====================

    private EmailLog sendEmailToRecipient(Recipient recipient, EmailTemplate template, Resume resume) {
        String processedSubject = processTemplate(template.getSubject(), recipient);
        String processedBody = processTemplate(template.getBody(), recipient);

        EmailLog emailLog = EmailLog.builder()
                .recipient(recipient)
                .template(template)
                .subject(processedSubject)
                .body(processedBody)
                .sentAt(LocalDateTime.now())
                .build();

        try {
            if ("brevo".equalsIgnoreCase(emailProvider)) {
                sendViaBrevo(recipient.getEmail(), processedSubject, processedBody, resume);
            } else {
                sendViaResend(recipient.getEmail(), processedSubject, processedBody, resume);
            }

            emailLog.setStatus(EmailStatus.SUCCESS);
            recipient.setStatus(RecipientStatus.SENT);
            log.info("Email sent successfully via {} to: {}", emailProvider, recipient.getEmail());

        } catch (Exception e) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            recipient.setStatus(RecipientStatus.FAILED);
            log.error("Failed to send email to: {}", recipient.getEmail(), e);
        }

        recipientRepository.save(recipient);
        return emailLogRepository.save(emailLog);
    }

    /**
     * Send email via Brevo (Sendinblue) API
     */
    private void sendViaBrevo(String to, String subject, String body, Resume resume) {
        log.info("Sending via Brevo - API Key starts with: {}",
                apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "NOT SET");
        log.info("From email: {}, From name: {}", fromEmail, fromName);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        Map<String, Object> requestBody = new HashMap<>();

        // Sender
        Map<String, String> sender = new HashMap<>();
        sender.put("name", fromName);
        sender.put("email", fromEmail);
        requestBody.put("sender", sender);

        // Recipients
        List<Map<String, String>> toList = new ArrayList<>();
        Map<String, String> toRecipient = new HashMap<>();
        toRecipient.put("email", to);
        toList.add(toRecipient);
        requestBody.put("to", toList);

        // Subject and body
        requestBody.put("subject", subject);
        requestBody.put("htmlContent", body);

        // Attachment (if resume provided)
        if (resume != null && resume.getData() != null) {
            List<Map<String, String>> attachments = new ArrayList<>();
            Map<String, String> attachment = new HashMap<>();
            attachment.put("name", resume.getOriginalFileName());
            attachment.put("content", Base64.getEncoder().encodeToString(resume.getData()));
            attachments.add(attachment);
            requestBody.put("attachment", attachments);
            log.info("Attaching resume: {}", resume.getOriginalFileName());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.brevo.com/v3/smtp/email",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Brevo API error: " + response.getBody());
            }

            log.info("Brevo response: {}", response.getBody());

        } catch (Exception e) {
            log.error("Brevo API error: {}", e.getMessage());
            throw new RuntimeException("Failed to send via Brevo: " + e.getMessage());
        }
    }

    /**
     * Send email via Resend API
     */
    private void sendViaResend(String to, String subject, String body, Resume resume) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("from", fromEmail);
        requestBody.put("to", new String[]{to});
        requestBody.put("subject", subject);
        requestBody.put("html", body);

        if (resume != null && resume.getData() != null) {
            List<Map<String, String>> attachments = new ArrayList<>();
            Map<String, String> attachment = new HashMap<>();
            attachment.put("filename", resume.getOriginalFileName());
            attachment.put("content", Base64.getEncoder().encodeToString(resume.getData()));
            attachments.add(attachment);
            requestBody.put("attachments", attachments);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.resend.com/emails",
                HttpMethod.POST,
                request,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Resend API error: " + response.getBody());
        }
    }

    private String processTemplate(String template, Recipient recipient) {
        if (template == null) return "";

        return template
                .replace("{{NAME}}", nullSafe(recipient.getName()))
                .replace("{{COMPANY}}", nullSafe(recipient.getCompany()))
                .replace("{{POSITION}}", nullSafe(recipient.getPosition()))
                .replace("{{EMAIL}}", nullSafe(recipient.getEmail()));
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}