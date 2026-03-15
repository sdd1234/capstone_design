package com.campusfit.api.timetable.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.timetable.dto.*;
import com.campusfit.api.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "시간표 관리", description = "시간표 생성·조회·강의 추가·삭제 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @Operation(summary = "시간표 생성", description = "학기별 시간표를 새로 생성합니다. name, year, termSeason(SPRING/FALL/SUMMER/WINTER)을 지정합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<TimetableResponse>> create(
            @Valid @RequestBody TimetableCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(timetableService.create(userId, request)));
    }

    @Operation(summary = "시간표 강의 추가·제거", description = "addLectureIds로 강의를 추가하고 removeLectureIds로 제거합니다. 강의 추가 시 시간 충돌을 자동으로 검증합니다.")
    @PatchMapping("/{timetableId}")
    public ResponseEntity<ApiResponse<TimetableResponse>> patch(
            @Parameter(description = "시간표 ID") @PathVariable Long timetableId,
            @RequestBody TimetablePatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(timetableService.patch(userId, timetableId, request)));
    }

    @Operation(summary = "시간표 목록 조회", description = "내가 만든 시간표 전체 목록과 각 시간표의 강의 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TimetableResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(timetableService.list(userId)));
    }

    @Operation(summary = "시간표 상세 조회", description = "특정 시간표에 담긴 강의 목록과 총 학점을 조회합니다.")
    @GetMapping("/{timetableId}")
    public ResponseEntity<ApiResponse<TimetableResponse>> get(
            @Parameter(description = "시간표 ID") @PathVariable Long timetableId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(timetableService.get(userId, timetableId)));
    }

    @Operation(summary = "시간표 삭제", description = "시간표와 담긴 강의 항목을 전체 삭제합니다.")
    @DeleteMapping("/{timetableId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "시간표 ID") @PathVariable Long timetableId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        timetableService.delete(userId, timetableId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
