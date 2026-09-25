package com.example.sitp.auth.dto;

import com.example.sitp.user.dto.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuthResponse {

    private final UserResponse user;
    private final String token;
    private final LocalDateTime expiresAt;
}
