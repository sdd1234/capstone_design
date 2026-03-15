package com.campusfit.api.timetablepreference.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.timetablepreference.dto.TimetablePreferenceRequest;
import com.campusfit.api.timetablepreference.dto.TimetablePreferenceResponse;
import com.campusfit.api.timetablepreference.service.TimetablePreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시간표 선호 설정", description = "AI 추천에 사용할 선호 조건(목표학점·기피시간·희망과목 등) 저장·조회 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/timetable-preferences")
@RequiredArgsConstructor
public class TimetablePreferenceController {

    private final TimetablePreferenceService preferenceService;

    @Operation(summary = "선호 설정 저장 (upsert)", description = "AI 추천 조건을 저장합니다. 이미 존재하면 덮어씁니다.\n\n" +
            "- creditPolicy: 목표 학점·최대 학점\n" +
            "- preferenceOption: 오전 제외 여부(excludeMorning), 최대 등교일수(maxDaysPerWeek)\n" +
            "- timeRanges: 선호(PREFER)/기피(AVOID) 시간대 설정\n" +
            "- desiredCourses: 희망 수강 과목 ID 목록")
    @PutMapping
    public ResponseEntity<ApiResponse<TimetablePreferenceResponse>> save(
            @Valid @RequestBody TimetablePreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(preferenceService.save(userId, request)));
    }

    @Operation(summary = "선호 설정 조회", description = "저장된 AI 추천 조건을 조회합니다. 설정이 없으면 404를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<TimetablePreferenceResponse>> get(
            @Parameter(description = "연도", example = "2026") @RequestParam Integer year,
            @Parameter(description = "학기 (SPRING / FALL / SUMMER / WINTER)", example = "SPRING") @RequestParam String termSeason,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(preferenceService.get(userId, year, termSeason)));
    }
}
