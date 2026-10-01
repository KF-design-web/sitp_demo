package com.example.sitp.course.dto;

import com.example.sitp.course.model.Chapter;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "One chapter on a course's detail card - a dot on the map: its order, its name, and where its video LIVES (a reference, not a file).")
public class ChapterCard {

    @Schema(description = "The chapter's number.")
    private final Long id;

    @Schema(description = "The chapter's name, in teaching order.")
    private final String title;

    @Schema(description = "The teaching-order number (1, 2, 3, ...). The order is DATA, not database luck.")
    private final Integer position;

    @Schema(description = "WHERE the video lives (a reference the video provider understands) - the app stores the catalog entry, never the film itself (requirements §6). Playback signing arrives with the video provider in Slice 6.")
    private final String videoUrl;

    public static ChapterCard fromEntity(Chapter chapter) {
        return ChapterCard.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .position(chapter.getPosition())
                .videoUrl(chapter.getVideoUrl())
                .build();
    }
}
