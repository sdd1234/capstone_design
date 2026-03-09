package com.campusfit.api.calendar.controller;

import com.campusfit.api.calendar.dto.EventCreateRequest;
import com.campusfit.api.calendar.service.EventService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.domain.Event;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

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
}
