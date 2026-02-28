package com.coldmail.repository;

import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    List<Recipient> findByStatus(RecipientStatus status);
    boolean existsByEmail(String email);
}