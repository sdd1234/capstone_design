package com.campusfit.api.timemanagement.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.timemanagement.dto.ActivityAllocationResponse;
import com.campusfit.api.timemanagement.service.ActivityRecommenderService;
import com.campusfit.api.timemanagement.service.AppliedAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시간 관리 - 활동 배치 추천", description = "룰 기반: 활동 목표를 빈 시간에 자동 배치")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/time-management/recommendations")
@RequiredArgsConstructor
public class ActivityRecommendationController {

    private final ActivityRecommenderService recommender;

    @Operation(summary = "활동 배치 추천", description = "사용자의 활동 목표(WINDOW + 해당 주 미적용)를 빈 시간 슬롯에 자동 배치. weekStart 미지정 시 현재 주.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityAllocationResponse>>> recommend(
            @Parameter(description = "학년도", example = "2026") @RequestParam Integer year,
            @Parameter(description = "학기") @RequestParam TermSeason termSeason,
            @Parameter(description = "주 시작일(월요일). 미지정 시 현재 주") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        LocalDate week = weekStart != null ? weekStart : AppliedAllocationService.currentWeekStart();
        return ResponseEntity.ok(ApiResponse.ok(recommender.recommend(userId, year, termSeason, week)));
    }
}
