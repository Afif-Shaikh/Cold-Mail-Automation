package com.coldmail.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${email.api-key:NOT_SET}")
    private String apiKey;

    @Value("${email.from-email:NOT_SET}")
    private String fromEmail;

    @Value("${email.provider:NOT_SET}")
    private String provider;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/debug/email-config")
    public ResponseEntity<Map<String, String>> debugEmailConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("provider", provider);
        config.put("from-email", fromEmail);
        config.put("api-key-set", apiKey != null && !apiKey.equals("NOT_SET") && apiKey.length() > 10 ? "YES (length: " + apiKey.length() + ")" : "NO");
        config.put("api-key-preview", apiKey != null && apiKey.length() > 15 ? apiKey.substring(0, 15) + "..." : "TOO SHORT OR NOT SET");
        return ResponseEntity.ok(config);
    }
}