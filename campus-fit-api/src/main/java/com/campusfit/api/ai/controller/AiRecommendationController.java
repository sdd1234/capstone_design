package com.campusfit.api.ai.controller;

import com.campusfit.api.ai.dto.RecommendationRequest;
import com.campusfit.api.ai.dto.RecommendationResponse;
import com.campusfit.api.ai.service.AiRecommendationService;
import com.campusfit.api.common.dto.ApiResponse;
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

@Tag(name = "AI 시간표 추천", description = "선호 설정 기반 시간표 자동 추천·조회·삭제 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/ai/timetable/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService recommendationService;

    @Operation(summary = "AI 추천 생성", description = "선호 설정(시간표 선호 설정 API)을 기반으로 충돌 없는 시간표 후보를 최대 3개 자동 생성합니다.\n\n" +
            "- preferredLectureIds: 우선 포함할 강의 ID 목록 (선택)\n" +
            "- excludeLectureIds: 제외할 강의 ID 목록 (선택)")
    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> create(
            @Valid @RequestBody RecommendationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(recommendationService.create(userId, request)));
    }

    @Operation(summary = "AI 추천 목록 조회", description = "내가 요청한 AI 추천 결과 목록을 최신순으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.list(userId)));
    }

    @Operation(summary = "AI 추천 상세 조회", description = "특정 추천 결과의 후보 시간표와 각 강의 구성을 상세히 조회합니다.")
    @GetMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> get(
            @Parameter(description = "추천 결과 ID") @PathVariable Long recommendationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.get(userId, recommendationId)));
    }

    @Operation(summary = "AI 추천 삭제", description = "특정 추천 결과를 삭제합니다.")
    @DeleteMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "추천 결과 ID") @PathVariable Long recommendationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        recommendationService.delete(userId, recommendationId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
