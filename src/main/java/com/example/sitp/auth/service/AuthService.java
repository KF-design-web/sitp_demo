package com.example.sitp.auth.service;

import com.example.sitp.access.model.Session;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.exception.AccountDisabledException;
import com.example.sitp.auth.exception.EmailAlreadyExistsException;
import com.example.sitp.auth.exception.InvalidCredentialsException;
import com.example.sitp.auth.dto.AuthResponse;
import com.example.sitp.auth.dto.LoginRequest;
import com.example.sitp.auth.dto.RegisterRequest;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import com.example.sitp.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    static final Duration SESSION_DURATION = Duration.ofDays(7);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        if (request.getAccountType() == AccountType.INTERN && request.getTrack() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "INTERN accounts must provide a track (ACADEMIC or PROFESSIONAL).");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.TRAINEE)
                .accountType(request.getAccountType())
                .enabled(true)
                .build();

        try {
            return UserResponse.fromEntity(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException();
        }
    }


    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new AccountDisabledException();
        }

        sessionRepository.invalidateByUserId(user.getId());

        String token = generateToken();
        Session session = Session.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plus(SESSION_DURATION))
                .build();

        sessionRepository.save(session);

        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .token(token)
                .expiresAt(session.getExpiresAt())
                .build();
    }

    public void logout(String token) {

        if (token == null) {
            return;
        }
        sessionRepository.findByTokenAndActiveTrue(token)
                .ifPresent(session -> {
                    session.setActive(false);
                    sessionRepository.save(session);
                });
    }

    public Optional<User> resolveSessionUser(String token) {

        if (token == null) {
            return Optional.empty();
        }

        return sessionRepository.findByTokenAndActiveTrue(token)
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(Session::getUser);
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
