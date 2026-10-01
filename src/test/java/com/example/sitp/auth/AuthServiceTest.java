package com.example.sitp.auth;

import com.example.sitp.access.model.Session;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallenge;
import com.example.sitp.auth.captcha.CaptchaService;
import com.example.sitp.auth.exception.EmailAlreadyExistsException;
import com.example.sitp.auth.exception.InvalidCredentialsException;
import com.example.sitp.auth.exception.UsernameAlreadyExistsException;
import com.example.sitp.auth.service.AuthService;
import com.example.sitp.auth.dto.LoginRequest;
import com.example.sitp.auth.dto.RegisterRequest;
import com.example.sitp.auth.dto.AuthResponse;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Gender;
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

    @Autowired
    private CaptchaService captchaService;

    private RegisterRequest registerRequest(String email) {
        String username = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-z0-9._-]", "");
        CaptchaChallenge captcha = captchaService.issue();
        return RegisterRequest.builder()
                .email(email)
                .username(username)
                .firstName("Ana")
                .lastName("Example")
                .gender(Gender.FEMALE)
                .password("password123")
                .passwordConfirm("password123")
                .captchaId(String.valueOf(captcha.getId()))
                .captchaAnswer(answerOf(captcha))
                .build();
    }

    private static String answerOf(CaptchaChallenge captcha) {
        String[] parts = captcha.getQuestion().split(" ");
        int sum = Integer.parseInt(parts[2]) + Integer.parseInt(parts[4].replace("?", ""));
        return String.valueOf(sum);
    }

    private void deleteAllRows() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_createsATrainee_DISABLED_withHashedPassword_andAProfile() {
        deleteAllRows();

        UserResponse created = authService.register(registerRequest("ana@mail.com"));

        assertThat(created.getRole()).isEqualTo(Role.TRAINEE);
        assertThat(created.isEnabled()).isFalse();
        assertThat(created.getUsername()).isEqualTo("ana");
        assertThat(created.getFirstName()).isEqualTo("Ana");
        assertThat(created.getGender()).isEqualTo(Gender.FEMALE);

        User stored = userRepository.findByEmail("ana@mail.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("password123");
        assertThat(authServiceMatches("password123", stored.getPasswordHash())).isTrue();
    }

    @Test
    void register_storesEmailAndUsernameLowercase_trimmed() {
        deleteAllRows();

        RegisterRequest messy = registerRequest("  Ana@Mail.COM ");
        messy.setUsername("  Ana ");
        authService.register(messy);

        assertThat(userRepository.existsByEmail("ana@mail.com")).isTrue();
        assertThat(userRepository.existsByUsername("ana")).isTrue();
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExists() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        assertThatThrownBy(() -> authService.register(registerRequest("ana@mail.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void register_duplicateUsername_throwsUsernameAlreadyExists() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        RegisterRequest other = registerRequest("other@mail.com");
        other.setUsername("ana");

        assertThatThrownBy(() -> authService.register(other))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void register_passwordConfirmMismatch_throws() {
        deleteAllRows();
        RegisterRequest sloppy = registerRequest("ana@mail.com");
        sloppy.setPasswordConfirm("different123");

        assertThatThrownBy(() -> authService.register(sloppy))
                .isInstanceOf(com.example.sitp.auth.exception.PasswordMismatchException.class);
        assertThat(userRepository.existsByEmail("ana@mail.com")).isFalse();
    }

    @Test
    void register_withAConsumedCaptcha_throwsBeforeAnythingElse() {
        deleteAllRows();
        RegisterRequest first = registerRequest("ana@mail.com");
        authService.register(first);

        RegisterRequest second = registerRequest("bob@mail.com");
        second.setCaptchaId(first.getCaptchaId());
        second.setCaptchaAnswer(first.getCaptchaAnswer());

        assertThatThrownBy(() -> authService.register(second))
                .isInstanceOf(com.example.sitp.auth.exception.InvalidCaptchaException.class);
        assertThat(userRepository.existsByEmail("bob@mail.com")).isFalse();
    }

    @Test
    void register_isTypeBlind_everyNewbornIsAnOutsider() {
        deleteAllRows();
        UserResponse created = authService.register(registerRequest("blind@mail.com"));

        assertThat(created.getAccountType()).isEqualTo(AccountType.OUTSIDER);
        assertThat(created.getRole()).isEqualTo(Role.TRAINEE);
    }

    @Test
    void register_mixedCaseUsername_acceptedAndStoredLowercase() {
        deleteAllRows();
        RegisterRequest request = registerRequest("mixed@mail.com");
        request.setUsername("Naira.Saint-2026");

        UserResponse created = authService.register(request);

        assertThat(created.getUsername()).isEqualTo("naira.saint-2026");
        assertThat(userRepository.findByUsername("naira.saint-2026")).isPresent();
        assertThat(userRepository.findByUsername("Naira.Saint-2026")).isEmpty();
    }

    @Test
    void login_byUsername_afterVerificationSurgery() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));
        enable("ana");

        AuthResponse auth = login("ana");

        assertThat(auth.getUser().getUsername()).isEqualTo("ana");
        assertThat(auth.getUser().getEmail()).isEqualTo("ana@mail.com");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));
        enable("ana");

        LoginRequest wrong = loginRequest("ana", "totally-wrong");

        assertThatThrownBy(() -> authService.login(wrong))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownUsername_throwsTheSAMEExceptionAsWrongPassword() {
        deleteAllRows();
        LoginRequest stranger = loginRequest("ghost", "whatever");

        assertThatThrownBy(() -> authService.login(stranger))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_disabledAccount_stillForbidden() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));

        assertThatThrownBy(() -> authService.login(loginRequest("ana", "password123")))
                .isInstanceOf(com.example.sitp.auth.exception.AccountDisabledException.class);
    }

    @Test
    void login_success_leavesExactlyOneActiveSession() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));
        enable("ana");

        AuthResponse first = login("ana");

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
        enable("ana");

        AuthResponse first = login("ana");
        AuthResponse second = login("ana");

        assertThat(sessionRepository.findByTokenAndActiveTrue(first.getToken())).isEmpty();
        assertThat(sessionRepository.findByTokenAndActiveTrue(second.getToken())).isPresent();
        assertThat(second.getUser().getUsername()).isEqualTo("ana");
        assertThat(second.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void logout_crossesOutTheRow_butKeepsTheHistory() {
        deleteAllRows();
        authService.register(registerRequest("ana@mail.com"));
        enable("ana");
        AuthResponse auth = login("ana");

        authService.logout(auth.getToken());

        assertThat(sessionRepository.findByTokenAndActiveTrue(auth.getToken())).isEmpty();
        boolean rowStillExists = sessionRepository.findAll().stream()
                .anyMatch(s -> s.getToken().equals(auth.getToken()));
        assertThat(rowStillExists).isTrue();
        authService.logout(auth.getToken());
    }


    private LoginRequest loginRequest(String username, String password) {
        CaptchaChallenge captcha = captchaService.issue();
        return LoginRequest.builder()
                .username(username)
                .password(password)
                .captchaId(String.valueOf(captcha.getId()))
                .captchaAnswer(answerOf(captcha))
                .build();
    }

    private AuthResponse login(String username) {
        return authService.login(loginRequest(username, "password123"));
    }

    private void enable(String username) {
        User u = userRepository.findByUsername(username).orElseThrow();
        u.setEnabled(true);
        userRepository.save(u);
    }

    private boolean authServiceMatches(String raw, String hash) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(raw, hash);
    }
}
