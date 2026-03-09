package com.campusfit.api.calendar.controller;

import com.campusfit.api.calendar.dto.CalendarItemResponse;
import com.campusfit.api.calendar.service.CalendarService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.common.enums.CalendarView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CalendarItemResponse>>> getCalendar(
            @RequestParam(defaultValue = "MONTH") CalendarView view,
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(calendarService.getCalendar(userId, view, targetDate)));
    }
}
