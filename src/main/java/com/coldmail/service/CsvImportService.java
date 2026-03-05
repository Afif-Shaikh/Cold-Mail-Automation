package com.coldmail.service;

import com.coldmail.dto.CsvImportResult;
import com.coldmail.model.Recipient;
import com.coldmail.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final RecipientRepository recipientRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    /**
     * Import recipients from CSV file
     * Expected format: email,name,company,position,notes
     * First row should be header (will be skipped)
     */
    public CsvImportResult importCsv(MultipartFile file) {
        CsvImportResult result = CsvImportResult.builder()
                .totalRows(0)
                .successCount(0)
                .failedCount(0)
                .skippedCount(0)
                .build();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int rowNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Skip header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);

                try {
                    processRow(line, rowNumber, result);
                } catch (Exception e) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.addError(rowNumber, e.getMessage());
                    log.error("Error processing row {}: {}", rowNumber, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error reading CSV file: {}", e.getMessage());
            result.addError(0, "Failed to read file: " + e.getMessage());
        }

        return result;
    }

    private void processRow(String line, int rowNumber, CsvImportResult result) {
        // Parse CSV line (handles quoted values)
        String[] columns = parseCsvLine(line);

        if (columns.length < 1) {
            result.setFailedCount(result.getFailedCount() + 1);
            result.addError(rowNumber, "Empty row");
            return;
        }

        String email = columns[0].trim();
        String name = columns.length > 1 ? columns[1].trim() : "";
        String company = columns.length > 2 ? columns[2].trim() : "";
        String position = columns.length > 3 ? columns[3].trim() : "";
        String notes = columns.length > 4 ? columns[4].trim() : "";

        // Validate email
        if (email.isEmpty()) {
            result.setFailedCount(result.getFailedCount() + 1);
            result.addError(rowNumber, "Email is required");
            return;
        }

        if (!isValidEmail(email)) {
            result.setFailedCount(result.getFailedCount() + 1);
            result.addError(rowNumber, "Invalid email format: " + email);
            return;
        }

        // Check for duplicate
        if (recipientRepository.existsByEmail(email)) {
            result.setSkippedCount(result.getSkippedCount() + 1);
            result.addSkipped(rowNumber, email, "Already exists");
            return;
        }

        // Create recipient
        Recipient recipient = Recipient.builder()
                .email(email)
                .name(name.isEmpty() ? null : name)
                .company(company.isEmpty() ? null : company)
                .position(position.isEmpty() ? null : position)
                .notes(notes.isEmpty() ? null : notes)
                .status(Recipient.RecipientStatus.PENDING)
                .build();

        recipientRepository.save(recipient);
        result.setSuccessCount(result.getSuccessCount() + 1);
        log.info("Imported recipient: {}", email);
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV parser that handles quoted values
        java.util.List<String> values = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}