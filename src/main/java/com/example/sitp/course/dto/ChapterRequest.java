package com.example.sitp.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Form fields a trainer posts to add one episode (chapter) to one of their own courses.")
public class ChapterRequest {

    @Schema(description = "The episode's name.")
    @NotBlank(message = "Title is required.")
    private String title;

    @Schema(description = "What the episode teaches.")
    private String description;

    @Schema(description = "Free-text tags, comma-separated (e.g. \"git, branching, tutorial\").")
    private String labels;

    @Schema(description = "WHERE the video lives (a link today — Bunny Stream fills the same field when it lands).")
    private String videoUrl;

    @Schema(description = "A cover image link for the episode card.")
    private String thumbnailUrl;

    @Schema(description = "The teaching-order number (optional — a new episode goes last unless you set this).")
    private Integer position;

}
