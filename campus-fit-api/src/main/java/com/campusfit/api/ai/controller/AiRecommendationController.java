package com.campusfit.api.ai.controller;

import com.campusfit.api.ai.dto.RecommendationRequest;
import com.campusfit.api.ai.dto.RecommendationResponse;
import com.campusfit.api.ai.service.AiRecommendationService;
import com.campusfit.api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/timetable/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> create(
            @Valid @RequestBody RecommendationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(recommendationService.create(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.list(userId)));
    }

    @GetMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> get(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.get(userId, recommendationId)));
    }

    @DeleteMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        recommendationService.delete(userId, recommendationId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
