package com.example.sitp.course.service;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.course.CourseAccessGuard;
import com.example.sitp.course.dto.CourseDetailResponse;
import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.dto.MyCourseResponse;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.ChapterRepository;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
