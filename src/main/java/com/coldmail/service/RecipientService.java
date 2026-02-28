package com.coldmail.service;

import com.coldmail.dto.RecipientRequest;
import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import com.coldmail.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;

    public List<Recipient> getAllRecipients() {
        return recipientRepository.findAll();
    }

    public List<Recipient> getRecipientsByStatus(RecipientStatus status) {
        return recipientRepository.findByStatus(status);
    }

    public Recipient getRecipientById(Long id) {
        return recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found with id: " + id));
    }

    public Recipient createRecipient(RecipientRequest request) {
        if (recipientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Recipient with email already exists: " + request.getEmail());
        }

        Recipient recipient = Recipient.builder()
                .email(request.getEmail())
                .name(request.getName())
                .company(request.getCompany())
                .position(request.getPosition())
                .notes(request.getNotes())
                .status(RecipientStatus.PENDING)
                .build();
        return recipientRepository.save(recipient);
    }

    public Recipient updateRecipient(Long id, RecipientRequest request) {
        Recipient recipient = getRecipientById(id);
        recipient.setEmail(request.getEmail());
        recipient.setName(request.getName());
        recipient.setCompany(request.getCompany());
        recipient.setPosition(request.getPosition());
        recipient.setNotes(request.getNotes());
        return recipientRepository.save(recipient);
    }

    public Recipient updateStatus(Long id, RecipientStatus status) {
        Recipient recipient = getRecipientById(id);
        recipient.setStatus(status);
        return recipientRepository.save(recipient);
    }

    public void deleteRecipient(Long id) {
        recipientRepository.deleteById(id);
    }
}