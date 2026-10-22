package com.example.sitp.course;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallengeRepository;
import com.example.sitp.course.dto.ChapterRequest;
import com.example.sitp.course.dto.ChapterResponse;
import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.dto.CreateCourseRequest;
import com.example.sitp.course.dto.UpdateCourseRequest;
import com.example.sitp.course.exception.ChapterNotFoundException;
import com.example.sitp.course.exception.CourseDeletionBlockedException;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.exception.CourseOwnershipException;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Chapter;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.ChapterRepository;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.course.service.CourseService;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrainerCourseServiceTest {

    @Autowired
    private CourseService courseService;

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

    private void cleanTables() {
        redeemableCodeRepository.deleteAll();
        sessionRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        chapterRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User seedTrainer(String username) {
        return userRepository.save(User.builder()
                .email(username + "@mail.com")
                .username(username)
                .passwordHash("irrelevant-for-this-test")
                .role(Role.TRAINER)
                .accountType(AccountType.OUTSIDER)
                .firstName("Test")
                .lastName("Trainer")
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

    private CreateCourseRequest createRequest(String title) {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle(title);
        request.setDescription("What the course teaches");
        request.setLabels("git, branching");
        request.setThumbnailUrl("https://img/cover.png");
        request.setTargetAudience(Audience.OUTSIDER);
        request.setPrice(new BigDecimal("49.90"));
        return request;
    }

    private UpdateCourseRequest updateRequest(String title) {
        UpdateCourseRequest request = new UpdateCourseRequest();
        request.setTitle(title);
        request.setDescription("An updated description");
        request.setLabels("edited, tags");
        request.setThumbnailUrl("https://img/edited-cover.png");
        request.setPrice(new BigDecimal("59.90"));
        return request;
    }

    private ChapterRequest chapterRequest(String title, Integer position) {
        ChapterRequest request = new ChapterRequest();
        request.setTitle(title);
        request.setDescription("What the episode teaches");
        request.setLabels("intro");
        request.setVideoUrl("https://video/1");
        request.setThumbnailUrl("https://img/episode.png");
        request.setPosition(position);
        return request;
    }

    @Test
    void createCourse_setsTheTrainerFromThePrincipal_andMapsEveryStudioField() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");

        CourseResponse card = courseService.createCourse(createRequest("Git Deep Dive"), trainer.getId());

        assertThat(card.getTitle()).isEqualTo("Git Deep Dive");
        assertThat(card.getLabels()).isEqualTo("git, branching");
        assertThat(card.getThumbnailUrl()).isEqualTo("https://img/cover.png");
        assertThat(card.getTargetAudience()).isEqualTo(Audience.OUTSIDER);
        assertThat(card.getPrice()).isEqualByComparingTo("49.90");
        assertThat(courseRepository.findByTrainerIdAndId(trainer.getId(), card.getId())).hasSize(1);
    }

    @Test
    void getMyCourses_returnsOnlyTheCallersCourses() {
        cleanTables();
        User trainerOne = seedTrainer("trainer.one");
        User trainerTwo = seedTrainer("trainer.two");
        courseFor(trainerOne, "Owned one");
        courseFor(trainerTwo, "Someone else's");

        List<CourseResponse> mine = courseService.getMyCourses(trainerOne.getId());

        assertThat(mine).extracting(CourseResponse::getTitle).containsExactly("Owned one");
    }

    @Test
    void getCourseForTrainer_ownCourse_returnsIt() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Mine");

        Course fetched = courseService.getCourseForTrainer(course.getId(), trainer.getId());

        assertThat(fetched.getId()).isEqualTo(course.getId());
    }

    @Test
    void getCourseForTrainer_anotherTrainersCourse_throwsTheOwnershipVerdict() {
        cleanTables();
        User owner = seedTrainer("trainer.one");
        User intruder = seedTrainer("trainer.two");
        Course course = courseFor(owner, "Mine");

        assertThatThrownBy(() -> courseService.getCourseForTrainer(course.getId(), intruder.getId()))
                .isInstanceOf(CourseOwnershipException.class);
    }

    @Test
    void getCourseForTrainer_unknownCourse_throwsTheThrown404() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");

        assertThatThrownBy(() -> courseService.getCourseForTrainer(9999L, trainer.getId()))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void updateCourse_ownCourse_updatesStudioFields_andLeavesTargetAudienceUntouched() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Before");

        CourseResponse card = courseService.updateCourse(course.getId(), trainer.getId(), updateRequest("After"));

        assertThat(card.getTitle()).isEqualTo("After");
        assertThat(card.getLabels()).isEqualTo("edited, tags");
        assertThat(card.getThumbnailUrl()).isEqualTo("https://img/edited-cover.png");
        assertThat(card.getPrice()).isEqualByComparingTo("59.90");
        assertThat(card.getTargetAudience()).isEqualTo(Audience.OUTSIDER);
    }

    @Test
    void updateCourse_anotherTrainersCourse_throwsTheOwnershipVerdict() {
        cleanTables();
        User owner = seedTrainer("trainer.one");
        User intruder = seedTrainer("trainer.two");
        Course course = courseFor(owner, "Mine");

        assertThatThrownBy(() -> courseService.updateCourse(course.getId(), intruder.getId(), updateRequest("Hijack")))
                .isInstanceOf(CourseOwnershipException.class);
        assertThat(courseRepository.findById(course.getId()).orElseThrow().getTitle()).isEqualTo("Mine");
    }

    @Test
    void deleteCourse_ownCourseWithNoCodes_wipesItsChaptersThenTheCourse() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Doomed");
        chapterRepository.save(Chapter.builder().title("Ep 1").position(1).course(course).build());

        courseService.deleteCourse(course.getId(), trainer.getId());

        assertThat(courseRepository.findById(course.getId())).isEmpty();
        assertThat(chapterRepository.findByCourseIdOrderByPositionAsc(course.getId())).isEmpty();
    }

    @Test
    void deleteCourse_withCodesHandedOut_throwsTheBlockInsteadOfCrashing() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Locked");
        redeemableCodeRepository.save(RedeemableCode.builder()
                .code("GIT-ABC12345")
                .course(course)
                .status(RedeemableCodeStatus.ACTIVE)
                .build());

        assertThatThrownBy(() -> courseService.deleteCourse(course.getId(), trainer.getId()))
                .isInstanceOf(CourseDeletionBlockedException.class);
        assertThat(courseRepository.findById(course.getId())).isPresent();
    }

    @Test
    void deleteCourse_anotherTrainersCourse_throwsTheOwnershipVerdict() {
        cleanTables();
        User owner = seedTrainer("trainer.one");
        User intruder = seedTrainer("trainer.two");
        Course course = courseFor(owner, "Mine");

        assertThatThrownBy(() -> courseService.deleteCourse(course.getId(), intruder.getId()))
                .isInstanceOf(CourseOwnershipException.class);
        assertThat(courseRepository.findById(course.getId())).isPresent();
    }

    @Test
    void createChapter_withNoPosition_appendsLast() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Series");

        ChapterResponse first = courseService.createChapter(course.getId(), trainer.getId(), chapterRequest("Ep 1", null));
        ChapterResponse second = courseService.createChapter(course.getId(), trainer.getId(), chapterRequest("Ep 2", null));

        assertThat(first.getPosition()).isEqualTo(1);
        assertThat(second.getPosition()).isEqualTo(2);
        assertThat(second.getCourseId()).isEqualTo(course.getId());
    }

    @Test
    void createChapter_withExplicitPosition_usesIt() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Series");

        ChapterResponse episode = courseService.createChapter(course.getId(), trainer.getId(), chapterRequest("Bonus", 5));

        assertThat(episode.getPosition()).isEqualTo(5);
    }

    @Test
    void updateChapter_ownEpisode_updatesEveryField() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Series");
        ChapterResponse episode = courseService.createChapter(course.getId(), trainer.getId(), chapterRequest("Original", 1));

        ChapterResponse updated = courseService.updateChapter(episode.getId(), trainer.getId(), chapterRequest("Renamed", 3));

        assertThat(updated.getTitle()).isEqualTo("Renamed");
        assertThat(updated.getPosition()).isEqualTo(3);
        assertThat(updated.getDescription()).isEqualTo("What the episode teaches");
        assertThat(updated.getVideoUrl()).isEqualTo("https://video/1");
    }

    @Test
    void updateChapter_anotherTrainersEpisode_throwsTheOwnershipVerdict() {
        cleanTables();
        User owner = seedTrainer("trainer.one");
        User intruder = seedTrainer("trainer.two");
        Course course = courseFor(owner, "Series");
        ChapterResponse episode = courseService.createChapter(course.getId(), owner.getId(), chapterRequest("Original", 1));

        assertThatThrownBy(() -> courseService.updateChapter(episode.getId(), intruder.getId(), chapterRequest("Hijack", 1)))
                .isInstanceOf(CourseOwnershipException.class);
    }

    @Test
    void deleteChapter_ownEpisode_deletesIt() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");
        Course course = courseFor(trainer, "Series");
        ChapterResponse episode = courseService.createChapter(course.getId(), trainer.getId(), chapterRequest("Doomed", 1));

        courseService.deleteChapter(episode.getId(), trainer.getId());

        assertThat(chapterRepository.findById(episode.getId())).isEmpty();
    }

    @Test
    void deleteChapter_unknownEpisode_throwsTheThrown404() {
        cleanTables();
        User trainer = seedTrainer("trainer.one");

        assertThatThrownBy(() -> courseService.deleteChapter(9999L, trainer.getId()))
                .isInstanceOf(ChapterNotFoundException.class);
    }
}
