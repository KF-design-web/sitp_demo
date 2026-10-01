package com.example.sitp.course;

import org.springframework.stereotype.Component;

@Component
public class CourseAccessGuard {

    public boolean canAccess(Long userId, Long courseId) {
        return true;
    }
}
