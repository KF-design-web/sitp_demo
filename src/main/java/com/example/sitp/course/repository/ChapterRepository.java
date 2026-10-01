package com.example.sitp.course.repository;

import com.example.sitp.course.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByCourseIdOrderByPositionAsc(Long courseId);
}
