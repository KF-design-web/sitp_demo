package com.example.sitp.auth.service;

import com.example.sitp.access.model.Session;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaService;
import com.example.sitp.auth.exception.AccountDisabledException;
import com.example.sitp.auth.exception.EmailAlreadyExistsException;
import com.example.sitp.auth.exception.InvalidCredentialsException;
import com.example.sitp.auth.exception.PasswordMismatchException;
import com.example.sitp.auth.exception.UsernameAlreadyExistsException;
import com.example.sitp.auth.dto.AuthResponse;
import com.example.sitp.auth.dto.LoginRequest;
import com.example.sitp.auth.dto.RegisterRequest;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import com.example.sitp.user.dto.UserResponse;
import com.example.sitp.verification.VerificationService;
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
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    static final Duration SESSION_DURATION = Duration.ofDays(7);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final VerificationService verificationService;

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder,
                       CaptchaService captchaService,
                       VerificationService verificationService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.verificationService = verificationService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer());

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }


        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new PasswordMismatchException();
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(trimOrNull(request.getPhoneNumber()))
                .address(trimOrNull(request.getAddress()))
                .country(trimOrNull(request.getCountry()))
                .gender(request.getGender())
                .role(Role.TRAINEE)
                .accountType(AccountType.OUTSIDER)
                .enabled(false)
                .build();

        try {
            User saved = userRepository.save(user);
            verificationService.issueEmailVerification(saved);
            return UserResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException();
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer());

        User user = userRepository.findByUsername(request.getUsername())
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

    private static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
