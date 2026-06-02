package com.campusfit.api.personalblock.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.personalblock.dto.PersonalBlockCreateRequest;
import com.campusfit.api.personalblock.dto.PersonalBlockResponse;
import com.campusfit.api.personalblock.service.PersonalBlockService;
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

@Tag(name = "개인 일정", description = "시간표에 표시되는 알바·개인 반복 일정 API (강의 시간표와 독립)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/personal-blocks")
@RequiredArgsConstructor
public class PersonalBlockController {

    private final PersonalBlockService personalBlockService;

    @Operation(summary = "개인 일정 목록", description = "해당 연도/학기의 알바·개인 반복 일정을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonalBlockResponse>>> list(
            @Parameter(description = "연도") @RequestParam Integer year,
            @Parameter(description = "학기 (SPRING/SUMMER/FALL/WINTER)") @RequestParam TermSeason termSeason,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(personalBlockService.list(userId, year, termSeason)));
    }

    @Operation(summary = "개인 일정 추가", description = "요일·시작/종료 시간으로 매주 반복되는 개인 일정(알바 등)을 추가합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<PersonalBlockResponse>> create(
            @Valid @RequestBody PersonalBlockCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(personalBlockService.create(userId, request)));
    }

    @Operation(summary = "개인 일정 삭제")
    @DeleteMapping("/{blockId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "개인 일정 ID") @PathVariable Long blockId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        personalBlockService.delete(userId, blockId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
