package com.campusfit.api.academic.controller;

import com.campusfit.api.academic.dto.AcademicCalendarEventResponse;
import com.campusfit.api.academic.service.AcademicCalendarService;
import com.campusfit.api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-calendar")
@RequiredArgsConstructor
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<AcademicCalendarEventResponse>>> getEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long universityId) {
        int targetYear = (year != null) ? year : java.time.Year.now().getValue();
        return ResponseEntity.ok(ApiResponse.ok(calendarService.getEvents(targetYear, universityId)));
    }
}
