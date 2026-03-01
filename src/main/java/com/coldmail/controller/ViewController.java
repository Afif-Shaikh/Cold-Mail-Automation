package com.coldmail.controller;

import com.coldmail.model.EmailLog;
import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.service.ResendEmailService;
import com.coldmail.service.EmailTemplateService;
import com.coldmail.service.RecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final EmailTemplateService templateService;
    private final RecipientService recipientService;
    private final ResendEmailService emailService;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Recipient> allRecipients = recipientService.getAllRecipients();
        List<EmailLog> allLogs = emailService.getAllEmailLogs();

        // Calculate stats
        Map<RecipientStatus, Long> statusCounts = allRecipients.stream()
                .collect(Collectors.groupingBy(Recipient::getStatus, Collectors.counting()));

        long totalRecipients = allRecipients.size();
        long pendingCount = statusCounts.getOrDefault(RecipientStatus.PENDING, 0L);
        long sentCount = statusCounts.getOrDefault(RecipientStatus.SENT, 0L);
        long repliedCount = statusCounts.getOrDefault(RecipientStatus.REPLIED, 0L);
        long interviewCount = statusCounts.getOrDefault(RecipientStatus.INTERVIEW, 0L);
        long failedCount = statusCounts.getOrDefault(RecipientStatus.FAILED, 0L);

        model.addAttribute("totalRecipients", totalRecipients);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("repliedCount", repliedCount);
        model.addAttribute("interviewCount", interviewCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("totalEmails", allLogs.size());
        model.addAttribute("templateCount", templateService.getAllTemplates().size());

        // Recent activity
        model.addAttribute("recentLogs", allLogs.stream()
                .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                .limit(5)
                .toList());

        return "dashboard";
    }

    @GetMapping("/templates")
    public String templates(Model model) {
        model.addAttribute("templates", templateService.getAllTemplates());
        return "templates";
    }

    @GetMapping("/recipients")
    public String recipients(Model model) {
        model.addAttribute("recipients", recipientService.getAllRecipients());
        model.addAttribute("statuses", RecipientStatus.values());
        return "recipients";
    }

    @GetMapping("/send")
    public String sendEmails(Model model) {
        model.addAttribute("templates", templateService.getAllTemplates());
        model.addAttribute("recipients", recipientService.getRecipientsByStatus(RecipientStatus.PENDING));
        model.addAttribute("allRecipients", recipientService.getAllRecipients());
        return "send";
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("logs", emailService.getAllEmailLogs().stream()
                .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                .toList());
        return "logs";
    }
}