package com.campusfit.api.academic.service;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.academic.dto.PrerequisiteResponse;
import java.util.List;

public interface LectureService {
    List<LectureResponse> search(Long universityId, Integer year, String termSeason, String keyword, String category,
            String area);

    LectureResponse getById(Long lectureId);

    List<PrerequisiteResponse> getPrerequisites(Long courseId);

    List<String> getDepts(Integer year, String termSeason);
}
