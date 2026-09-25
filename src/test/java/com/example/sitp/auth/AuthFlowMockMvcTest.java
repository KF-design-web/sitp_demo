package com.example.sitp.auth;

import com.example.sitp.access.repository.SessionRepository;
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

    private void cleanTables() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerThenLoginThenLogout_fullWalkthrough() throws Exception {
        cleanTables();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("password", "password123")
                        .param("accountType", "OUTSIDER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.role").value("TRAINEE"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("password", "password123")
                        .param("accountType", "OUTSIDER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This email is already registered."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "  Ana@Mail.COM ")
                        .param("password", "password123")
                        .param("accountType", "OUTSIDER"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "not-an-email")
                        .param("password", "123")
                        .param("accountType", "OUTSIDER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        String cookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
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


    private void registerAna() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("password", "password123")
                        .param("accountType", "OUTSIDER"))
                .andExpect(status().isCreated());
    }

    private jakarta.servlet.http.Cookie loginAndGrabCookie() throws Exception {
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        String[] nameAndValue = setCookie.split(";", 2)[0].split("=", 2);
        return new jakarta.servlet.http.Cookie(nameAndValue[0], nameAndValue[1]);
    }

    @Test
    void me_withValidCookie_returnsTheIdCard_withoutAnyHashLine() throws Exception {
        cleanTables();
        registerAna();
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
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
        jakarta.servlet.http.Cookie oldCookie = loginAndGrabCookie();
        jakarta.servlet.http.Cookie newCookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/auth/me").cookie(oldCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").cookie(newCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@mail.com"));
    }
}
