package com.example.sitp.course.dto;

import com.example.sitp.course.model.Chapter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "One episode (chapter) as the trainer studio reads it: order, text, tags, cover, video link.")
public class ChapterResponse {

    @Schema(description = "The episode's number.")
    private final Long id;

    @Schema(description = "Which course the episode belongs to.")
    private final Long courseId;

    @Schema(description = "The episode's name.")
    private final String title;

    @Schema(description = "What the episode teaches.")
    private final String description;

    @Schema(description = "The episode's free-text tags.")
    private final String labels;

    @Schema(description = "WHERE the video lives (a reference, not a file — requirements §6).")
    private final String videoUrl;

    @Schema(description = "The cover image link for the episode card.")
    private final String thumbnailUrl;

    @Schema(description = "The teaching-order number (1, 2, 3, ...).")
    private final Integer position;

    public static ChapterResponse fromEntity(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourse().getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .labels(chapter.getLabels())
                .videoUrl(chapter.getVideoUrl())
                .thumbnailUrl(chapter.getThumbnailUrl())
                .position(chapter.getPosition())
                .build();
    }

}
