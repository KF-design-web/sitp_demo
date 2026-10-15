package com.example.sitp.access;

import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallengeRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RedeemableCodeControllerMockMvcTest {

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
        userRepository.save(com.example.sitp.user.model.User.builder()
                .email(username + "@mail.com")
                .username(username)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .accountType(com.example.sitp.user.model.AccountType.OUTSIDER)
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build());
    }

    private String[] freshCaptcha() throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/captcha"))
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
                        .param("email", email)                        .param("password", "password123")
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
    void anonymousMint_earnsTheBuildingsLock_401() throws Exception {
        cleanTables();
        Course c = course();

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void traineeMint_earnsTheHonest403() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("plain.trainee", Role.TRAINEE);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("plain.trainee@mail.com");

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "1")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void bossMint_200WithDistinctUnclaimedRows() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("the.boss@mail.com");

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "3")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].code").value(
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.matchesPattern("[A-Z]{3}-[A-Z0-9]{8}"))))
                .andExpect(jsonPath("$[*].status").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))))
                .andExpect(jsonPath("$[0].redeemedAt").doesNotExist())
                .andExpect(jsonPath("$[0].courseTitle").value("Git Foundations"));
    }

    @Test
    void adminAssistantMint_alsoOpensTheMintDoor_200() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.assistant", Role.ADMIN_ASSISTANT);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("the.assistant@mail.com");

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void mint_unknownCourse_answersTheThrown404() throws Exception {
        cleanTables();
        seedUser("the.boss", Role.BOSS);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("the.boss@mail.com");

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", "9999")
                        .param("count", "1")
                        .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Course not found."));
    }

    @Test
    void mint_countOutOfBounds_earnsTheHonest400() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("the.boss@mail.com");

        mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "0")
                        .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("1 and 500")));
    }

    @Test
    void listAnonAndTrainee_earnLockAnd403() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("plain.trainee", Role.TRAINEE);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/codes").param("courseId", String.valueOf(c.getId())))
                .andExpect(status().isUnauthorized());

        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("plain.trainee@mail.com");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/codes").param("courseId", String.valueOf(c.getId())).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void bossList_200WithClaimedAndUnclaimed_fieldsExact() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        seedUser("code.holder2", Role.TRAINEE);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");

        String mintBody = mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "2")
                        .cookie(bossCookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"code\":\"([A-Z0-9-]+)\"").matcher(mintBody);
        matcher.find();
        String first = matcher.group(1);
        matcher.find();
        String second = matcher.group(1);

        jakarta.servlet.http.Cookie holderCookie = loginAndGrabCookie("code.holder2@mail.com");
        mockMvc.perform(post("/api/codes/" + first + "/redeem").cookie(holderCookie))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/codes").param("courseId", String.valueOf(c.getId())).cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].claimantUsername").value("code.holder2"))
                .andExpect(jsonPath("$[1].claimantUsername").doesNotExist())
                .andExpect(jsonPath("$[*].status").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/codes").param("courseId", "9999").cookie(bossCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found."));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/codes").cookie(bossCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeemArc_mintThenClaimThenReplay_theFullWalk() throws Exception {
        cleanTables();
        Course c = course();
        seedUser("the.boss", Role.BOSS);
        seedUser("code.holder", Role.TRAINEE);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");

        String body = mockMvc.perform(post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(c.getId()))
                        .param("count", "1")
                        .cookie(bossCookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String codeString = body.replaceAll(".*\"code\":\"([A-Z0-9-]+)\".*", "$1").trim();

        jakarta.servlet.http.Cookie holderCookie = loginAndGrabCookie("code.holder@mail.com");
        mockMvc.perform(post("/api/codes/" + codeString + "/redeem").cookie(holderCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(c.getId()))
                .andExpect(jsonPath("$.courseTitle").value("Git Foundations"))
                .andExpect(jsonPath("$.accessUntil").value("lifetime"));

        mockMvc.perform(post("/api/codes/" + codeString + "/redeem").cookie(holderCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This code has already been used."));

        mockMvc.perform(post("/api/codes/XXX-NOPE0000/redeem").cookie(holderCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid or expired code."));
    }
}
