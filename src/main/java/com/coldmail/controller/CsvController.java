package com.coldmail.controller;

import com.coldmail.dto.ApiResponse;
import com.coldmail.dto.CsvImportResult;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.service.CsvExportService;
import com.coldmail.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/csv")
@RequiredArgsConstructor
public class CsvController {

    private final CsvImportService csvImportService;
    private final CsvExportService csvExportService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<CsvImportResult>> importCsv(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please select a file to upload"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only CSV files are allowed"));
        }

        CsvImportResult result = csvImportService.importCsv(file);

        String message = String.format(
                "Import complete: %d success, %d failed, %d skipped",
                result.getSuccessCount(),
                result.getFailedCount(),
                result.getSkippedCount()
        );

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) RecipientStatus status) {

        byte[] csvData = csvExportService.exportRecipients(status);

        String filename = "recipients_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }
}