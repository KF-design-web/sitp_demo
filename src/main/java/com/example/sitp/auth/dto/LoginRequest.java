package com.example.sitp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }

    @NotBlank
    private String password;

    @NotBlank
    private String captchaId;

    @NotBlank
    private String captchaAnswer;
}
