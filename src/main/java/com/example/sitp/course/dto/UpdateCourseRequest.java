package com.example.sitp.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Form fields a trainer posts to update one of their own courses.")
public class UpdateCourseRequest {

    @Schema(description = "The course name shown on the noticeboard.")
    @NotBlank(message = "Title is required.")
    private String title;

    @Schema(description = "A short text about what the course teaches.")
    private String description;

    @Schema(description = "Free-text tags, comma-separated (e.g. \"git, branching, tutorial\").")
    private String labels;

    @Schema(description = "A cover image link for the course card.")
    private String thumbnailUrl;

    @Schema(description = "The course price (optional — an intern course can stay free).")
    @DecimalMin(value = "0.00", message = "Price must be zero or more.")
    private BigDecimal price;

}
