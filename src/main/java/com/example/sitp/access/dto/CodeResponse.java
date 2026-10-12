package com.example.sitp.access.dto;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "One redeemable code — as minted, or as read in the codes list.")
public class CodeResponse {

    @Schema(description = "The code's database ID.")
    private final Long id;

    @Schema(description = "The code string (label + hyphen + 8 random alphanumerics).")
    private final String code;

    @Schema(description = "The course this code opens.")
    private final Long courseId;

    @Schema(description = "The course title.")
    private final String courseTitle;

    @Schema(description = "The code status: ACTIVE or REVOKED.")
    private final RedeemableCodeStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "When the code was redeemed (null until redeemed).")
    private final LocalDateTime redeemedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Username of the person who redeemed the code (null until redeemed).")
    private final String claimantUsername;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "When the code row was created.")
    private final LocalDateTime createdAt;

    public static CodeResponse fromEntity(RedeemableCode code) {
        return CodeResponse.builder()
                .id(code.getId())
                .code(code.getCode())
                .courseId(code.getCourse().getId())
                .courseTitle(code.getCourse().getTitle())
                .status(code.getStatus())
                .redeemedAt(code.getRedeemedAt())
                .claimantUsername(code.getClaimant() != null ? code.getClaimant().getUsername() : null)
                .createdAt(code.getCreatedAt())
                .build();
    }
}
