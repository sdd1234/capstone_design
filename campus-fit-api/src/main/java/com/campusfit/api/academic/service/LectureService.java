package com.campusfit.api.academic.service;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.academic.dto.PrerequisiteResponse;
import java.util.List;

public interface LectureService {
    List<LectureResponse> search(Long universityId, Integer year, String termSeason, String keyword);

    List<PrerequisiteResponse> getPrerequisites(Long courseId);
}
