package com.example.sitp.auth;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthFlowMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CaptchaChallengeRepository captchaChallengeRepository;

    private void cleanTables() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
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

    @Test
    void register_isTypeBlind_aHandSentInternClaim_cannotBuyTheCard() throws Exception {
        cleanTables();
        String[] cap = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "climber@mail.com")
                        .param("username", "climber")
                        .param("firstName", "Climber")
                        .param("lastName", "Test")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("accountType", "INTERN")
                        .param("track", "ACADEMIC")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType").value("OUTSIDER"))
                .andExpect(jsonPath("$.role").value("TRAINEE"));
    }

    private void registerAna() throws Exception {
        String[] cap = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("username", "ana")
                        .param("firstName", "Ana")
                        .param("lastName", "Example")
                        .param("gender", "FEMALE")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isCreated());
    }

    private void enableAnaDirectly() {
        var ana = userRepository.findByUsername("ana").orElseThrow();
        ana.setEnabled(true);
        userRepository.save(ana);
    }

    private jakarta.servlet.http.Cookie loginAndGrabCookie() throws Exception {
        String[] cap = freshCaptcha();
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "ana")
                        .param("password", "password123")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        String[] nameAndValue = setCookie.split(";", 2)[0].split("=", 2);
        return new jakarta.servlet.http.Cookie(nameAndValue[0], nameAndValue[1]);
    }

    @Test
    void registerThenLoginThenLogout_fullWalkthrough() throws Exception {
        cleanTables();

        String[] cap1 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("username", "ana")
                        .param("firstName", "Ana")
                        .param("lastName", "Example")
                        .param("gender", "FEMALE")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap1[0])
                        .param("captchaAnswer", cap1[1]))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.role").value("TRAINEE"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String[] cap2 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("username", "ana")
                        .param("firstName", "Ana")
                        .param("lastName", "Example")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap2[0])
                        .param("captchaAnswer", cap2[1]))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This email is already registered."));

        String[] cap3 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "  Ana@Mail.COM ")
                        .param("username", "ana")
                        .param("firstName", "Ana")
                        .param("lastName", "Example")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap3[0])
                        .param("captchaAnswer", cap3[1]))
                .andExpect(status().isConflict());

        String[] cap4 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "other@mail.com")
                        .param("username", "ana")
                        .param("firstName", "Other")
                        .param("lastName", "Person")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap4[0])
                        .param("captchaAnswer", cap4[1]))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This username is already taken."));

        String[] cap5 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "other@mail.com")
                        .param("username", "other")
                        .param("firstName", "Other")
                        .param("lastName", "Person")
                        .param("password", "password123")
                        .param("passwordConfirm", "different123")
                        .param("captchaId", cap5[0])
                        .param("captchaAnswer", cap5[1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match."));

        String[] cap6 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "other@mail.com")
                        .param("username", "other")
                        .param("firstName", "Other")
                        .param("lastName", "Person")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap6[0])
                        .param("captchaAnswer", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Captcha check failed. A new question has been issued."));

        String[] cap7 = freshCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "not-an-email")
                        .param("username", "x")
                        .param("firstName", "")
                        .param("password", "123")
                        .param("passwordConfirm", "123")
                        .param("captchaId", cap7[0])
                        .param("captchaAnswer", cap7[1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        String[] cap8 = freshCaptcha();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "ana")
                        .param("password", "password123")
                        .param("captchaId", cap8[0])
                        .param("captchaAnswer", cap8[1]))
                .andExpect(status().isForbidden());

        enableAnaDirectly();

        String[] cap9 = freshCaptcha();
        String cookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "ana")
                        .param("password", "password123")
                        .param("captchaId", cap9[0])
                        .param("captchaAnswer", cap9[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("ana"))
                .andExpect(jsonPath("$.user.email").value("ana@mail.com"))
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");

        org.assertj.core.api.Assertions.assertThat(cookie).isNotNull();
        org.assertj.core.api.Assertions.assertThat(cookie).contains("HttpOnly");

        String tokenPair = cookie.split(";", 2)[0];

        String[] nameAndValue = tokenPair.split("=", 2);
        jakarta.servlet.http.Cookie jarCookie =
                new jakarta.servlet.http.Cookie(nameAndValue[0], nameAndValue[1]);

        mockMvc.perform(post("/api/auth/logout").cookie(jarCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_withoutAnyTicket_isRejectedByTheRulebook_401() throws Exception {
        cleanTables();

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void register_withJsonBody_getsTheHonest415NotAFake500() throws Exception {
        cleanTables();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"x@mail.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }


    @Test
    void me_withValidCookie_returnsTheIdCard_withoutAnyHashLine() throws Exception {
        cleanTables();
        registerAna();
        enableAnaDirectly();
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.role").value("TRAINEE"))
                .andExpect(jsonPath("$.accountType").value("OUTSIDER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void me_withoutAnyCookie_theRulebookSpeaks_401() throws Exception {
        cleanTables();

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Not authenticated."));
    }

    @Test
    void me_withAForgedToken_isRejectedByTheRulebook_401() throws Exception {
        cleanTables();

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("sitp_session", "not-a-real-key")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_withAnExpiredSession_theDateCheckStillBites_401() throws Exception {
        cleanTables();
        registerAna();
        enableAnaDirectly();
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        var session = sessionRepository.findByTokenAndActiveTrue(cookie.getValue()).orElseThrow();
        session.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        sessionRepository.save(session);

        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void me_afterAKickout_theOldKeyAnswersLikeAStranger_401() throws Exception {
        cleanTables();
        registerAna();
        enableAnaDirectly();
        jakarta.servlet.http.Cookie oldCookie = loginAndGrabCookie();
        jakarta.servlet.http.Cookie newCookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/auth/me").cookie(oldCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").cookie(newCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@mail.com"));
    }
}
