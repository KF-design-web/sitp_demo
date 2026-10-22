package com.example.sitp.course.repository;

import com.example.sitp.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTrainerId(Long trainerId);

    List<Course> findByTrainerIdAndId(Long trainerId, Long courseId);

}
