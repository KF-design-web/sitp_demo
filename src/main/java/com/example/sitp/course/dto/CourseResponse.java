package com.example.sitp.course.dto;

import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "The course price. ONLY present for a logged-in viewer (and only when the course carries one) - a logged-out visitor never receives a price line.")
    private final BigDecimal price;

    @Schema(description = "Whether THIS viewer is allowed to see prices (logged in) - the display rule itself, so the frontend never has to guess.")
    private final boolean priceVisible;

    public static CourseResponse fromEntity(Course course, boolean priceVisible) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .targetAudience(course.getTargetAudience())
                .price(priceVisible ? course.getPrice() : null)
                .priceVisible(priceVisible)
                .build();
    }
}
