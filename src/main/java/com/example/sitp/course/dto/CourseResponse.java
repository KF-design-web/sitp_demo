package com.example.sitp.course.dto;

import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "One course in the public catalog - the display card a visitor sees without logging in.")
public class CourseResponse {

    @Schema(description = "The course's number - the list shows it so a later request can ask for ONE course by number (Slice 3).")
    private final Long id;

    @Schema(description = "The course name shown on the noticeboard.")
    private final String title;

    @Schema(description = "A short text about what the course teaches.")
    private final String description;

    @Schema(description = "Who the course is for: INTERN (company interns) or OUTSIDER (pays per course).")
    private final Audience targetAudience;

    public static CourseResponse fromEntity(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .targetAudience(course.getTargetAudience())
                .build();
    }
}
