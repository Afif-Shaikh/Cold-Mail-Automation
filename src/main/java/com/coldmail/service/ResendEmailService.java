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

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

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
            // Use default resume if available
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
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", fromEmail);
            requestBody.put("to", new String[]{recipient.getEmail()});
            requestBody.put("subject", processedSubject);
            requestBody.put("html", processedBody);

            // Add attachment if resume is provided
            if (resume != null && resume.getData() != null) {
                List<Map<String, String>> attachments = new ArrayList<>();
                Map<String, String> attachment = new HashMap<>();
                attachment.put("filename", resume.getOriginalFileName());
                attachment.put("content", Base64.getEncoder().encodeToString(resume.getData()));
                attachments.add(attachment);
                requestBody.put("attachments", attachments);
                log.info("Attaching resume: {}", resume.getOriginalFileName());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

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