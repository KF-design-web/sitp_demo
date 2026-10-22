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
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrainerCourseControllerMockMvcTest {

    private static final String FORM = "application/x-www-form-urlencoded";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private RedeemableCodeRepository redeemableCodeRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CaptchaChallengeRepository captchaChallengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void cleanTables() {
        redeemableCodeRepository.deleteAll();
        sessionRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        chapterRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User seedUser(String username, Role role) {
        return userRepository.save(User.builder()
                .email(username + "@mail.com")
                .username(username)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .accountType(AccountType.OUTSIDER)
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build());
    }

    private Course courseFor(User trainer, String title) {
        return courseRepository.save(Course.builder()
                .title(title)
                .description("A course called " + title)
                .targetAudience(Audience.OUTSIDER)
                .price(new BigDecimal("49.90"))
                .trainer(trainer)
                .build());
    }

    private Chapter chapterFor(Course course, String title, int position) {
        return chapterRepository.save(Chapter.builder()
                .title(title)
                .videoUrl("bunny-ref-" + position)
                .position(position)
                .course(course)
                .build());
    }

    private String[] freshCaptcha() throws Exception {
        String body = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1").trim();
        var challenge = captchaChallengeRepository.findById(Long.parseLong(id)).orElseThrow();
        String[] parts = challenge.getQuestion().split(" ");
        String answer = String.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[2]));
        return new String[]{id, answer};
    }

    private jakarta.servlet.http.Cookie loginAndGrabCookie(String email) throws Exception {
        String[] cap = freshCaptcha();
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(FORM)
                        .param("email", email)
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

    // ----- create -----

    @Test
    void anonymousCreate_earnsTheBuildingsLock_401() throws Exception {
        cleanTables();

        mockMvc.perform(post("/api/courses/trainer")
                        .contentType(FORM)
                        .param("title", "Sneaky")
                        .param("targetAudience", "INTERN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void traineeCreate_earnsTheHonest403() throws Exception {
        cleanTables();
        seedUser("plain.trainee", Role.TRAINEE);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("plain.trainee@mail.com");

        mockMvc.perform(post("/api/courses/trainer")
                        .contentType(FORM)
                        .param("title", "Sneaky")
                        .param("targetAudience", "INTERN")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void trainerCreate_201_withEveryStudioField() throws Exception {
        cleanTables();
        seedUser("trainer.one", Role.TRAINER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(post("/api/courses/trainer")
                        .contentType(FORM)
                        .param("title", "Git Deep Dive")
                        .param("description", "Branches without fear")
                        .param("labels", "git, branching")
                        .param("thumbnailUrl", "https://img/cover.png")
                        .param("targetAudience", "OUTSIDER")
                        .param("price", "49.90")
                        .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Git Deep Dive"))
                .andExpect(jsonPath("$.labels").value("git, branching"))
                .andExpect(jsonPath("$.thumbnailUrl").value("https://img/cover.png"))
                .andExpect(jsonPath("$.targetAudience").value("OUTSIDER"))
                .andExpect(jsonPath("$.price").value(49.90))
                .andExpect(jsonPath("$.priceVisible").value(true));
    }

    @Test
    void trainerCreate_missingTitle_earnsTheHonest400() throws Exception {
        cleanTables();
        seedUser("trainer.one", Role.TRAINER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(post("/api/courses/trainer")
                        .contentType(FORM)
                        .param("targetAudience", "INTERN")
                        .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Title is required.")));
    }

    // ----- list -----

    @Test
    void myCreatedCourses_200_onlyTheCallersOwnRows() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        User other = seedUser("trainer.two", Role.TRAINER);
        courseFor(owner, "Owned one");
        courseFor(other, "Someone else's");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(get("/api/courses/trainer/mine").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Owned one"));
    }

    @Test
    void myCreatedCourses_trainee_earnsTheHonest403() throws Exception {
        cleanTables();
        seedUser("plain.trainee", Role.TRAINEE);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("plain.trainee@mail.com");

        mockMvc.perform(get("/api/courses/trainer/mine").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    // ----- the owner's library door -----

    @Test
    void trainerCourseDetail_ownerReadsEpisodes_200_withoutAnyRedeemableCode() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Git Deep Dive");
        chapterFor(course, "First steps", 1);
        chapterFor(course, "Going further", 2);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(get("/api/courses/trainer/" + course.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(course.getId()))
                .andExpect(jsonPath("$.chapters", hasSize(2)))
                .andExpect(jsonPath("$.chapters[*].title").value(contains("First steps", "Going further")));
    }

    @Test
    void trainerCourseDetail_anotherTrainersCourse_earnsTheNamed403() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        seedUser("trainer.two", Role.TRAINER);
        Course course = courseFor(owner, "Mine");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.two@mail.com");

        mockMvc.perform(get("/api/courses/trainer/" + course.getId()).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This course belongs to another trainer."));
    }

    // ----- episodes -----

    @Test
    void addEpisode_owner_201_positionAutoAppendsLast() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Git Deep Dive");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(post("/api/courses/" + course.getId() + "/chapters")
                        .contentType(FORM)
                        .param("title", "First steps")
                        .param("description", "Where to begin")
                        .param("labels", "intro")
                        .param("videoUrl", "https://video/1")
                        .param("thumbnailUrl", "https://img/ep.png")
                        .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.title").value("First steps"))
                .andExpect(jsonPath("$.position").value(1));

        mockMvc.perform(post("/api/courses/" + course.getId() + "/chapters")
                        .contentType(FORM)
                        .param("title", "Going further")
                        .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(2));
    }

    @Test
    void addEpisode_anotherTrainersCourse_earnsTheNamed403() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        seedUser("trainer.two", Role.TRAINER);
        Course course = courseFor(owner, "Mine");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.two@mail.com");

        mockMvc.perform(post("/api/courses/" + course.getId() + "/chapters")
                        .contentType(FORM)
                        .param("title", "Hijack")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This course belongs to another trainer."));
    }

    @Test
    void addEpisode_unknownCourse_earnsTheThrown404() throws Exception {
        cleanTables();
        seedUser("trainer.one", Role.TRAINER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(post("/api/courses/9999/chapters")
                        .contentType(FORM)
                        .param("title", "Nowhere")
                        .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found."));
    }

    // ----- update -----

    @Test
    void updateOwnCourse_200_theStudioFieldsRoundTrip() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Before");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .contentType(FORM)
                        .param("title", "After")
                        .param("description", "An updated description")
                        .param("labels", "edited, tags")
                        .param("thumbnailUrl", "https://img/edited-cover.png")
                        .param("price", "59.90")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("After"))
                .andExpect(jsonPath("$.labels").value("edited, tags"))
                .andExpect(jsonPath("$.thumbnailUrl").value("https://img/edited-cover.png"))
                .andExpect(jsonPath("$.price").value(59.90))
                .andExpect(jsonPath("$.targetAudience").value("OUTSIDER"));
    }

    @Test
    void updateAnotherTrainersCourse_earnsTheNamed403() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        seedUser("trainer.two", Role.TRAINER);
        Course course = courseFor(owner, "Mine");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.two@mail.com");

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .contentType(FORM)
                        .param("title", "Hijack")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This course belongs to another trainer."));
    }

    @Test
    void updateUnknownCourse_earnsTheThrown404() throws Exception {
        cleanTables();
        seedUser("trainer.one", Role.TRAINER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(put("/api/courses/9999")
                        .contentType(FORM)
                        .param("title", "Nowhere")
                        .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found."));
    }

    // ----- delete -----

    @Test
    void deleteOwnCourse_204_andItsChaptersGoWithIt() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Doomed");
        chapterFor(course, "Ep 1", 1);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(delete("/api/courses/" + course.getId()).cookie(cookie))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(courseRepository.findById(course.getId())).isEmpty();
        org.assertj.core.api.Assertions.assertThat(chapterRepository.findByCourseIdOrderByPositionAsc(course.getId())).isEmpty();
    }

    @Test
    void deleteCourseWithCodesHandedOut_earnsThe409Block() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Locked");
        redeemableCodeRepository.save(RedeemableCode.builder()
                .code("GIT-ABC12345")
                .course(course)
                .status(RedeemableCodeStatus.ACTIVE)
                .build());
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(delete("/api/courses/" + course.getId()).cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This course has codes handed out and cannot be deleted yet."));
    }

    @Test
    void deleteAnotherTrainersCourse_earnsTheNamed403() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        seedUser("trainer.two", Role.TRAINER);
        Course course = courseFor(owner, "Mine");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.two@mail.com");

        mockMvc.perform(delete("/api/courses/" + course.getId()).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This course belongs to another trainer."));
    }

    // ----- episode update / delete doors -----

    @Test
    void updateOwnEpisode_200() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Git Deep Dive");
        Chapter episode = chapterFor(course, "Original", 1);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(put("/api/chapters/" + episode.getId())
                        .contentType(FORM)
                        .param("title", "Renamed")
                        .param("description", "A new story")
                        .param("labels", "edited")
                        .param("videoUrl", "https://video/9")
                        .param("position", "4")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.position").value(4));
    }

    @Test
    void updateAnotherTrainersEpisode_earnsTheNamed403() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        seedUser("trainer.two", Role.TRAINER);
        Course course = courseFor(owner, "Git Deep Dive");
        Chapter episode = chapterFor(course, "Original", 1);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.two@mail.com");

        mockMvc.perform(put("/api/chapters/" + episode.getId())
                        .contentType(FORM)
                        .param("title", "Hijack")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This course belongs to another trainer."));
    }

    @Test
    void updateUnknownEpisode_earnsTheThrown404() throws Exception {
        cleanTables();
        seedUser("trainer.one", Role.TRAINER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(put("/api/chapters/9999")
                        .contentType(FORM)
                        .param("title", "Nowhere")
                        .cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Episode not found."));
    }

    @Test
    void deleteOwnEpisode_204() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        Course course = courseFor(owner, "Git Deep Dive");
        Chapter episode = chapterFor(course, "Doomed", 1);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(delete("/api/chapters/" + episode.getId()).cookie(cookie))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(chapterRepository.findById(episode.getId())).isEmpty();
    }

    @Test
    void deleteAnotherTrainersEpisode_earnsTheNamed403() throws Exception {
        cleanTables();
        User owner = seedUser("trainer.one", Role.TRAINER);
        seedUser("trainer.two", Role.TRAINER);
        Course course = courseFor(owner, "Git Deep Dive");
        Chapter episode = chapterFor(course, "Mine", 1);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.two@mail.com");

        mockMvc.perform(delete("/api/chapters/" + episode.getId()).cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This course belongs to another trainer."));
    }

    @Test
    void deleteUnknownEpisode_earnsTheThrown404() throws Exception {
        cleanTables();
        seedUser("trainer.one", Role.TRAINER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("trainer.one@mail.com");

        mockMvc.perform(delete("/api/chapters/9999").cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Episode not found."));
    }
}
