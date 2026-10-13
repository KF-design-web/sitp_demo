package com.example.sitp.course;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CourseAccessGuard {

    private final RedeemableCodeRepository redeemableCodeRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public CourseAccessGuard(RedeemableCodeRepository redeemableCodeRepository,
                             UserRepository userRepository,
                             CourseRepository courseRepository) {
        this.redeemableCodeRepository = redeemableCodeRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public boolean canAccess(Long userId, Long courseId) {
        if (userId == null) {
            return false;
        }

        List<RedeemableCode> exact = redeemableCodeRepository
                .findByClaimantIdAndCourseIdAndStatus(userId, courseId, RedeemableCodeStatus.ACTIVE);
        if (exact.stream().anyMatch(this::withinTimeBox)) {
            return true;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getAccountType() != AccountType.INTERN) {
            return false;
        }
        Course knocked = courseRepository.findById(courseId).orElse(null);
        if (knocked == null || knocked.getTargetAudience() != Audience.INTERN) {
            return false;
        }

        return redeemableCodeRepository.findByClaimantIdAndStatus(userId, RedeemableCodeStatus.ACTIVE)
                .stream()
                .filter(code -> code.getCourse().getTargetAudience() == Audience.INTERN)
                .anyMatch(this::withinTimeBox);
    }

    private boolean withinTimeBox(RedeemableCode code) {
        if (code.getValidityDays() == null) {
            return true;
        }
        if (code.getRedeemedAt() == null) {
            return false;
        }
        return code.getRedeemedAt().plusDays(code.getValidityDays()).isAfter(LocalDateTime.now());
    }
}
