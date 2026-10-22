package com.example.sitp.course.service;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.course.CourseAccessGuard;
import com.example.sitp.course.dto.CourseDetailResponse;
import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.dto.ChapterRequest;
import com.example.sitp.course.dto.ChapterResponse;
import com.example.sitp.course.dto.CreateCourseRequest;
import com.example.sitp.course.dto.MyCourseResponse;
import com.example.sitp.course.dto.UpdateCourseRequest;
import com.example.sitp.course.exception.CourseDeletionBlockedException;
import com.example.sitp.course.exception.ChapterNotFoundException;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.exception.CourseOwnershipException;
import com.example.sitp.course.model.Chapter;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.ChapterRepository;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final CourseAccessGuard guard;
    private final RedeemableCodeRepository redeemableCodeRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository,
                         ChapterRepository chapterRepository,
                         CourseAccessGuard guard,
                         RedeemableCodeRepository redeemableCodeRepository,
                         UserRepository userRepository) {

        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.guard = guard;
        this.redeemableCodeRepository = redeemableCodeRepository;
        this.userRepository = userRepository;
    }

    public List<CourseResponse> getAll() {
        boolean priceVisible = isKnockerAMember();

        return courseRepository.findAll().stream()
                .map(course -> CourseResponse.fromEntity(course, priceVisible))
                .toList();
    }

    public CourseDetailResponse getById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        Long memberId = knockerMemberId();
        if (!guard.canAccess(memberId, course.getId())) {
            throw new com.example.sitp.common.NoAccessGrantedException();
        }

        boolean priceVisible = memberId != null;
        List<com.example.sitp.course.model.Chapter> chapters =
                chapterRepository.findByCourseIdOrderByPositionAsc(courseId);

        return CourseDetailResponse.from(course, chapters, priceVisible);
    }

    public List<MyCourseResponse> myCourses(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }

        List<RedeemableCode> codes = redeemableCodeRepository
                .findByClaimantIdAndStatus(userId, RedeemableCodeStatus.ACTIVE);

        List<MyCourseResponse> result = new ArrayList<>();
        for (RedeemableCode code : codes) {
            Course course = code.getCourse();
            boolean revoked = code.getStatus() == RedeemableCodeStatus.REVOKED;
            boolean expired = isExpired(code);
            boolean valid = !revoked && !expired;

            result.add(MyCourseResponse.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .targetAudience(course.getTargetAudience().name())
                    .codeStatus(code.getStatus().name())
                    .accessUntil(computeAccessUntil(user, code))
                    .accessValid(valid)
                    .build());
        }
        return result;
    }

    public CourseResponse createCourse(CreateCourseRequest request, Long trainerId) {
        User trainer = userRepository.findById(trainerId).orElseThrow();

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .labels(request.getLabels())
                .thumbnailUrl(request.getThumbnailUrl())
                .price(request.getPrice())
                .targetAudience(request.getTargetAudience())
                .trainer(trainer)
                .build();

        return CourseResponse.fromEntity(courseRepository.save(course), true);
    }

    public List<CourseResponse> getMyCourses(Long trainerId) {
        return courseRepository.findByTrainerId(trainerId).stream()
                .map(course -> CourseResponse.fromEntity(course, true))
                .toList();
    }

    public Course getCourseForTrainer(Long courseId, Long trainerId) {
        List<Course> owned = courseRepository.findByTrainerIdAndId(trainerId, courseId);
        if (!owned.isEmpty()) {
            return owned.get(0);
        }
        if (courseRepository.existsById(courseId)) {
            throw new CourseOwnershipException();
        }
        throw new CourseNotFoundException();
    }

    @Transactional
    public CourseResponse updateCourse(Long courseId, Long trainerId, UpdateCourseRequest request) {
        Course course = getCourseForTrainer(courseId, trainerId);

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setLabels(request.getLabels());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());

        return CourseResponse.fromEntity(courseRepository.save(course), true);
    }

    @Transactional
    public void deleteCourse(Long courseId, Long trainerId) {
        Course course = getCourseForTrainer(courseId, trainerId);

        if (!redeemableCodeRepository.findByCourseId(courseId).isEmpty()) {
            throw new CourseDeletionBlockedException();
        }
        chapterRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }

    public CourseDetailResponse getTrainerCourseDetail(Long courseId, Long trainerId) {
        Course course = getCourseForTrainer(courseId, trainerId);

        List<com.example.sitp.course.model.Chapter> chapters =
                chapterRepository.findByCourseIdOrderByPositionAsc(courseId);

        return CourseDetailResponse.from(course, chapters, true);
    }

    @Transactional
    public ChapterResponse createChapter(Long courseId, Long trainerId, ChapterRequest request) {
        Course course = getCourseForTrainer(courseId, trainerId);

        Chapter last = chapterRepository.findFirstByCourseIdOrderByPositionDesc(courseId);
        int position = request.getPosition() != null
                ? request.getPosition()
                : (last != null ? last.getPosition() + 1 : 1);

        Chapter chapter = Chapter.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .labels(request.getLabels())
                .thumbnailUrl(request.getThumbnailUrl())
                .videoUrl(request.getVideoUrl())
                .position(position)
                .course(course)
                .build();

        return ChapterResponse.fromEntity(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterResponse updateChapter(Long chapterId, Long trainerId, ChapterRequest request) {
        Chapter chapter = chapterForTrainer(chapterId, trainerId);

        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        chapter.setLabels(request.getLabels());
        chapter.setThumbnailUrl(request.getThumbnailUrl());
        chapter.setVideoUrl(request.getVideoUrl());
        if (request.getPosition() != null) {
            chapter.setPosition(request.getPosition());
        }

        return ChapterResponse.fromEntity(chapterRepository.save(chapter));
    }

    @Transactional
    public void deleteChapter(Long chapterId, Long trainerId) {
        Chapter chapter = chapterForTrainer(chapterId, trainerId);

        chapterRepository.delete(chapter);
    }

    private Chapter chapterForTrainer(Long chapterId, Long trainerId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(ChapterNotFoundException::new);

        User trainer = chapter.getCourse().getTrainer();
        if (trainer == null || !trainer.getId().equals(trainerId)) {
            if (courseRepository.existsById(chapter.getCourse().getId())) {
                throw new CourseOwnershipException();
            }
            throw new CourseNotFoundException();
        }
        return chapter;
    }

    private boolean isExpired(RedeemableCode code) {
        if (code.getValidityDays() == null || code.getRedeemedAt() == null) {
            return false;
        }
        return code.getRedeemedAt().plusDays(code.getValidityDays()).isBefore(LocalDateTime.now());
    }

    private String computeAccessUntil(User user, RedeemableCode code) {
        if (user.getAccountType() == AccountType.OUTSIDER || code.getValidityDays() == null) {
            return "lifetime";
        }
        LocalDateTime expiresAt = code.getRedeemedAt().plusDays(code.getValidityDays());
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
        return Math.max(daysLeft, 0) + " days left";
    }

    private boolean isKnockerAMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long;
    }

    private Long knockerMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof Long id ? id : null;
    }
}
