package com.example.sitp.payment.dto;

import com.example.sitp.payment.model.PaymentProof;
import com.example.sitp.payment.model.PaymentProofStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentProofResponse {

    private Long id;
    private Long uploaderId;
    private String uploaderName;
    private String uploaderUsername;
    private Long courseId;
    private String courseTitle;
    private String paymentMethod;
    private BigDecimal amount;
    private String fileUrl;
    private PaymentProofStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public static PaymentProofResponse fromEntity(PaymentProof proof) {
        return PaymentProofResponse.builder()
                .id(proof.getId())
                .uploaderId(proof.getUploader().getId())
                .uploaderName(proof.getUploader().getFirstName() + " " + proof.getUploader().getLastName())
                .uploaderUsername(proof.getUploader().getUsername())
                .courseId(proof.getCourse().getId())
                .courseTitle(proof.getCourse().getTitle())
                .paymentMethod(proof.getPaymentMethod())
                .amount(proof.getAmount())
                .fileUrl(proof.getFileUrl())
                .status(proof.getStatus())
                .createdAt(proof.getCreatedAt())
                .decidedAt(proof.getDecidedAt())
                .build();
    }
}
