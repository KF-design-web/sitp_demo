package com.example.sitp.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "One course the logged-in user has access to, with its code status and access expiry.")
public class MyCourseResponse {

    @Schema(description = "The course's number.")
    private final Long courseId;

    @Schema(description = "The course title.")
    private final String courseTitle;

    @Schema(description = "Who the course is for: INTERN or OUTSIDER.")
    private final String targetAudience;

    @Schema(description = "The code status: ACTIVE or REVOKED.")
    private final String codeStatus;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "When the access expires (null = lifetime).")
    private final String accessUntil;

    @Schema(description = "Whether the access is currently valid (not expired, not revoked).")
    private final boolean accessValid;
}
