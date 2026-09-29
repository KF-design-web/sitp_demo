package com.example.sitp.course.service;

import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.repository.CourseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {

        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> getAll() {
        boolean priceVisible = isKnockerAMember();

        return courseRepository.findAll().stream()
                .map(course -> CourseResponse.fromEntity(course, priceVisible))
                .toList();
    }

    private boolean isKnockerAMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long;
    }
}
