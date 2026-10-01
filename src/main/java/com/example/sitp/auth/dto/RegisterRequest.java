package com.example.sitp.auth.dto;

import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Track;
import jakarta.validation.constraints.Email;
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
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    public void setEmail(String email) {

        this.email = email == null ? null : email.trim();
    }

    @NotBlank
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may only contain letters, digits, dot, underscore, hyphen (capitals welcome — stored lowercase)")
    private String username;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    private String phoneNumber;

    private String address;

    private String country;

    private com.example.sitp.user.model.Gender gender;


    @NotBlank
    private String captchaId;

    @NotBlank
    private String captchaAnswer;
}
