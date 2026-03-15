package com.campusfit.api.calendar.controller;

import com.campusfit.api.calendar.dto.CalendarItemResponse;
import com.campusfit.api.calendar.service.CalendarService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.common.enums.CalendarView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "캘린더 통합 뷰", description = "월·주·일 단위로 이벤트와 태스크를 통합 조회하는 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "캘린더 통합 조회", description = "지정한 날짜 기준으로 이벤트와 태스크를 한 번에 조회합니다.\n\n" +
            "- view=MONTH: 해당 월 전체\n" +
            "- view=WEEK: 해당 주 (월~일)\n" +
            "- view=DAY: 해당 날 하루\n" +
            "- date 미입력 시 오늘 날짜 기준")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CalendarItemResponse>>> getCalendar(
            @Parameter(description = "조회 단위 (MONTH / WEEK / DAY)", example = "MONTH") @RequestParam(defaultValue = "MONTH") CalendarView view,
            @Parameter(description = "기준 날짜 (YYYY-MM-DD), 미입력 시 오늘", example = "2026-04-01") @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(calendarService.getCalendar(userId, view, targetDate)));
    }
}
