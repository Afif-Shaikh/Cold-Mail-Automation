package com.coldmail.controller;

import com.coldmail.dto.ApiResponse;
import com.coldmail.model.Resume;
import com.coldmail.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Resume>> uploadResume(
            @RequestParam("file") MultipartFile file) {
        try {
            Resume resume = resumeService.uploadResume(file);
            return ResponseEntity.ok(ApiResponse.success("Resume uploaded successfully", resume));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload resume"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Resume>>> getAllResumes() {
        List<Resume> resumes = resumeService.getAllResumes();
        // Don't include file data in list response
        resumes.forEach(r -> r.setData(null));
        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadResume(@PathVariable Long id) {
        return resumeService.getResumeById(id)
                .map(resume -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(resume.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resume.getOriginalFileName() + "\"")
                        .body(resume.getData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultResume(@PathVariable Long id) {
        resumeService.setDefaultResume(id);
        return ResponseEntity.ok(ApiResponse.success("Default resume updated", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(@PathVariable Long id) {
        resumeService.deleteResume(id);
        return ResponseEntity.ok(ApiResponse.success("Resume deleted", null));
    }
}