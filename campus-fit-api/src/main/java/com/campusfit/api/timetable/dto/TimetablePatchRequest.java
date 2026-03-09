package com.campusfit.api.timetable.dto;

import com.campusfit.api.common.enums.TimetableStatus;
import java.util.List;

public record TimetablePatchRequest(
        String title,
        TimetableStatus status,
        List<Long> lectureIds) {
}
