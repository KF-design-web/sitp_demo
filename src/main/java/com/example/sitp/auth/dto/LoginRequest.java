package com.example.sitp.auth.dto;

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
    private String username;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase();
    }

    @NotBlank
    private String password;

    @NotBlank
    private String captchaId;

    @NotBlank
    private String captchaAnswer;
}
