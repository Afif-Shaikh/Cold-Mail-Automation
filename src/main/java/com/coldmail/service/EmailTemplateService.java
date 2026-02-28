package com.coldmail.service;

import com.coldmail.dto.EmailTemplateRequest;
import com.coldmail.model.EmailTemplate;
import com.coldmail.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    public List<EmailTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public EmailTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
    }

    public EmailTemplate createTemplate(EmailTemplateRequest request) {
        EmailTemplate template = EmailTemplate.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .body(request.getBody())
                .description(request.getDescription())
                .build();
        return templateRepository.save(template);
    }

    public EmailTemplate updateTemplate(Long id, EmailTemplateRequest request) {
        EmailTemplate template = getTemplateById(id);
        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setDescription(request.getDescription());
        return templateRepository.save(template);
    }

    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }
}