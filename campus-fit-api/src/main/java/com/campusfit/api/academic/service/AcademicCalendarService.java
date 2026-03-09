package com.campusfit.api.academic.service;

import com.campusfit.api.academic.dto.AcademicCalendarEventResponse;
import java.util.List;

public interface AcademicCalendarService {
    List<AcademicCalendarEventResponse> getEvents(Integer year, Long universityId);
}
