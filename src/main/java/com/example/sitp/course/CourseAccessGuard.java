package com.example.sitp.course;

import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import org.springframework.stereotype.Component;

@Component
public class CourseAccessGuard {

    private final RedeemableCodeRepository redeemableCodeRepository;

    public CourseAccessGuard(RedeemableCodeRepository redeemableCodeRepository) {
        this.redeemableCodeRepository = redeemableCodeRepository;
    }

    public boolean canAccess(Long userId, Long courseId) {
        if (userId == null) {
            return false;
        }
        return redeemableCodeRepository
                .existsByClaimantIdAndCourseIdAndStatus(userId, courseId, RedeemableCodeStatus.ACTIVE);
    }
}
