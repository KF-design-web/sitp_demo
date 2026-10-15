package com.example.sitp.course;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallengeRepository;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Chapter;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.ChapterRepository;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CourseControllerDetailTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaptchaChallengeRepository captchaChallengeRepository;

    @Autowired
    private RedeemableCodeRepository redeemableCodeRepository;

    private void cleanTables() {
        redeemableCodeRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        chapterRepository.deleteAll();
        courseRepository.deleteAll();
    }

    private Course seededCourse() {
        Course c = courseRepository.save(Course.builder()
                .title("Git Foundations")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());
        chapterRepository.save(Chapter.builder()
                .title("First steps").videoUrl("bunny-ref-1").position(1).course(c).build());
        chapterRepository.save(Chapter.builder()
                .title("Going further").videoUrl("bunny-ref-2").position(2).course(c).build());
        return c;
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
                        .param("email", "ana@mail.com")
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

    private RedeemableCode grantAnaAnActiveCode(Course course, String codeLiteral) {
        var ana = userRepository.findByUsername("ana").orElseThrow();
        return redeemableCodeRepository.save(RedeemableCode.builder()
                .code(codeLiteral)
                .course(course)
                .claimant(ana)
                .status(RedeemableCodeStatus.ACTIVE)
                .build());
    }

    private void grantAnaASecondActiveCodeForTheSameCourse(Course course) {
        grantAnaAnActiveCode(course, "GIT-TEST0002");
    }

    private void registeredAna_enabledWithAccountType(AccountType accountType) throws Exception {
        registerAna();
        var ana = userRepository.findByUsername("ana").orElseThrow();
        ana.setAccountType(accountType);
        userRepository.save(ana);
    }

    @Test
    void anonymousKnock_earnsTheBuildingsLock_401() throws Exception {
        cleanTables();

        mockMvc.perform(get("/api/courses/" + 42L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInMember_unknownCourse_earnsTheThrown404() throws Exception {
        cleanTables();
        registerAna();
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + 9999L).cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Course not found."));
    }

    @Test
    void loggedInMember_realCourse_200WithChaptersInTeachingOrder() throws Exception {
        cleanTables();
        Course course = seededCourse();
        Long id = course.getId();
        registerAna();
        grantAnaAnActiveCode(course, "GIT-TEST0001");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + id).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Git Foundations"))
                .andExpect(jsonPath("$.chapters[*].title").value(
                        org.hamcrest.Matchers.contains("First steps", "Going further")))
                .andExpect(jsonPath("$.chapters[*].position").value(
                        org.hamcrest.Matchers.contains(1, 2)))
                .andExpect(jsonPath("$.chapters[0].videoUrl").value("bunny-ref-1"))
                .andExpect(jsonPath("$.priceVisible").value(true))
                .andExpect(jsonPath("$.price").value(49.90));
    }

    @Test
    void codeHolder_opensTheCourse_200WithChapters() throws Exception {
        cleanTables();
        Course course = seededCourse();
        registerAna();
        grantAnaAnActiveCode(course, "GIT-TEST0001");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + course.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters", hasSize(2)));
    }

    @Test
    void loggedInOutsider_withoutAnyCode_earnsTheHonest403() throws Exception {
        cleanTables();
        Long id = seededCourse().getId();
        registerAna();
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + id).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void holderWithTwoActiveCodesForOneCourse_stillOpensTheCourse_200NotServerError() throws Exception {
        cleanTables();
        Course course = seededCourse();
        registerAna();
        grantAnaAnActiveCode(course, "GIT-TEST0001");
        grantAnaASecondActiveCodeForTheSameCourse(course);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + course.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters", hasSize(2)));
    }

    private Course seededInternCourse(String title, int chapterCount) {
        Course c = courseRepository.save(Course.builder()
                .title(title)
                .description("People skills for the port workforce")
                .price(new java.math.BigDecimal("50.00"))
                .targetAudience(Audience.INTERN)
                .build());
        for (int i = 1; i <= chapterCount; i++) {
            chapterRepository.save(Chapter.builder()
                    .title("Chapter " + i).videoUrl("bunny-ref-" + i).position(i).course(c).build());
        }
        return c;
    }

    @Test
    void internHolder_oneCodeForOneInternCourse_opensEveryInternCourse() throws Exception {
        cleanTables();
        Course hrCourse = seededInternCourse("HR Essentials", 1);
        Course freightCourse = seededInternCourse("Freight Documentation", 2);
        registeredAna_enabledWithAccountType(AccountType.INTERN);
        grantAnaAnActiveCode(hrCourse, "HRE-TEST0001");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + freightCourse.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Freight Documentation"))
                .andExpect(jsonPath("$.chapters", hasSize(2)));
    }

    @Test
    void internHolder_codeServingAnOutsiderCourse_doesNotOpenOtherInternCourses() throws Exception {
        cleanTables();
        Course outsiderCourse = seededCourse();
        Course otherInternCourse = seededInternCourse("Workplace Communication", 1);
        registeredAna_enabledWithAccountType(AccountType.INTERN);
        grantAnaAnActiveCode(outsiderCourse, "GIT-TEST0001");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + otherInternCourse.getId()).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));

        mockMvc.perform(get("/api/courses/" + outsiderCourse.getId()).cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    void internHolder_anInternCode_doesNotOpenOutsiderCourses() throws Exception {
        cleanTables();
        Course outsiderCourse = seededCourse();
        Course hrCourse = seededInternCourse("HR Essentials", 1);
        registeredAna_enabledWithAccountType(AccountType.INTERN);
        grantAnaAnActiveCode(hrCourse, "HRE-TEST0001");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + outsiderCourse.getId()).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void internHolder_passedTheTimeBox_earnsTheLocked403() throws Exception {
        cleanTables();
        Course internshipCourse = seededInternCourse("HR Essentials", 2);
        registeredAna_enabledWithAccountType(AccountType.INTERN);
        RedeemableCode row = grantAnaAnActiveCode(internshipCourse, "HRE-TEST0001");
        row.setValidityDays(7);
        row.setRedeemedAt(java.time.LocalDateTime.now().minusDays(10));
        redeemableCodeRepository.save(row);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie();

        mockMvc.perform(get("/api/courses/" + internshipCourse.getId()).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }
}
