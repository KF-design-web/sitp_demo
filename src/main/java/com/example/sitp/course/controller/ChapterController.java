package com.example.sitp.course.controller;

import com.example.sitp.common.NoAccessGrantedException;
import com.example.sitp.course.dto.ChapterRequest;
import com.example.sitp.course.dto.ChapterResponse;
import com.example.sitp.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final CourseService courseService;

    public ChapterController(CourseService courseService) {

        this.courseService = courseService;
    }

    @Operation(summary = "A trainer updates one episode of their own course (owner-only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The updated episode card."),
            @ApiResponse(responseCode = "400",
                    description = "A form field failed its check."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "The episode's course belongs to another trainer."),
            @ApiResponse(responseCode = "404",
                    description = "No episode carries this number.")
    })
    @PutMapping("/{id}")
    public ChapterResponse updateChapter(@PathVariable Long id,
                                         @Valid @ModelAttribute ChapterRequest request) {

        return courseService.updateChapter(id, requireTrainerId(), request);
    }

    @Operation(summary = "A trainer deletes one episode of their own course (owner-only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "The episode is gone (it never carried codes - no refusal here)."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "The episode's course belongs to another trainer."),
            @ApiResponse(responseCode = "404",
                    description = "No episode carries this number.")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        courseService.deleteChapter(id, requireTrainerId());

        return ResponseEntity.noContent().build();
    }

    private Long requireTrainerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        boolean isTrainer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRAINER"));
        if (!isTrainer) {
            throw new NoAccessGrantedException();
        }
        return (Long) auth.getPrincipal();
    }
}
