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
class DuplicateRedeemLawMockMvcTest {

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

    private Course outsiderCourse() {
        return courseRepository.save(Course.builder()
                .title("Git Foundations")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());
    }

    private Course internCourse(String title) {
        return courseRepository.save(Course.builder()
                .title(title)
                .description("The intern track")
                .price(null)
                .targetAudience(Audience.INTERN)
                .build());
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

    private String mintOneCourseCode(jakarta.servlet.http.Cookie bossCookie, Course course,
                                     Integer validityDays) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                post("/api/codes")
                        .contentType("application/x-www-form-urlencoded")
                        .param("courseId", String.valueOf(course.getId()))
                        .param("count", "1")
                        .cookie(bossCookie);
        if (validityDays != null) {
            request.param("validityDays", String.valueOf(validityDays));
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\\\"code\\\":\\\"([A-Z0-9-]+)\\\"").matcher(body);
        matcher.find();
        return matcher.group(1);
    }

    @Test
    void outsiderSecondCodeForHeldCourse_refused_409AndTheFreshCodeStaysClaimable() throws Exception {
        cleanTables();
        Course c = outsiderCourse();
        seedUser("the.boss", Role.BOSS, AccountType.OUTSIDER);
        seedUser("plain.trainee", Role.TRAINEE, AccountType.OUTSIDER);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String firstCode = mintOneCourseCode(bossCookie, c, null);
        String secondCode = mintOneCourseCode(bossCookie, c, null);

        jakarta.servlet.http.Cookie holderCookie = loginAndGrabCookie("plain.trainee@mail.com");
        mockMvc.perform(post("/api/codes/" + firstCode + "/redeem").cookie(holderCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessUntil").value("lifetime"));

        mockMvc.perform(post("/api/codes/" + secondCode + "/redeem").cookie(holderCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("You already have access to this course."));

        mockMvc.perform(get("/api/codes").param("courseId", String.valueOf(c.getId())).cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].claimantUsername").value("plain.trainee"))
                .andExpect(jsonPath("$[1].claimantUsername").doesNotExist());
    }

    @Test
    void internRenewal_secondCodeForTheSameInternCourse_stillClaims_200() throws Exception {
        cleanTables();
        Course c = internCourse("Workplace Communication");
        seedUser("the.boss", Role.BOSS, AccountType.OUTSIDER);
        seedUser("intern.marie", Role.TRAINEE, AccountType.INTERN);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String firstCode = mintOneCourseCode(bossCookie, c, 30);
        String renewalCode = mintOneCourseCode(bossCookie, c, 30);

        jakarta.servlet.http.Cookie internCookie = loginAndGrabCookie("intern.marie@mail.com");
        mockMvc.perform(post("/api/codes/" + firstCode + "/redeem").cookie(internCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessUntil")
                        .value(org.hamcrest.Matchers.containsString("days left")));

        mockMvc.perform(post("/api/codes/" + renewalCode + "/redeem").cookie(internCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(c.getId()))
                .andExpect(jsonPath("$.accessUntil")
                        .value(org.hamcrest.Matchers.containsString("days left")));
    }

    @Test
    void outsiderWhoseHoldWasRevoked_earnsAFreshCodeForTheSameCourse_200() throws Exception {
        cleanTables();
        Course c = outsiderCourse();
        seedUser("the.boss", Role.BOSS, AccountType.OUTSIDER);
        seedUser("plain.trainee", Role.TRAINEE, AccountType.OUTSIDER);
        jakarta.servlet.http.Cookie bossCookie = loginAndGrabCookie("the.boss@mail.com");
        String heldCode = mintOneCourseCode(bossCookie, c, null);
        String freshCode = mintOneCourseCode(bossCookie, c, null);

        jakarta.servlet.http.Cookie traineeCookie = loginAndGrabCookie("plain.trainee@mail.com");
        mockMvc.perform(post("/api/codes/" + heldCode + "/redeem").cookie(traineeCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/codes/" + heldCode + "/revoke").cookie(bossCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        mockMvc.perform(post("/api/codes/" + freshCode + "/redeem").cookie(traineeCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(c.getId()))
                .andExpect(jsonPath("$.accessUntil").value("lifetime"));
    }
}
