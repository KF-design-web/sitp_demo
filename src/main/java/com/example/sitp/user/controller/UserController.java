package com.example.sitp.user.controller;

import com.example.sitp.user.dto.UpdateProfileRequest;
import com.example.sitp.user.dto.UserResponse;
import com.example.sitp.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/me", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @ModelAttribute UpdateProfileRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserResponse updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(updated);
    }
}
