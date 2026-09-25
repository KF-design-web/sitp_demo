package com.example.sitp.course.service;

import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {

        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> getAll() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::fromEntity)
                .toList();
    }
}
