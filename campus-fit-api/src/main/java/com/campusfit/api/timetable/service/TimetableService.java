package com.campusfit.api.timetable.service;

import com.campusfit.api.timetable.dto.*;
import java.util.List;

public interface TimetableService {
    TimetableResponse create(Long userId, TimetableCreateRequest request);

    TimetableResponse patch(Long userId, Long timetableId, TimetablePatchRequest request);

    List<TimetableResponse> list(Long userId);

    TimetableResponse get(Long userId, Long timetableId);

    void delete(Long userId, Long timetableId);
}
