package com.example.sitp.verification.controller;

import com.example.sitp.verification.VerificationService;
import com.example.sitp.verification.dto.ForgotPasswordRequest;
import com.example.sitp.verification.dto.ResetPasswordRequest;
import com.example.sitp.verification.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping(value = "/verify-email", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> verifyEmail(@Valid @ModelAttribute VerifyEmailRequest request) {
        verificationService.verifyEmail(request.getUsername(), request.getCode());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> forgotPassword(@Valid @ModelAttribute ForgotPasswordRequest request) {
        verificationService.issuePasswordReset(request.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @ModelAttribute ResetPasswordRequest request) {
        verificationService.resetPassword(request.getUsername(), request.getCode(),
                request.getNewPassword(), request.getNewPasswordConfirm());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
