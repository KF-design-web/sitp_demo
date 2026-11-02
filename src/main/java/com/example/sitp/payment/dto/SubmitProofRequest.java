package com.example.sitp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(description = "Form fields an outsider posts to prove they paid for one course offline.")
public class SubmitProofRequest {

    @Schema(description = "The paid course.")
    @NotNull(message = "Course is required.")
    private Long courseId;

    @Schema(description = "The amount paid, in XAF.")
    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
    private BigDecimal amount;

    @Schema(description = "How the payment moved (e.g. \"MTN Mobile Money\", \"Orange Money\", \"bank transfer\").")
    @NotBlank(message = "Payment method is required.")
    private String paymentMethod;

    @Schema(description = "The receipt file (a screenshot or scan of the proof of payment).")
    @NotNull(message = "The receipt file is required.")
    private MultipartFile receipt;

}
