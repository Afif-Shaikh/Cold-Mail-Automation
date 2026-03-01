package com.coldmail.service;

import com.coldmail.model.EmailLog;
import com.coldmail.model.EmailLog.EmailStatus;
import com.coldmail.model.EmailTemplate;
import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.repository.EmailLogRepository;
import com.coldmail.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService {

    private final EmailTemplateService templateService;
    private final RecipientRepository recipientRepository;
    private final EmailLogRepository emailLogRepository;

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    /**
     * Send email to a single recipient using a template
     */
    public EmailLog sendEmail(Long recipientId, Long templateId) {
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        EmailTemplate template = templateService.getTemplateById(templateId);

        return sendEmailToRecipient(recipient, template);
    }

    /**
     * Send emails to multiple recipients
     */
    public List<EmailLog> sendBulkEmails(Long templateId, List<Long> recipientIds) {
        EmailTemplate template = templateService.getTemplateById(templateId);

        List<Recipient> recipients;
        if (recipientIds == null || recipientIds.isEmpty()) {
            recipients = recipientRepository.findByStatus(RecipientStatus.PENDING);
        } else {
            recipients = recipientRepository.findAllById(recipientIds);
        }

        return recipients.stream()
                .map(recipient -> sendEmailToRecipient(recipient, template))
                .toList();
    }

    /**
     * Core method to send email via Resend API using RestTemplate
     */
    private EmailLog sendEmailToRecipient(Recipient recipient, EmailTemplate template) {
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
            RestTemplate restTemplate = new RestTemplate();

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", fromEmail);
            requestBody.put("to", new String[]{recipient.getEmail()});
            requestBody.put("subject", processedSubject);
            requestBody.put("html", processedBody);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Send request
            ResponseEntity<Map> response = restTemplate.exchange(
                    RESEND_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                emailLog.setStatus(EmailStatus.SUCCESS);
                recipient.setStatus(RecipientStatus.SENT);
                log.info("Email sent successfully to: {}", recipient.getEmail());
            } else {
                throw new RuntimeException("Failed to send email: " + response.getBody());
            }

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
     * Replace placeholders in template with actual values
     */
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

    public List<EmailLog> getEmailLogs(Long recipientId) {
        return emailLogRepository.findByRecipientId(recipientId);
    }

    public List<EmailLog> getAllEmailLogs() {
        return emailLogRepository.findAll();
    }
}