package com.example.sitp.course.controller;

import com.example.sitp.common.NoAccessGrantedException;
import com.example.sitp.course.dto.ChapterRequest;
import com.example.sitp.course.dto.ChapterResponse;
import com.example.sitp.course.dto.CourseDetailResponse;
import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.dto.CreateCourseRequest;
import com.example.sitp.course.dto.MyCourseResponse;
import com.example.sitp.course.dto.UpdateCourseRequest;
import com.example.sitp.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {

        this.courseService = courseService;
    }

    @Operation(summary = "List all courses in the public catalog")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The catalog answers - an empty catalog answers []S never 404.")
    })
    @GetMapping
    public List<CourseResponse> getAll() {

        return courseService.getAll();
    }

    @Operation(summary = "The logged-in user's courses (from their claimed redeemable codes)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The user's courses with code status and access expiry."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in.")
    })
    @GetMapping("/mine")
    public List<MyCourseResponse> myCourses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        Long userId = (Long) auth.getPrincipal();
        return courseService.myCourses(userId);
    }

    @Operation(summary = "One course's full detail with its chapters in teaching order (logged-in members only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The detail card: course fields + chapters ordered by position; the price line present only for a logged-in viewer."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - the building's lock speaks first (not signed in)."),
            @ApiResponse(responseCode = "404",
                    description = "No course carries this number (thrown by the cook).")
    })
    @GetMapping("/{id}")
    public CourseDetailResponse getById(@PathVariable Long id) {

        return courseService.getById(id);
    }

    @Operation(summary = "A trainer creates a course of their own (Trainer role only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "The course is created with the trainer set from the session."),
            @ApiResponse(responseCode = "400",
                    description = "A form field failed its check (missing title, unknown audience, bad price)."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "The knocker is not a trainer.")
    })
    @PostMapping("/trainer")
    public ResponseEntity<CourseResponse> createCourse(@Valid @ModelAttribute CreateCourseRequest request) {
        Long trainerId = requireTrainerId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(request, trainerId));
    }

    @Operation(summary = "The logged-in trainer's own courses (Trainer role only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Only the caller's own courses - never another trainer's."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "The knocker is not a trainer.")
    })
    @GetMapping("/trainer/mine")
    public List<CourseResponse> myCreatedCourses() {

        return courseService.getMyCourses(requireTrainerId());
    }

    @Operation(summary = "A trainer reads their OWN course with its episodes (owner-only; the studio's library view)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The detail card with the episodes in teaching order - no redeemable code asked of the owner."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "Not a trainer - or the course belongs to another trainer."),
            @ApiResponse(responseCode = "404",
                    description = "No course carries this number.")
    })
    @GetMapping("/trainer/{id}")
    public CourseDetailResponse trainerCourseDetail(@PathVariable Long id) {

        return courseService.getTrainerCourseDetail(id, requireTrainerId());
    }

    @Operation(summary = "A trainer adds one episode (chapter) to their own course (owner-only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "The episode is created (last in teaching order unless a position was posted)."),
            @ApiResponse(responseCode = "400",
                    description = "A form field failed its check."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "Not a trainer - or the course belongs to another trainer."),
            @ApiResponse(responseCode = "404",
                    description = "No course carries this number.")
    })
    @PostMapping("/{id}/chapters")
    public ResponseEntity<ChapterResponse> createChapter(@PathVariable Long id,
                                                         @Valid @ModelAttribute ChapterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createChapter(id, requireTrainerId(), request));
    }

    @Operation(summary = "A trainer updates their own course (owner-only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "The updated course card."),
            @ApiResponse(responseCode = "400",
                    description = "A form field failed its check."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "Not a trainer - or the course belongs to another trainer."),
            @ApiResponse(responseCode = "404",
                    description = "No course carries this number.")
    })
    @PutMapping("/{id}")
    public CourseResponse updateCourse(@PathVariable Long id,
                                       @Valid @ModelAttribute UpdateCourseRequest request) {

        return courseService.updateCourse(id, requireTrainerId(), request);
    }

    @Operation(summary = "A trainer deletes their own course (owner-only; refused while codes are handed out for it)")
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "The course and its chapters are gone."),
            @ApiResponse(responseCode = "401",
                    description = "Anonymous knock - not signed in."),
            @ApiResponse(responseCode = "403",
                    description = "Not a trainer - or the course belongs to another trainer."),
            @ApiResponse(responseCode = "404",
                    description = "No course carries this number."),
            @ApiResponse(responseCode = "409",
                    description = "Redeemable codes were handed out for this course - deletion is refused.")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id, requireTrainerId());

        return ResponseEntity.noContent().build();
    }

    private Long requireTrainerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated.");
        }
        var principal = auth.getPrincipal();
        boolean isTrainer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRAINER"));
        if (!isTrainer) {
            throw new NoAccessGrantedException();
        }
        return (Long) principal;
    }
}
