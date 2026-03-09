package com.campusfit.api.calendar.service;

import com.campusfit.api.calendar.dto.*;
import com.campusfit.api.common.enums.CalendarView;
import java.time.LocalDate;
import java.util.List;

public interface CalendarService {
    List<CalendarItemResponse> getCalendar(Long userId, CalendarView view, LocalDate date);
}
