package com.coldmail.controller;

import com.coldmail.dto.ApiResponse;
import com.coldmail.dto.EmailTemplateRequest;
import com.coldmail.model.EmailTemplate;
import com.coldmail.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAllTemplates()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplateById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailTemplate>> createTemplate(
            @Valid @RequestBody EmailTemplateRequest request) {
        EmailTemplate template = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created successfully", template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request) {
        EmailTemplate template = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
    }
}