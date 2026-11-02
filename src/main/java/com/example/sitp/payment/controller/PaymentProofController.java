package com.example.sitp.payment.controller;

import com.example.sitp.common.NoAccessGrantedException;
import com.example.sitp.payment.dto.PaymentProofResponse;
import com.example.sitp.payment.dto.SubmitProofRequest;
import com.example.sitp.payment.exception.ProofNotFoundException;
import com.example.sitp.payment.model.PaymentProof;
import com.example.sitp.payment.repository.PaymentProofRepository;
import com.example.sitp.payment.service.PaymentProofService;
import com.example.sitp.payment.service.ReceiptFileStore;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/payment-proofs")
@Tag(name = "Payment proofs", description = "An outsider proves an offline payment; an Admin_Assistant verifies it.")
public class PaymentProofController {

    private final PaymentProofService paymentProofService;
    private final PaymentProofRepository proofRepository;
    private final ReceiptFileStore receiptFileStore;

    public PaymentProofController(PaymentProofService paymentProofService,
                                  PaymentProofRepository proofRepository,
                                  ReceiptFileStore receiptFileStore) {
        this.paymentProofService = paymentProofService;
        this.proofRepository = proofRepository;
        this.receiptFileStore = receiptFileStore;
    }

    @Operation(summary = "Submit a payment proof (OUTSIDER only).")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaymentProofResponse> submit(@Valid @ModelAttribute SubmitProofRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentProofService.submitProof(request, requireSubmitterId()));
    }

    @Operation(summary = "The verification queue (Admin_Assistant only).")
    @GetMapping("/pending")
    public List<PaymentProofResponse> pending() {
        requireAssistant();
        return paymentProofService.pendingQueue();
    }

    @Operation(summary = "Approve a proof (Admin_Assistant only).")
    @PostMapping("/{id}/approve")
    public PaymentProofResponse approve(@PathVariable Long id) {
        requireAssistant();
        return paymentProofService.approve(id);
    }

    @Operation(summary = "Reject a proof (Admin_Assistant only).")
    @PostMapping("/{id}/reject")
    public PaymentProofResponse reject(@PathVariable Long id) {
        requireAssistant();
        return paymentProofService.reject(id);
    }

    @Operation(summary = "Stream a stored receipt file (Admin_Assistant only).")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/octet-stream"))
    @GetMapping("/{id}/receipt")
    public ResponseEntity<Resource> receipt(@PathVariable Long id) {
        requireAssistant();
        PaymentProof proof = proofRepository.findById(id)
                .orElseThrow(ProofNotFoundException::new);
        Resource file = receiptFileStore.resolve(proof.getFileUrl());
        String contentType = receiptFileStore.probeContentType(proof.getFileUrl());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(file);
    }

    private Long requireSubmitterId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        return (Long) auth.getPrincipal();
    }

    private void requireAssistant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        boolean isAssistant = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_ASSISTANT"));
        if (!isAssistant) {
            throw new NoAccessGrantedException();
        }
    }
}
