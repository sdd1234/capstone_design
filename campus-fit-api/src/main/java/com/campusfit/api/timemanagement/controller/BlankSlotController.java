package com.campusfit.api.timemanagement.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.timemanagement.dto.BlankSlotResponse;
import com.campusfit.api.timemanagement.service.AppliedAllocationService;
import com.campusfit.api.timemanagement.service.BlankSlotService;
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

@Tag(name = "시간 관리 - 빈 슬롯", description = "주간 빈 시간대 분석 (시간표 + 활동 목표 점유 제외)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/time-management/blank-slots")
@RequiredArgsConstructor
public class BlankSlotController {

    private final BlankSlotService blankSlotService;

    @Operation(summary = "주간 빈 슬롯 조회", description = "월~일 09:00-22:00에서 시간표 강의 + FIXED 활동 + (해당 주의) 적용분 제외한 빈 시간대. weekStart 미지정 시 현재 주.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BlankSlotResponse>>> getBlankSlots(
            @Parameter(description = "학년도", example = "2026") @RequestParam Integer year,
            @Parameter(description = "학기") @RequestParam TermSeason termSeason,
            @Parameter(description = "주 시작일(월요일). 미지정 시 현재 주") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        LocalDate week = weekStart != null ? weekStart : AppliedAllocationService.currentWeekStart();
        return ResponseEntity.ok(ApiResponse.ok(blankSlotService.getBlankSlots(userId, year, termSeason, week)));
    }
}
