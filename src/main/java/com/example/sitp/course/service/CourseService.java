package com.example.sitp.course.service;

import com.example.sitp.course.CourseAccessGuard;
import com.example.sitp.course.dto.CourseDetailResponse;
import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.ChapterRepository;
import com.example.sitp.course.repository.CourseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final CourseAccessGuard guard;

    public CourseService(CourseRepository courseRepository,
                         ChapterRepository chapterRepository,
                         CourseAccessGuard guard) {

        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.guard = guard;
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
