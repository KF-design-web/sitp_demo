package com.example.sitp.auth.dto;

import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Track;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    public void setEmail(String email) {

        this.email = email == null ? null : email.trim();
    }

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotNull
    private AccountType accountType;

    private Track track;
}
