package com.coldmail.controller;

import com.coldmail.dto.ApiResponse;
import com.coldmail.dto.RecipientRequest;
import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.service.RecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final RecipientService recipientService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Recipient>>> getAllRecipients(
            @RequestParam(required = false) RecipientStatus status,
            @RequestParam(required = false) String search) {
        List<Recipient> recipients = recipientService.searchRecipients(search, status);
        return ResponseEntity.ok(ApiResponse.success(recipients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipient>> getRecipient(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recipientService.getRecipientById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Recipient>> createRecipient(
            @Valid @RequestBody RecipientRequest request) {
        Recipient recipient = recipientService.createRecipient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recipient created successfully", recipient));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipient>> updateRecipient(
            @PathVariable Long id,
            @Valid @RequestBody RecipientRequest request) {
        Recipient recipient = recipientService.updateRecipient(id, request);
        return ResponseEntity.ok(ApiResponse.success("Recipient updated successfully", recipient));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Recipient>> updateStatus(
            @PathVariable Long id,
            @RequestParam RecipientStatus status) {
        Recipient recipient = recipientService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", recipient));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipient(@PathVariable Long id) {
        recipientService.deleteRecipient(id);
        return ResponseEntity.ok(ApiResponse.success("Recipient deleted successfully", null));
    }

    // NEW: Bulk delete
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkDelete(
            @RequestBody List<Long> ids) {
        int deleted = recipientService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(
                deleted + " recipients deleted",
                Map.of("deleted", deleted)
        ));
    }

    // NEW: Bulk update status
    @PatchMapping("/bulk/status")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkUpdateStatus(
            @RequestBody List<Long> ids,
            @RequestParam RecipientStatus status) {
        int updated = recipientService.bulkUpdateStatus(ids, status);
        return ResponseEntity.ok(ApiResponse.success(
                updated + " recipients updated",
                Map.of("updated", updated)
        ));
    }
}