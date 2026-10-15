package com.example.sitp.user.service;

import com.example.sitp.auth.exception.UsernameAlreadyExistsException;
import com.example.sitp.user.dto.UpdateProfileRequest;
import com.example.sitp.user.dto.UserResponse;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newUsername = request.getUsername().trim().toLowerCase();
        if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            throw new UsernameAlreadyExistsException();
        }

        user.setUsername(newUsername);
        if (request.getProfilePictureUrl() != null && !request.getProfilePictureUrl().isBlank()) {
            user.setProfilePictureUrl(request.getProfilePictureUrl().trim());
        } else {
            user.setProfilePictureUrl(null);
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }
}
