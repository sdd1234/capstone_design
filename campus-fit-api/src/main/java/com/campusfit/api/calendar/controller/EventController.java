package com.campusfit.api.calendar.controller;

import com.campusfit.api.calendar.dto.EventCreateRequest;
import com.campusfit.api.calendar.dto.EventResponse;
import com.campusfit.api.calendar.dto.EventUpdateRequest;
import com.campusfit.api.calendar.service.EventService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.domain.Event;
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
import java.util.Map;

@Tag(name = "이벤트 관리", description = "개인 일정(시험·과제·행사 등) 등록·조회·수정·삭제 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 생성", description = "캘린더에 새 일정을 등록합니다.\n\ncategory 값: CLASS · ASSIGNMENT · EXAM · PERSONAL · SCHOOL · PROJECT")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        Event event = eventService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(Map.of(
                "id", event.getId(),
                "title", event.getTitle(),
                "startAt", event.getStartAt().toString(),
                "endAt", event.getEndAt().toString(),
                "category", event.getCategory().name())));
    }

    @Operation(summary = "이벤트 목록 조회", description = "내 전체 이벤트 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(eventService.list(userId)));
    }

    @Operation(summary = "이벤트 상세 조회", description = "특정 이벤트의 시간·설명·색상 등 상세 정보를 반환합니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> get(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(eventService.get(userId, eventId)));
    }

    @Operation(summary = "이벤트 수정", description = "변경할 필드만 포함해 부분 수정합니다. null인 필드는 기존 값을 유지합니다.")
    @PatchMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> update(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @RequestBody EventUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(eventService.update(userId, eventId, request)));
    }

    @Operation(summary = "이벤트 삭제", description = "특정 이벤트를 삭제합니다. 본인 소유 이벤트만 삭제 가능합니다.")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "이벤트 ID") @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        eventService.delete(userId, eventId);
        return ResponseEntity.noContent().build();
    }
}
