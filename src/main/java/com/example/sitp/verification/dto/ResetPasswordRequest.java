package com.example.sitp.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank
    private String username;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase();
    }

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "must be the 6-digit code from the email")
    private String code;

    @NotBlank
    @Size(min = 8)
    private String newPassword;

    @NotBlank
    private String newPasswordConfirm;
}
