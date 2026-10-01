package com.example.sitp.course.dto;

import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Chapter;
import com.example.sitp.course.model.Course;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "One course's full detail: the catalog card's fields plus its chapters in teaching order. A protected answer - only a logged-in member receives it (the anonymous knock earns the rulebook's 401 first).")
public class CourseDetailResponse {

    @Schema(description = "The course's number (the same one the catalog card showed).")
    private final Long id;

    @Schema(description = "The course name.")
    private final String title;

    @Schema(description = "The full description (B3: never clamped on screens).")
    private final String description;

    @Schema(description = "Who the course is for: INTERN or OUTSIDER.")
    private final Audience targetAudience;

    @Schema(description = "The course price - the SAME gate rule as the catalog card (Phase B): only present for a logged-in viewer, and only when the course carries one.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final BigDecimal price;

    @Schema(description = "Whether THIS viewer may see prices - the display rule itself.")
    private final boolean priceVisible;

    @Schema(description = "The chapters, smallest position first - the teaching order IS the data (Phase A).")
    private final List<ChapterCard> chapters;

    public static CourseDetailResponse from(Course course, List<Chapter> chapters, boolean priceVisible) {
        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .targetAudience(course.getTargetAudience())
                .price(priceVisible ? course.getPrice() : null)
                .priceVisible(priceVisible)
                .chapters(chapters.stream().map(ChapterCard::fromEntity).toList())
                .build();
    }
}
