package com.coldmail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class ColdMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColdMailApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = System.getenv("PORT");
        System.out.println("==========================================");
        System.out.println("Application started successfully!");
        System.out.println("PORT: " + (port != null ? port : "8080 (default)"));
        System.out.println("==========================================");
    }
}