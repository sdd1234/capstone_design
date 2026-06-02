package com.campusfit.api.timetable.service;

import com.campusfit.api.timetable.dto.*;
import java.util.List;

public interface TimetableService {
    TimetableResponse create(Long userId, TimetableCreateRequest request);

    TimetableResponse patch(Long userId, Long timetableId, TimetablePatchRequest request);

    List<TimetableResponse> list(Long userId);

    TimetableResponse get(Long userId, Long timetableId);

    /** 해당 시간표를 그 학기의 대표로 지정한다(같은 학기 다른 시간표의 대표 표시는 해제). */
    TimetableResponse setPrimary(Long userId, Long timetableId);

    void delete(Long userId, Long timetableId);
}
