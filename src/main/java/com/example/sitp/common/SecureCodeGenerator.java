package com.example.sitp.common;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureCodeGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate(String courseTitle) {
        String label = deriveLabel(courseTitle);
        StringBuilder sb = new StringBuilder(label).append('-');
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private String deriveLabel(String courseTitle) {
        if (courseTitle == null || courseTitle.isBlank()) {
            return "SITP";
        }
        String cleaned = courseTitle.replaceAll("[^A-Za-z]", "");
        if (cleaned.length() < 3) {
            return "SITP";
        }
        return cleaned.substring(0, 3).toUpperCase();
    }
}
