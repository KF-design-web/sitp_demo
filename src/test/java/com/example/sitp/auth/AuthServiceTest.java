package com.example.sitp.auth;

import com.example.sitp.access.model.Session;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.exception.EmailAlreadyExistsException;
import com.example.sitp.auth.exception.InvalidCredentialsException;
import com.example.sitp.auth.service.AuthService;
import com.example.sitp.auth.dto.LoginRequest;
import com.example.sitp.auth.dto.RegisterRequest;
import com.example.sitp.auth.dto.AuthResponse;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import com.example.sitp.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private RegisterRequest registerRequest(String email) {
        return RegisterRequest.builder()
                .email(email)
                .password("password123")
                .accountType(AccountType.OUTSIDER)
                .build();
    }

    private void deleteAllRows() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_createsATraineeEnabledWithHashedPassword() {
        deleteAllRows();

        UserResponse created = authService.register(registerRequest("ana@mail.com"));

        assertThat(created.getRole()).isEqualTo(Role.TRAINEE);
        assertThat(created.isEnabled()).isTrue();

        User stored = userRepository.findByEmail("ana@mail.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("password123");
        assertThat(authServiceMatches("password123", stored.getPasswordHash())).isTrue();
    }

    @Test
    void register_storesEmailLowercase_trimmed() {
        deleteAllRows();

        RegisterRequest messy = registerRequest("  Ana@Mail.COM ");
        authService.register(messy);

        assertThat(userRepository.existsByEmail("ana@mail.com")).isTrue();
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExists() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        assertThatThrownBy(() -> authService.register(registerRequest("ana@mail.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void register_internWithoutTrack_isRejected() {
        deleteAllRows();
        RegisterRequest intern = RegisterRequest.builder()
                .email("intern@mail.com")
                .password("password123")
                .accountType(AccountType.INTERN)
                .track(null)
                .build();

        assertThatThrownBy(() -> authService.register(intern))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void register_internWithTrack_isAccepted() {
        deleteAllRows();
        RegisterRequest intern = RegisterRequest.builder()
                .email("intern2@mail.com")
                .password("password123")
                .accountType(AccountType.INTERN)
                .track(com.example.sitp.user.model.Track.ACADEMIC)
                .build();

        UserResponse created = authService.register(intern);

        assertThat(created.getAccountType()).isEqualTo(AccountType.INTERN);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        LoginRequest wrong = LoginRequest.builder()
                .email("ana@mail.com")
                .password("totally-wrong")
                .build();

        assertThatThrownBy(() -> authService.login(wrong))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsTheSAMEExceptionAsWrongPassword() {
        deleteAllRows();
        LoginRequest stranger = LoginRequest.builder()
                .email("ghost@mail.com")
                .password("whatever")
                .build();

        assertThatThrownBy(() -> authService.login(stranger))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_success_leavesExactlyOneActiveSession() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        AuthResponse first = authService.login(LoginRequest.builder()
                .email("ana@mail.com").password("password123").build());

        var active = sessionRepository.findByTokenAndActiveTrue(first.getToken());
        assertThat(active).isPresent();
        long activeCount = sessionRepository.findAll().stream()
                .filter(Session::isActive)
                .count();
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    void login_twice_theFirstTokenDies_theSecondWorks() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        AuthResponse first = authService.login(LoginRequest.builder()
                .email("ana@mail.com").password("password123").build());
        AuthResponse second = authService.login(LoginRequest.builder()
                .email("ana@mail.com").password("password123").build());

        assertThat(sessionRepository.findByTokenAndActiveTrue(first.getToken())).isEmpty();
        assertThat(sessionRepository.findByTokenAndActiveTrue(second.getToken())).isPresent();
        assertThat(second.getUser().getEmail()).isEqualTo("ana@mail.com");
        assertThat(second.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void logout_crossesOutTheRow_butKeepsTheHistory() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));
        AuthResponse auth = authService.login(LoginRequest.builder()
                .email("ana@mail.com").password("password123").build());

        authService.logout(auth.getToken());

        assertThat(sessionRepository.findByTokenAndActiveTrue(auth.getToken())).isEmpty();
        boolean rowStillExists = sessionRepository.findAll().stream()
                .anyMatch(s -> s.getToken().equals(auth.getToken()));
        assertThat(rowStillExists).isTrue();
        authService.logout(auth.getToken());
    }

    private boolean authServiceMatches(String raw, String hash) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(raw, hash);
    }
}
