package com.campusfit.api.timemanagement.service;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.timemanagement.dto.ActivityGoalCreateRequest;
import com.campusfit.api.timemanagement.dto.ActivityGoalResponse;
import com.campusfit.api.timemanagement.dto.ActivityGoalUpdateRequest;
import java.util.List;

public interface ActivityGoalService {
    ActivityGoalResponse create(Long userId, ActivityGoalCreateRequest req, Integer year, TermSeason termSeason);
    List<ActivityGoalResponse> list(Long userId);
    ActivityGoalResponse update(Long userId, Long goalId, ActivityGoalUpdateRequest req, Integer year, TermSeason termSeason);
    void delete(Long userId, Long goalId);
}
