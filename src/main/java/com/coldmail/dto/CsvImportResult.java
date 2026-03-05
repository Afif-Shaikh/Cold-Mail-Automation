package com.coldmail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportResult {

    private int totalRows;
    private int successCount;
    private int failedCount;
    private int skippedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> skipped = new ArrayList<>();

    public void addError(int row, String message) {
        errors.add("Row " + row + ": " + message);
    }

    public void addSkipped(int row, String email, String reason) {
        skipped.add("Row " + row + " (" + email + "): " + reason);
    }
}