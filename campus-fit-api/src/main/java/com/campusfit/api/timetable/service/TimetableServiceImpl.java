package com.campusfit.api.timetable.service;

import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import com.campusfit.api.timetable.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.util.ArrayList;
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
            List<Lecture> added = new ArrayList<>();
            req.lectureIds().forEach(lid -> {
                Lecture lecture = lectureRepository.findById(lid)
                        .orElseThrow(() -> BusinessException.notFound("강의를 찾을 수 없습니다: " + lid));
                checkScheduleConflicts(added, lecture);
                added.add(lecture);
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
            List<Lecture> added = new ArrayList<>();
            req.lectureIds().forEach(lid -> {
                Lecture lecture = lectureRepository.findById(lid)
                        .orElseThrow(() -> BusinessException.notFound("강의를 찾을 수 없습니다: " + lid));
                checkScheduleConflicts(added, lecture);
                added.add(lecture);
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

    /** 기존 강의 목록에 추가 강의를 넣었을 때 시간 충돌 여부 확인 */
    private void checkScheduleConflicts(List<Lecture> existingLectures, Lecture newLecture) {
        List<LectureSchedule> newSchedules = newLecture.getSchedules();
        for (Lecture existing : existingLectures) {
            for (LectureSchedule es : existing.getSchedules()) {
                for (LectureSchedule ns : newSchedules) {
                    if (es.getDayOfWeek() == ns.getDayOfWeek()
                            && isOverlapping(es.getStartTime(), es.getEndTime(), ns.getStartTime(), ns.getEndTime())) {
                        throw BusinessException.conflict(
                                "강의 시간이 충돌합니다: [" + existing.getCourse().getName() + "] vs ["
                                        + newLecture.getCourse().getName() + "] " + es.getDayOfWeek() + " "
                                        + es.getStartTime() + "~" + es.getEndTime());
                    }
                }
            }
        }
    }

    private boolean isOverlapping(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
