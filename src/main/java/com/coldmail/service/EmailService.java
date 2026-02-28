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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final RecipientRepository recipientRepository;
    private final EmailLogRepository emailLogRepository;

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
            // Send to all pending recipients
            recipients = recipientRepository.findByStatus(RecipientStatus.PENDING);
        } else {
            recipients = recipientRepository.findAllById(recipientIds);
        }

        return recipients.stream()
                .map(recipient -> sendEmailToRecipient(recipient, template))
                .toList();
    }

    /**
     * Core method to send email and log result
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient.getEmail());
            helper.setSubject(processedSubject);
            helper.setText(processedBody, true); // true = HTML

            mailSender.send(message);

            emailLog.setStatus(EmailStatus.SUCCESS);
            recipient.setStatus(RecipientStatus.SENT);
            log.info("Email sent successfully to: {}", recipient.getEmail());

        } catch (MessagingException e) {
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
     * Supports: {{NAME}}, {{COMPANY}}, {{POSITION}}, {{EMAIL}}
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

    /**
     * Get email logs for a recipient
     */
    public List<EmailLog> getEmailLogs(Long recipientId) {
        return emailLogRepository.findByRecipientId(recipientId);
    }

    public List<EmailLog> getAllEmailLogs() {
        return emailLogRepository.findAll();
    }
}