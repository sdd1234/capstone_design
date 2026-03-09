package com.campusfit.api.timetable.service;

import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import com.campusfit.api.timetable.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;

    @Override
    public TimetableResponse create(Long userId, TimetableCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        Timetable timetable = Timetable.builder()
                .user(user)
                .year(req.year())
                .termSeason(req.termSeason())
                .title(req.title())
                .sourceRecommendationId(req.sourceRecommendationId())
                .build();

        if (req.lectureIds() != null) {
            req.lectureIds().forEach(lid -> {
                Lecture lecture = lectureRepository.findById(lid)
                        .orElseThrow(() -> BusinessException.notFound("강의를 찾을 수 없습니다: " + lid));
                timetable.getItems().add(TimetableItem.builder()
                        .timetable(timetable)
                        .lecture(lecture)
                        .build());
            });
        }

        return TimetableResponse.from(timetableRepository.save(timetable));
    }

    @Override
    public TimetableResponse patch(Long userId, Long timetableId, TimetablePatchRequest req) {
        Timetable t = findOwned(userId, timetableId);

        if (req.title() != null)
            t.setTitle(req.title());
        if (req.status() != null)
            t.setStatus(req.status());

        if (req.lectureIds() != null) {
            t.getItems().clear();
            req.lectureIds().forEach(lid -> {
                Lecture lecture = lectureRepository.findById(lid)
                        .orElseThrow(() -> BusinessException.notFound("강의를 찾을 수 없습니다: " + lid));
                t.getItems().add(TimetableItem.builder().timetable(t).lecture(lecture).build());
            });
        }

        return TimetableResponse.from(timetableRepository.save(t));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableResponse> list(Long userId) {
        return timetableRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(TimetableResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableResponse get(Long userId, Long timetableId) {
        return TimetableResponse.from(findOwned(userId, timetableId));
    }

    @Override
    public void delete(Long userId, Long timetableId) {
        timetableRepository.delete(findOwned(userId, timetableId));
    }

    private Timetable findOwned(Long userId, Long timetableId) {
        Timetable t = timetableRepository.findById(timetableId)
                .orElseThrow(() -> BusinessException.notFound("시간표를 찾을 수 없습니다."));
        if (!t.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        return t;
    }
}
