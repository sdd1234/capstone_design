package com.campusfit.api.academic.controller;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.academic.dto.PrerequisiteResponse;
import com.campusfit.api.academic.service.LectureService;
import com.campusfit.api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @GetMapping("/api/v1/lectures")
    public ResponseEntity<ApiResponse<List<LectureResponse>>> search(
            @RequestParam Long universityId,
            @RequestParam Integer year,
            @RequestParam String termSeason,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(lectureService.search(universityId, year, termSeason, keyword)));
    }

    @GetMapping("/api/v1/courses/{courseId}/prerequisites")
    public ResponseEntity<ApiResponse<List<PrerequisiteResponse>>> getPrerequisites(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(lectureService.getPrerequisites(courseId)));
    }
}
