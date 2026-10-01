package com.example.sitp.user.dto;

import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Gender;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String address;
    private final String country;
    private final Gender gender;
    private final Role role;
    private final AccountType accountType;
    private final boolean enabled;
    private final LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .country(user.getCountry())
                .gender(user.getGender())
                .role(user.getRole())
                .accountType(user.getAccountType())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
