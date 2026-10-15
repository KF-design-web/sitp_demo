package com.example.sitp.access.service;

import com.example.sitp.access.dto.RedeemResponse;
import com.example.sitp.access.dto.CodeResponse;
import com.example.sitp.access.exception.CodeAlreadyUsedException;
import com.example.sitp.access.exception.CodeNotFoundException;
import com.example.sitp.access.exception.CodeRevokedException;
import com.example.sitp.access.exception.CourseAlreadyHeldException;
import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.common.SecureCodeGenerator;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RedeemableCodeService {

    private final RedeemableCodeRepository codeRepository;
    private final CourseRepository courseRepository;
    private final SecureCodeGenerator codeGenerator;
    private final UserRepository userRepository;

    public RedeemableCodeService(RedeemableCodeRepository codeRepository,
                                  CourseRepository courseRepository,
                                  SecureCodeGenerator codeGenerator,
                                  UserRepository userRepository) {
        this.codeRepository = codeRepository;
        this.courseRepository = courseRepository;
        this.codeGenerator = codeGenerator;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<RedeemableCode> generateCodes(Long courseId, int count) {
        return generateCodes(courseId, count, null);
    }

    @Transactional
    public List<RedeemableCode> generateCodes(Long courseId, int count, Integer validityDays) {
        if (count < 1 || count > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Count must be between 1 and 500.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        Integer timeBox = validityDays;
        if (course.getTargetAudience() == Audience.INTERN) {
            if (timeBox == null || timeBox < 1 || timeBox > 3650) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Intern codes need a validity between 1 and 3650 days.");
            }
        } else {
            timeBox = null;
        }

        List<RedeemableCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String codeString;
            do {
                codeString = codeGenerator.generate(course.getTitle());
            } while (codeRepository.findByCode(codeString).isPresent());

            codes.add(RedeemableCode.builder()
                    .code(codeString)
                    .course(course)
                    .validityDays(timeBox)
                    .status(RedeemableCodeStatus.ACTIVE)
                    .build());
        }

        return codeRepository.saveAll(codes);
    }

    @Transactional(readOnly = true)
    public List<RedeemableCode> listCodesForCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);
        return codeRepository.findByCourseId(course.getId());
    }

    @Transactional
    public RedeemResponse redeem(String codeString, Long userId) {
        RedeemableCode code = codeRepository.findByCode(codeString)
                .orElseThrow(CodeNotFoundException::new);

        if (code.getStatus() == RedeemableCodeStatus.REVOKED) {
            throw new CodeRevokedException();
        }

        if (code.getClaimant() != null) {
            throw new CodeAlreadyUsedException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Not authenticated."));

        if (user.getAccountType() == AccountType.OUTSIDER
                && coversTheUsersHeldCourse(user, code.getCourse())) {
            throw new CourseAlreadyHeldException();
        }

        code.setClaimant(user);
        code.setRedeemedAt(LocalDateTime.now());

        RedeemableCode saved = codeRepository.save(code);

        return RedeemResponse.of(
                saved.getCourse().getId(),
                saved.getCourse().getTitle(),
                computeAccessUntil(user, saved)
        );
    }

    private boolean coversTheUsersHeldCourse(User user, Course course) {
        return !codeRepository
                .findByClaimantIdAndCourseIdAndStatus(user.getId(), course.getId(),
                        RedeemableCodeStatus.ACTIVE)
                .isEmpty();
    }

    @Transactional
    public CodeResponse revoke(String codeString) {
        RedeemableCode code = codeRepository.findByCode(codeString)
                .orElseThrow(CodeNotFoundException::new);

        if (code.getStatus() == RedeemableCodeStatus.REVOKED) {
            throw new CodeRevokedException();
        }

        code.setStatus(RedeemableCodeStatus.REVOKED);
        RedeemableCode saved = codeRepository.save(code);
        return CodeResponse.fromEntity(saved);
    }

    private String computeAccessUntil(User user, RedeemableCode code) {
        if (user.getAccountType() == AccountType.OUTSIDER || code.getValidityDays() == null) {
            return "lifetime";
        }
        LocalDateTime expiresAt = code.getRedeemedAt().plusDays(code.getValidityDays());
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
        return daysLeft + " days left";
    }
}
