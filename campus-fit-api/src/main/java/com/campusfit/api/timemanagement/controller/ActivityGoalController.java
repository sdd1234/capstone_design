package com.campusfit.api.timemanagement.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.timemanagement.dto.ActivityGoalCreateRequest;
import com.campusfit.api.timemanagement.dto.ActivityGoalResponse;
import com.campusfit.api.timemanagement.dto.ActivityGoalUpdateRequest;
import com.campusfit.api.timemanagement.service.ActivityGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시간 관리 - 활동 목표", description = "주간 반복 활동 목표 등록·조회·수정·삭제 (운동/공부/식사 등)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/time-management/goals")
@RequiredArgsConstructor
public class ActivityGoalController {

    private final ActivityGoalService goalService;

    @Operation(summary = "활동 목표 생성", description = "주간 반복 활동 목표를 등록합니다. 선호 요일/시간은 모두 선택 입력. year+termSeason 제공 시 해당 학기 강의와 시간 충돌 검증.")
    @PostMapping
    public ResponseEntity<ApiResponse<ActivityGoalResponse>> create(
            @Valid @RequestBody ActivityGoalCreateRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) TermSeason termSeason,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        ActivityGoalResponse created = goalService.create(userId, request, year, termSeason);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @Operation(summary = "활동 목표 목록", description = "내 활동 목표를 최신순으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityGoalResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(goalService.list(userId)));
    }

    @Operation(summary = "활동 목표 부분 수정", description = "전달된 필드만 갱신합니다.")
    @PatchMapping("/{goalId}")
    public ResponseEntity<ApiResponse<ActivityGoalResponse>> update(
            @Parameter(description = "활동 목표 ID") @PathVariable Long goalId,
            @Valid @RequestBody ActivityGoalUpdateRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) TermSeason termSeason,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(goalService.update(userId, goalId, request, year, termSeason)));
    }

    @Operation(summary = "활동 목표 삭제")
    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "활동 목표 ID") @PathVariable Long goalId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        goalService.delete(userId, goalId);
        return ResponseEntity.noContent().build();
    }
}
