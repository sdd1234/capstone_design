package com.campusfit.api.academic.service;

import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.academic.dto.PrerequisiteResponse;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.Course;
import com.campusfit.api.repository.CourseRepository;
import com.campusfit.api.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;

    @Override
    public List<LectureResponse> search(Long universityId, Integer year, String termSeason, String keyword) {
        TermSeason ts = TermSeason.valueOf(termSeason);
        return lectureRepository.searchLectures(universityId, year, ts, keyword)
                .stream().map(LectureResponse::from).toList();
    }

    @Override
    public List<PrerequisiteResponse> getPrerequisites(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> BusinessException.notFound("강좌를 찾을 수 없습니다."));
        return course.getPrerequisites().stream().map(PrerequisiteResponse::from).toList();
    }
}
