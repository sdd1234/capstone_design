package com.campusfit.api.timetablepreference.controller;

import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.timetablepreference.dto.TimetablePreferenceRequest;
import com.campusfit.api.timetablepreference.dto.TimetablePreferenceResponse;
import com.campusfit.api.timetablepreference.service.TimetablePreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/timetable-preferences")
@RequiredArgsConstructor
public class TimetablePreferenceController {

    private final TimetablePreferenceService preferenceService;

    @PutMapping
    public ResponseEntity<ApiResponse<TimetablePreferenceResponse>> save(
            @Valid @RequestBody TimetablePreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(preferenceService.save(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TimetablePreferenceResponse>> get(
            @RequestParam Integer year,
            @RequestParam String termSeason,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(preferenceService.get(userId, year, termSeason)));
    }
}
