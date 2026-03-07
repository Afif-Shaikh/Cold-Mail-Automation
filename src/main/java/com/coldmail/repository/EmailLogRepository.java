package com.coldmail.repository;

import com.coldmail.model.EmailLog;
import com.coldmail.model.EmailLog.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientId(Long recipientId);
    List<EmailLog> findByStatus(EmailStatus status);
}