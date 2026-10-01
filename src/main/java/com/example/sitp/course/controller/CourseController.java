package com.example.sitp.course.controller;

import com.example.sitp.course.dto.CourseDetailResponse;
import com.example.sitp.course.dto.CourseResponse;
import com.example.sitp.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
