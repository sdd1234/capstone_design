package com.campusfit.api.timetablepreference.service;

import com.campusfit.api.timetablepreference.dto.*;

public interface TimetablePreferenceService {
    TimetablePreferenceResponse save(Long userId, TimetablePreferenceRequest request);

    TimetablePreferenceResponse get(Long userId, Integer year, String termSeason);
}
