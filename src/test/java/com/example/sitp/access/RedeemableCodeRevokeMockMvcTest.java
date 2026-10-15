package com.example.sitp.access;

import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallengeRepository;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RedeemableCodeRevokeMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedeemableCodeRepository codeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaptchaChallengeRepository captchaChallengeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void cleanTables() {
        codeRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        courseRepository.deleteAll();
    }

    private Course course() {
        return courseRepository.save(Course.builder()
                .title("Git Foundations")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());
    }

    private void seedUser(String username, Role role) {
        seedUser(username, role, AccountType.OUTSIDER);
    }

    private void seedUser(String username, Role role, AccountType accountType) {
        userRepository.save(com.example.sitp.user.model.User.builder()
                .email(username + "@mail.com")
                .username(username)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(role)
                .accountType(accountType)
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build());
    }

    private String[] freshCaptcha() throws Exception {
        String body = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\\\"id\\\":(\\d+).*", "$1").trim();
        var challenge = captchaChallengeRepository.findById(Long.parseLong(id)).orElseThrow();
        String[] parts = challenge.getQuestion().split(" ");
        String answer = String.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[2]));
        return new String[]{id, answer};
    }

    private jakarta.servlet.http.Cookie loginAndGrabCookie(String email) throws Exception {
        String[] cap = freshCaptcha();
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", email)
                        .param("password", "Password123!")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        String[] nameAndValue = setCookie.split(";", 2)[0].split("=", 2);
        return new jakarta.servlet.http.Cookie(nameAndValue[0], nameAndValue[1]);
    }

    private String mintOneCodeAsBoss(jakarta.servlet.http.Cookie bossCookie, Course course) throws Exception {
        String body = mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(course.getId()))
                        .param("count", "1")
                        .cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\\\"code\\\":\\\"([A-Z0-9-]+)\\\"").matcher(body);
        matcher.find();
        return matcher.group(1);
    }

    @Test
    void anonymousRevoke_earnsTheBuildingsLock_401() throws Exception {
        cleanTables();
        Course c = course();

        mockMvc.perform(post("/api/codes/GIT-TEST0000/revoke"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void traineeRevoke_earnsTheHonest403() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("plain.trainee", Role.TRAINEE);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("plain.trainee@mail.com");

        mockMvc.perform(post("/api/codes/GIT-TEST0000/revoke").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void bossRevoke_activeCode_200AndTheRowEchoesRevoked() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String codeString = mintOneCodeAsBoss(bossCookie, c);

        mockMvc.perform(post("/api/codes/" + codeString + "/revoke").cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(codeString))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.courseTitle").value("Git Foundations"))
                .andExpect(jsonPath("$.claimantUsername").doesNotExist())
                .andExpect(jsonPath("$.redeemedAt").doesNotExist());

        mockMvc.perform(post("/api/codes/" + codeString + "/revoke").cookie(bossCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This code was revoked."));
    }

    @Test
    void adminAssistantRevoke_alsoOpensTheDoor_200() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.assistant", Role.ADMIN_ASSISTANT);
        jakarta.servlet.http.Cookie assistantCookie = loginAndGrabCookie("the.assistant@mail.com");

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "1")
                        .cookie(assistantCookie))
                .andExpect(status().isOk());
        String codeString = codeRepository.findByCourseId(c.getId()).get(0).getCode();

        mockMvc.perform(post("/api/codes/" + codeString + "/revoke").cookie(assistantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void revoke_unknownString_answersTheSharedRefusal_409() throws Exception {
        cleanTables();
        seedUser("the.boss", Role.BOSS);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");

        mockMvc.perform(post("/api/codes/XXX-NOPE0000/revoke").cookie(bossCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Invalid or expired code."));
    }

    @Test
    void revoke_claimedCode_theCrossedOutRowKeepsItsHistory() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        seedUser("code.holder", Role.TRAINEE);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String codeString = mintOneCodeAsBoss(bossCookie, c);

        jakarta.servlet.http.Cookie holderCookie = loginAndGrabCookie("code.holder@mail.com");
        mockMvc.perform(post("/api/codes/" + codeString + "/redeem").cookie(holderCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/codes/" + codeString + "/revoke").cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.claimantUsername").value("code.holder"))
                .andExpect(jsonPath("$.redeemedAt").isNotEmpty());

        mockMvc.perform(get("/api/codes").param("courseId", String.valueOf(c.getId())).cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("REVOKED"))
                .andExpect(jsonPath("$[0].claimantUsername").value("code.holder"));
    }

    @Test
    void revokedCode_closesTheClaimantsDoor_403AccessDenied() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        seedUser("code.holder", Role.TRAINEE);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String codeString = mintOneCodeAsBoss(bossCookie, c);

        jakarta.servlet.http.Cookie holderCookie = loginAndGrabCookie("code.holder@mail.com");
        mockMvc.perform(post("/api/codes/" + codeString + "/redeem").cookie(holderCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/" + c.getId()).cookie(holderCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/codes/" + codeString + "/revoke").cookie(bossCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/" + c.getId()).cookie(holderCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void revokedCodeOnRedeem_earnsItsOwnHonestWord_409NotTheSpentWord() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        seedUser("code.holder", Role.TRAINEE);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String codeString = mintOneCodeAsBoss(bossCookie, c);

        mockMvc.perform(post("/api/codes/" + codeString + "/revoke").cookie(bossCookie))
                .andExpect(status().isOk());

        jakarta.servlet.http.Cookie holderCookie = loginAndGrabCookie("code.holder@mail.com");
        mockMvc.perform(post("/api/codes/" + codeString + "/redeem").cookie(holderCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This code was revoked."));
    }
}
