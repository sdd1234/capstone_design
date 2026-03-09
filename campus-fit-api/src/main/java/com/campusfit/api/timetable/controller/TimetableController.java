package com.campusfit.api.timetable.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.timetable.dto.*;
import com.campusfit.api.timetable.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @PostMapping
    public ResponseEntity<ApiResponse<TimetableResponse>> create(
            @Valid @RequestBody TimetableCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(timetableService.create(userId, request)));
    }

    @PatchMapping("/{timetableId}")
    public ResponseEntity<ApiResponse<TimetableResponse>> patch(
            @PathVariable Long timetableId,
            @RequestBody TimetablePatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(timetableService.patch(userId, timetableId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TimetableResponse>>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(timetableService.list(userId)));
    }

    @GetMapping("/{timetableId}")
    public ResponseEntity<ApiResponse<TimetableResponse>> get(
            @PathVariable Long timetableId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(timetableService.get(userId, timetableId)));
    }

    @DeleteMapping("/{timetableId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long timetableId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        timetableService.delete(userId, timetableId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
