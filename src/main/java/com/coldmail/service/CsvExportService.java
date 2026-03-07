package com.coldmail.service;

import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final RecipientRepository recipientRepository;

    public byte[] exportRecipients(RecipientStatus status) {
        List<Recipient> recipients;

        if (status != null) {
            recipients = recipientRepository.findByStatus(status);
        } else {
            recipients = recipientRepository.findAll();
        }

        return generateCsv(recipients);
    }

    private byte[] generateCsv(List<Recipient> recipients) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        // Header row
        writer.println("email,name,company,position,status,notes,created_at");

        // Data rows
        for (Recipient r : recipients) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(r.getEmail()),
                    escapeCsv(r.getName()),
                    escapeCsv(r.getCompany()),
                    escapeCsv(r.getPosition()),
                    r.getStatus(),
                    escapeCsv(r.getNotes()),
                    r.getCreatedAt()
            ));
        }

        writer.flush();
        return out.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Escape quotes and wrap in quotes if contains comma or quote
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}