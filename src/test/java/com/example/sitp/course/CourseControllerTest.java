package com.example.sitp.course;

import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private com.example.sitp.course.repository.ChapterRepository chapterRepository;

    @Autowired
    private com.example.sitp.auth.captcha.CaptchaChallengeRepository captchaChallengeRepository;

    @Autowired
    private com.example.sitp.access.repository.SessionRepository sessionRepository;

    @Autowired
    private com.example.sitp.user.repository.UserRepository userRepository;

    private void cleanTables() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        chapterRepository.deleteAll();
        courseRepository.deleteAll();
    }

    private Course course(String title, Audience audience) {
        return Course.builder()
                .title(title)
                .description("A course called " + title)
                .targetAudience(audience)
                .build();
    }

    @Test
    void loggedOutVisitor_gets200AndTheList() throws Exception {
        cleanTables();

        courseRepository.save(course("Spring Boot Basics", Audience.INTERN));
        courseRepository.save(course("Advanced Git", Audience.OUTSIDER));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title",
                        hasItem("Spring Boot Basics")))
                .andExpect(jsonPath("$[*].title", hasItem("Advanced Git")))
                .andExpect(jsonPath("$[*].targetAudience",
                        hasItem("INTERN")))
                .andExpect(jsonPath("$[*].targetAudience",
                        hasItem("OUTSIDER")));

    }

    @Test
    void loggedOutVisitor_seesNoPriceLine_ever() throws Exception {
        cleanTables();
        courseRepository.save(Course.builder()
                .title("Advanced Git")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].price").doesNotExist())
                .andExpect(jsonPath("$[*].priceVisible").value(false));
    }

    @Test
    void loggedInMember_seesThePriceLine_theGateRuleCashedIn() throws Exception {
        cleanTables();
        courseRepository.save(Course.builder()
                .title("Advanced Git")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());

        registerAna();
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].priceVisible").value(true))
                .andExpect(jsonPath("$[0].price").value(49.90));
    }

    private String[] freshCaptcha() throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1").trim();
        var challenge = captchaChallengeRepository.findById(Long.parseLong(id)).orElseThrow();
        String[] parts = challenge.getQuestion().split(" ");
        String answer = String.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[2]));
        return new String[]{id, answer};
    }

    private void registerAna() throws Exception {
        String[] cap = freshCaptcha();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", "ana@mail.com")
                        .param("username", "ana")
                        .param("firstName", "Ana")
                        .param("lastName", "Example")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isCreated());

        var ana = userRepository.findByUsername("ana").orElseThrow();
        ana.setEnabled(true);
        userRepository.save(ana);
    }

    private jakarta.servlet.http.Cookie loginAndGrabCookie() throws Exception {
        String[] cap = freshCaptcha();
        String setCookie = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login")
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
    void emptyCatalog_answersEmptyList_not404() throws Exception {
        cleanTables();

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }
}
