package com.campusfit.api.academic.controller;

import com.campusfit.api.academic.dto.AcademicCalendarEventResponse;
import com.campusfit.api.academic.service.AcademicCalendarService;
import com.campusfit.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "학사 일정", description = "학교 공식 학사 일정(수강신청·개강·종강·고사기간 등) 조회 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/academic-calendar")
@RequiredArgsConstructor
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    @Operation(summary = "학사 일정 조회", description = "연도·대학교 기준으로 학사 이벤트 목록을 반환합니다.\n\n" +
            "2026년 기준 17개 항목이 초기화되어 있습니다. (수강신청 1·2차, 개강, 종강, 중간·기말고사, 추석 연휴 등)")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<AcademicCalendarEventResponse>>> getEvents(
            @Parameter(description = "연도 (미입력 시 현재 연도)", example = "2026") @RequestParam(required = false) Integer year,
            @Parameter(description = "대학교 ID (계명대학교 = 1)", example = "1") @RequestParam(required = false) Long universityId) {
        int targetYear = (year != null) ? year : java.time.Year.now().getValue();
        return ResponseEntity.ok(ApiResponse.ok(calendarService.getEvents(targetYear, universityId)));
    }
}
