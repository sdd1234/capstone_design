package com.campusfit.api.timemanagement.service;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.PreferredMode;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.ActivityGoal;
import com.campusfit.api.domain.LectureSchedule;
import com.campusfit.api.domain.Timetable;
import com.campusfit.api.domain.TimetableItem;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.ActivityGoalRepository;
import com.campusfit.api.repository.LectureRepository;
import com.campusfit.api.repository.TimetableRepository;
import com.campusfit.api.repository.UserRepository;
import com.campusfit.api.timemanagement.dto.ActivityGoalCreateRequest;
import com.campusfit.api.timemanagement.dto.ActivityGoalResponse;
import com.campusfit.api.timemanagement.dto.ActivityGoalUpdateRequest;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityGoalServiceImpl implements ActivityGoalService {

    private final ActivityGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final TimetableRepository timetableRepository;
    private final LectureRepository lectureRepository;

    @Override
    public ActivityGoalResponse create(Long userId, ActivityGoalCreateRequest req, Integer year, TermSeason termSeason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        validateTimeWindow(req.preferredStartTime(), req.preferredEndTime());
        validateFixedMode(req.preferredMode(), req.preferredDayOfWeek(),
                req.preferredStartTime(), req.preferredEndTime());
        validateNoCollision(userId, req.preferredDayOfWeek(), req.preferredStartTime(), req.preferredEndTime(),
                year, termSeason, null);
        ActivityGoal goal = ActivityGoal.builder()
                .user(user)
                .title(req.title())
                .category(req.category())
                .targetHoursPerWeek(req.targetHoursPerWeek())
                .preferredDayOfWeek(req.preferredDayOfWeek())
                .preferredStartTime(req.preferredStartTime())
                .preferredEndTime(req.preferredEndTime())
                .preferredMode(req.preferredMode() != null ? req.preferredMode() : PreferredMode.WINDOW)
                .rawInput(req.rawInput())
                .build();
        goalRepository.save(goal);
        return ActivityGoalResponse.from(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityGoalResponse> list(Long userId) {
        return goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ActivityGoalResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public ActivityGoalResponse update(Long userId, Long goalId, ActivityGoalUpdateRequest req, Integer year, TermSeason termSeason) {
        ActivityGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> BusinessException.notFound("활동 목표를 찾을 수 없습니다."));
        if (!goal.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("본인 활동 목표만 수정 가능합니다.");
        }
        if (req.title() != null) goal.setTitle(req.title());
        if (req.category() != null) goal.setCategory(req.category());
        if (req.targetHoursPerWeek() != null) goal.setTargetHoursPerWeek(req.targetHoursPerWeek());
        if (req.preferredDayOfWeek() != null) goal.setPreferredDayOfWeek(req.preferredDayOfWeek());
        if (req.preferredStartTime() != null) goal.setPreferredStartTime(req.preferredStartTime());
        if (req.preferredEndTime() != null) goal.setPreferredEndTime(req.preferredEndTime());
        if (req.preferredMode() != null) goal.setPreferredMode(req.preferredMode());
        if (req.rawInput() != null) goal.setRawInput(req.rawInput());
        validateTimeWindow(goal.getPreferredStartTime(), goal.getPreferredEndTime());
        validateFixedMode(goal.getPreferredMode(), goal.getPreferredDayOfWeek(),
                goal.getPreferredStartTime(), goal.getPreferredEndTime());
        validateNoCollision(userId, goal.getPreferredDayOfWeek(), goal.getPreferredStartTime(), goal.getPreferredEndTime(),
                year, termSeason, goalId);
        return ActivityGoalResponse.from(goal);
    }

    @Override
    public void delete(Long userId, Long goalId) {
        ActivityGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> BusinessException.notFound("활동 목표를 찾을 수 없습니다."));
        if (!goal.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("본인 활동 목표만 삭제 가능합니다.");
        }
        goalRepository.delete(goal);
    }

    private void validateTimeWindow(LocalTime start, LocalTime end) {
        if (start == null && end == null) return;
        if (start == null || end == null) {
            throw BusinessException.badRequest("선호 시작·끝 시각은 함께 입력해야 합니다.");
        }
        if (!start.isBefore(end)) {
            throw BusinessException.badRequest("선호 시작 시각은 끝 시각보다 빨라야 합니다.");
        }
    }

    /** FIXED 모드는 요일·시작·끝 3필드 모두 명시 필수 (없으면 commit할 시간이 정의 안 됨). */
    private void validateFixedMode(PreferredMode mode, DayOfWeekEnum day, LocalTime start, LocalTime end) {
        if (mode != PreferredMode.FIXED) return;
        if (day == null || start == null || end == null) {
            throw BusinessException.badRequest("고정(FIXED) 모드는 선호 요일·시작·끝 시각을 모두 입력해야 합니다.");
        }
    }

    /**
     * 충돌 검증 — 요일·시작·끝 3필드 모두 명시된 경우에만 수행.
     * 1) 같은 사용자의 다른 fully-specified ActivityGoal과 시간 겹침
     * 2) year+termSeason 제공 시, 그 학기 시간표 강의 schedule과 시간 겹침
     */
    private void validateNoCollision(Long userId, DayOfWeekEnum day, LocalTime start, LocalTime end,
                                      Integer year, TermSeason termSeason, Long excludeGoalId) {
        if (day == null || start == null || end == null) return;

        // 1) ActivityGoal vs ActivityGoal
        for (ActivityGoal other : goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId)) {
            if (excludeGoalId != null && other.getId().equals(excludeGoalId)) continue;
            if (other.getPreferredDayOfWeek() != day) continue;
            if (other.getPreferredStartTime() == null || other.getPreferredEndTime() == null) continue;
            if (overlaps(start, end, other.getPreferredStartTime(), other.getPreferredEndTime())) {
                throw BusinessException.badRequest(
                        "다른 활동 \"" + other.getTitle() + "\" (" + other.getPreferredStartTime()
                                + "~" + other.getPreferredEndTime() + ")과 시간이 겹칩니다.");
            }
        }

        // 2) ActivityGoal vs Lecture (학기 컨텍스트가 제공된 경우만)
        if (year == null || termSeason == null) return;
        List<Timetable> timetables = timetableRepository
                .findByUserIdAndYearAndTermSeasonWithLectures(userId, year, termSeason);
        Set<Long> lectureIds = new HashSet<>();
        for (Timetable tt : timetables) {
            for (TimetableItem item : tt.getItems()) {
                if (item.getLecture() != null) lectureIds.add(item.getLecture().getId());
            }
        }
        if (lectureIds.isEmpty()) return;
        lectureRepository.findByIdsWithSchedules(lectureIds);

        for (Timetable tt : timetables) {
            for (TimetableItem item : tt.getItems()) {
                if (item.getLecture() == null) continue;
                for (LectureSchedule s : item.getLecture().getSchedules()) {
                    if (s.getDayOfWeek() != day) continue;
                    if (overlaps(start, end, s.getStartTime(), s.getEndTime())) {
                        throw BusinessException.badRequest(
                                "강의 \"" + item.getLecture().getCourse().getName() + "\" ("
                                        + s.getStartTime() + "~" + s.getEndTime() + ")과 시간이 겹칩니다.");
                    }
                }
            }
        }
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
