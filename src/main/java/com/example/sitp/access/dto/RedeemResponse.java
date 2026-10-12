package com.example.sitp.access.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "The confirmation card after a successful redeem.")
public class RedeemResponse {

    @Schema(description = "The course this code opens.")
    private final Long courseId;

    @Schema(description = "The course title.")
    private final String courseTitle;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "When the access expires (null = lifetime).")
    private final String accessUntil;

    public static RedeemResponse of(Long courseId, String courseTitle, String accessUntil) {
        return RedeemResponse.builder()
                .courseId(courseId)
                .courseTitle(courseTitle)
                .accessUntil(accessUntil)
                .build();
    }
}
