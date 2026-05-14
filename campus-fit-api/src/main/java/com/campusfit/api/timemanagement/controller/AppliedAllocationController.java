package com.campusfit.api.timemanagement.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.timemanagement.dto.AppliedAllocationResponse;
import com.campusfit.api.timemanagement.dto.ApplyAllocationsRequest;
import com.campusfit.api.timemanagement.dto.WeeklySummaryResponse;
import com.campusfit.api.timemanagement.service.AppliedAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시간 관리 - 적용된 배치", description = "추천을 시간표에 영구 반영 + 주별 달성률 추적")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/time-management/applied-allocations")
@RequiredArgsConstructor
public class AppliedAllocationController {

    private final AppliedAllocationService service;

    @Operation(summary = "주별 적용된 배치 목록", description = "weekStart 미지정 시 현재 주(월요일 기준)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppliedAllocationResponse>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.listForWeek(userId, weekStart)));
    }

    @Operation(summary = "전체 history 조회", description = "모든 주의 적용분 — debugging/admin용")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AppliedAllocationResponse>>> listAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.listAll(userId)));
    }

    @Operation(summary = "주별 달성률 summary", description = "활동별 적용/완료 시간과 % — \"이번 주 운동 3/3시간 (100%)\"")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<WeeklySummaryResponse>> weeklySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.weeklySummary(userId, weekStart)));
    }

    @Operation(summary = "추천 일괄 적용 — (weekStart, goalId) 단위 replace")
    @PutMapping
    public ResponseEntity<ApiResponse<List<AppliedAllocationResponse>>> applyAll(
            @Valid @RequestBody ApplyAllocationsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.applyAll(userId, request)));
    }

    @Operation(summary = "완료 토글", description = "체크/체크해제 — 주간 달성률 집계 키")
    @PatchMapping("/{allocationId}/complete")
    public ResponseEntity<ApiResponse<AppliedAllocationResponse>> toggleCompleted(
            @PathVariable Long allocationId,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ResponseEntity.ok(ApiResponse.ok(service.toggleCompleted(userId, allocationId, completed)));
    }

    @Operation(summary = "특정 주 wipe", description = "한 주의 모든 적용분 삭제 (한 주 다시 짜기)")
    @DeleteMapping("/week")
    public ResponseEntity<Void> clearWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        service.clearWeek(userId, weekStart);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 history wipe — 모든 주 삭제")
    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAll(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        service.clearAll(userId);
        return ResponseEntity.noContent().build();
    }
}
