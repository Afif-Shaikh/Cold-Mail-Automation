package com.coldmail.service;

import com.coldmail.model.Resume;
import com.coldmail.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public Resume uploadResume(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PDF and Word documents are allowed");
        }

        // Create unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".pdf";
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // If this is the first resume, make it default
        boolean isDefault = resumeRepository.count() == 0;

        Resume resume = Resume.builder()
                .fileName(uniqueFilename)
                .originalFileName(originalFilename)
                .contentType(contentType)
                .fileSize(file.getSize())
                .data(file.getBytes())
                .isDefault(isDefault)
                .build();

        Resume saved = resumeRepository.save(resume);
        log.info("Resume uploaded: {} ({})", originalFilename, uniqueFilename);

        return saved;
    }

    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    public Optional<Resume> getResumeById(Long id) {
        return resumeRepository.findById(id);
    }

    public Optional<Resume> getDefaultResume() {
        return resumeRepository.findByIsDefaultTrue();
    }

    public void setDefaultResume(Long id) {
        // Remove current default
        resumeRepository.findByIsDefaultTrue().ifPresent(resume -> {
            resume.setDefault(false);
            resumeRepository.save(resume);
        });

        // Set new default
        resumeRepository.findById(id).ifPresent(resume -> {
            resume.setDefault(true);
            resumeRepository.save(resume);
        });
    }

    public void deleteResume(Long id) {
        resumeRepository.deleteById(id);
    }
}