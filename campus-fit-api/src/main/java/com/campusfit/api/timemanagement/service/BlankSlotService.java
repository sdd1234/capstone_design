package com.campusfit.api.timemanagement.service;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.ActivityGoal;
import com.campusfit.api.domain.AppliedAllocation;
import com.campusfit.api.domain.LectureSchedule;
import com.campusfit.api.domain.Timetable;
import com.campusfit.api.domain.TimetableItem;
import com.campusfit.api.repository.ActivityGoalRepository;
import com.campusfit.api.repository.AppliedAllocationRepository;
import com.campusfit.api.repository.LectureRepository;
import com.campusfit.api.repository.TimetableRepository;
import com.campusfit.api.timemanagement.dto.BlankSlotResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주간 빈 시간대 분석.
 *
 * 점유 시간 = 사용자의 그 학기 시간표 강의 + 이미 배치된 ActivityGoal(요일·시간 3필드 모두 명시).
 * 분석 범위: 월~일 09:00-22:00 (주말 포함). 30분 미만 짧은 슬롯은 무시.
 *
 * Event / Task 는 단발성이라 weekly typical 빈 슬롯 계산에는 미포함.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlankSlotService {

    private static final LocalTime DAY_START = LocalTime.of(9, 0);
    private static final LocalTime DAY_END = LocalTime.of(22, 0);
    private static final int MIN_SLOT_MINUTES = 30;
    private static final List<DayOfWeekEnum> WEEKDAYS = List.of(
            DayOfWeekEnum.MON, DayOfWeekEnum.TUE, DayOfWeekEnum.WED,
            DayOfWeekEnum.THU, DayOfWeekEnum.FRI, DayOfWeekEnum.SAT, DayOfWeekEnum.SUN);

    private final TimetableRepository timetableRepository;
    private final ActivityGoalRepository goalRepository;
    private final LectureRepository lectureRepository;
    private final AppliedAllocationRepository appliedRepository;

    /** 기존 호출자 호환 — weekStart=현재 주 default */
    public List<BlankSlotResponse> getBlankSlots(Long userId, Integer year, TermSeason termSeason) {
        return getBlankSlots(userId, year, termSeason, AppliedAllocationService.currentWeekStart());
    }

    public List<BlankSlotResponse> getBlankSlots(Long userId, Integer year, TermSeason termSeason, LocalDate weekStart) {
        Map<DayOfWeekEnum, List<TimeRange>> occupiedByDay = new EnumMap<>(DayOfWeekEnum.class);
        for (DayOfWeekEnum d : WEEKDAYS) {
            occupiedByDay.put(d, new ArrayList<>());
        }

        // 1. 시간표 + items + lecture 한 쿼리로 fetch (N+1 제거 1단계)
        List<Timetable> timetables = timetableRepository.findByUserIdAndYearAndTermSeasonWithLectures(userId, year, termSeason);

        // schedules 일괄 로딩 (N+1 제거 2단계) — 같은 영속성 컨텍스트의 Lecture 인스턴스에 collection 채워짐
        Set<Long> lectureIds = new HashSet<>();
        for (Timetable tt : timetables) {
            for (TimetableItem item : tt.getItems()) {
                if (item.getLecture() != null) lectureIds.add(item.getLecture().getId());
            }
        }
        if (!lectureIds.isEmpty()) lectureRepository.findByIdsWithSchedules(lectureIds);

        for (Timetable tt : timetables) {
            for (TimetableItem item : tt.getItems()) {
                if (item.getLecture() == null) continue;
                for (LectureSchedule s : item.getLecture().getSchedules()) {
                    if (occupiedByDay.containsKey(s.getDayOfWeek())) {
                        occupiedByDay.get(s.getDayOfWeek()).add(new TimeRange(s.getStartTime(), s.getEndTime()));
                    }
                }
            }
        }

        // 2. ActivityGoal 중 FIXED 모드이면서 요일·시작·끝 3필드 모두 명시된 거 → occupied
        //    (WINDOW 모드는 hint이지 commitment 아니라서 occupied 아님)
        for (ActivityGoal g : goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId)) {
            if (g.getPreferredMode() != com.campusfit.api.common.enums.PreferredMode.FIXED) continue;
            DayOfWeekEnum d = g.getPreferredDayOfWeek();
            LocalTime s = g.getPreferredStartTime();
            LocalTime e = g.getPreferredEndTime();
            if (d != null && s != null && e != null && occupiedByDay.containsKey(d)) {
                occupiedByDay.get(d).add(new TimeRange(s, e));
            }
        }

        // 3. AppliedAllocation (해당 주의 사용자 명시 적용분) → occupied
        for (AppliedAllocation a : appliedRepository.findByUserAndWeek(userId, weekStart)) {
            if (occupiedByDay.containsKey(a.getDayOfWeek())) {
                occupiedByDay.get(a.getDayOfWeek()).add(new TimeRange(a.getStartTime(), a.getEndTime()));
            }
        }

        // 4. 요일별로 occupied 머지 → 09:00-22:00 범위에서 빈 구간 추출
        List<BlankSlotResponse> result = new ArrayList<>();
        for (DayOfWeekEnum day : WEEKDAYS) {
            List<TimeRange> merged = mergeRanges(occupiedByDay.get(day));
            for (TimeRange free : freeRanges(merged, DAY_START, DAY_END)) {
                int minutes = (int) Duration.between(free.start, free.end).toMinutes();
                if (minutes >= MIN_SLOT_MINUTES) {
                    result.add(new BlankSlotResponse(day, free.start, free.end, minutes));
                }
            }
        }
        return result;
    }

    private List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        if (ranges.isEmpty()) return List.of();
        List<TimeRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(r -> r.start));
        List<TimeRange> merged = new ArrayList<>();
        TimeRange cur = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            TimeRange next = sorted.get(i);
            if (!next.start.isAfter(cur.end)) {
                cur = new TimeRange(cur.start, cur.end.isAfter(next.end) ? cur.end : next.end);
            } else {
                merged.add(cur);
                cur = next;
            }
        }
        merged.add(cur);
        return merged;
    }

    private List<TimeRange> freeRanges(List<TimeRange> merged, LocalTime windowStart, LocalTime windowEnd) {
        List<TimeRange> result = new ArrayList<>();
        LocalTime cursor = windowStart;
        for (TimeRange r : merged) {
            if (r.end.compareTo(windowStart) <= 0) continue;
            if (r.start.compareTo(windowEnd) >= 0) break;
            LocalTime occStart = r.start.isBefore(windowStart) ? windowStart : r.start;
            LocalTime occEnd = r.end.isAfter(windowEnd) ? windowEnd : r.end;
            if (cursor.isBefore(occStart)) {
                result.add(new TimeRange(cursor, occStart));
            }
            if (occEnd.isAfter(cursor)) {
                cursor = occEnd;
            }
        }
        if (cursor.isBefore(windowEnd)) {
            result.add(new TimeRange(cursor, windowEnd));
        }
        return result;
    }

    private record TimeRange(LocalTime start, LocalTime end) {}
}
