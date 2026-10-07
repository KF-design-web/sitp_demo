package com.example.sitp.verification;

import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallenge;
import com.example.sitp.auth.captcha.CaptchaChallengeRepository;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VerificationFlowMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CaptchaChallengeRepository captchaChallengeRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private EmailSender emailSender;

    private final java.util.Deque<String> sentBodies = new java.util.concurrent.ConcurrentLinkedDeque<>();

    private void cleanTables() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        verificationCodeRepository.deleteAll();
        sentBodies.clear();
    }

    private String[] freshCaptcha() throws Exception {
        String body = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1").trim();
        CaptchaChallenge challenge = captchaChallengeRepository.findById(Long.parseLong(id)).orElseThrow();
        String[] parts = challenge.getQuestion().split(" ");
        String answer = String.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[2]));
        return new String[]{id, answer};
    }

    private void registerBob() throws Exception {
        String[] cap = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "bob@mail.com")
                        .param("username", "bob")
                        .param("firstName", "Bob")
                        .param("lastName", "Example")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isCreated());
    }

    private String lastCode() {
        String body = sentBodies.getLast();
        var m = java.util.regex.Pattern.compile("code is: (\\d{6})").matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private void mockitoSend(String to, String subject, String body) {
        sentBodies.addLast(body);
    }

    @Test
    void register_thenVerifyEmail_opensTheAccount() throws Exception {
        cleanTables();

        org.mockito.Mockito.doAnswer(inv -> {
            mockitoSend(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
            return null;
        }).when(emailSender).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        registerBob();

        String[] capA = freshCaptcha();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("password", "password123")
                        .param("captchaId", capA[0])
                        .param("captchaAnswer", capA[1]))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/verification/verify-email")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", "000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired code."));

        mockMvc.perform(post("/api/verification/verify-email")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", lastCode()))
                .andExpect(status().isNoContent());

        String[] capB = freshCaptcha();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("password", "password123")
                        .param("captchaId", capB[0])
                        .param("captchaAnswer", capB[1]))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/verification/verify-email")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", lastCode()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_thenResetPassword_changesTheHash_andKillsSessions() throws Exception {
        cleanTables();

        org.mockito.Mockito.doAnswer(inv -> {
            mockitoSend(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
            return null;
        }).when(emailSender).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        registerBob();
        mockMvc.perform(post("/api/verification/verify-email")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", lastCode()))
                .andExpect(status().isNoContent());

        String[] capC = freshCaptcha();
        String cookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("password", "password123")
                        .param("captchaId", capC[0])
                        .param("captchaAnswer", capC[1]))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Set-Cookie");
        String oldToken = cookie.split(";", 2)[0].split("=", 2)[1];

        String oldHash = userRepository.findByUsername("bob").orElseThrow().getPasswordHash();

        mockMvc.perform(post("/api/verification/forgot-password")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "bob@mail.com"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/verification/forgot-password")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ghost@mail.com"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/verification/reset-password")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", lastCode())
                        .param("newPassword", "newpass456")
                        .param("newPasswordConfirm", "newpass456"))
                .andExpect(status().isNoContent());

        String newHash = userRepository.findByUsername("bob").orElseThrow().getPasswordHash();
        org.assertj.core.api.Assertions.assertThat(newHash).isNotEqualTo(oldHash);
        String[] capD = freshCaptcha();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("password", "newpass456")
                        .param("captchaId", capD[0])
                        .param("captchaAnswer", capD[1]))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(
                sessionRepository.findByTokenAndActiveTrue(oldToken)).isEmpty();
    }

    @Test
    void resetPassword_disagreeingConfirmation_rejected() throws Exception {
        cleanTables();

        org.mockito.Mockito.doAnswer(inv -> {
            mockitoSend(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
            return null;
        }).when(emailSender).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        registerBob();
        mockMvc.perform(post("/api/verification/verify-email")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", lastCode()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/verification/forgot-password")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "bob@mail.com"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/verification/reset-password")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", lastCode())
                        .param("newPassword", "newpass456")
                        .param("newPasswordConfirm", "different789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match."));
    }

    @Test
    void expiredVerificationCode_rejected() throws Exception {
        cleanTables();

        org.mockito.Mockito.doAnswer(inv -> {
            mockitoSend(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
            return null;
        }).when(emailSender).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        registerBob();

        var row = verificationCodeRepository
                .findFirstByUsernameIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        "bob", VerificationPurpose.VERIFY_EMAIL)
                .orElseThrow();
        row.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        verificationCodeRepository.save(row);

        mockMvc.perform(post("/api/verification/verify-email")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "bob")
                        .param("code", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired code."));
    }
}
