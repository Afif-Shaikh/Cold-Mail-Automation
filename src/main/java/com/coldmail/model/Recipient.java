package com.coldmail.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recipients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String name;

    private String company;

    private String position; // Position you're applying for

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientStatus status;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = RecipientStatus.PENDING;
        }
    }

    public enum RecipientStatus {
        PENDING,    // Not yet emailed
        SENT,       // Email sent
        FAILED,     // Email failed
        REPLIED,    // Got a response
        REJECTED,   // Got rejected
        INTERVIEW   // Got interview call
    }
}