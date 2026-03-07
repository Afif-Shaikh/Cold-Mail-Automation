package com.coldmail.repository;

import com.coldmail.model.Recipient;
import com.coldmail.model.Recipient.RecipientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    List<Recipient> findByStatus(RecipientStatus status);

    boolean existsByEmail(String email);

    // Search by name, email, or company
    @Query("SELECT r FROM Recipient r WHERE " +
            "LOWER(r.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.company) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Recipient> search(@Param("search") String search);

    // Search with status filter
    @Query("SELECT r FROM Recipient r WHERE " +
            "r.status = :status AND (" +
            "LOWER(r.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Recipient> searchByStatus(@Param("search") String search, @Param("status") RecipientStatus status);

    // Find all by IDs
    List<Recipient> findAllByIdIn(List<Long> ids);
}