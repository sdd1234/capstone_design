package com.campusfit.api.ai.scoring;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TimeRangeType;
import com.campusfit.api.domain.Lecture;
import com.campusfit.api.domain.LectureSchedule;
import com.campusfit.api.domain.PreferredTimeRange;
import com.campusfit.api.domain.TimetablePreference;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 시간표 후보의 효용을 6개 항목으로 점수화한다.
 *
 * 점수 항목
 *  - emptyDays           : 주중 공강일 수 (월~금 5일 기준)
 *  - preferTimeMatch     : PREFERRED 시간대에 들어간 강의 비율
 *  - consecutivePenalty  : 3시간 이상 연강 페널티
 *  - lunchBreak          : 모든 등교일에 12:00~13:00 비어있으면 가점
 *  - firstPeriodPenalty  : 09:00 이전에 시작하는 강의 개수
 *  - majorRatio          : 전공 학점 / 총 학점
 */
@Component
public class TimetableScorer {

    private static final Set<DayOfWeekEnum> WEEKDAYS = Set.of(
            DayOfWeekEnum.MON, DayOfWeekEnum.TUE, DayOfWeekEnum.WED, DayOfWeekEnum.THU, DayOfWeekEnum.FRI);

    private static final LocalTime FIRST_PERIOD_CUTOFF = LocalTime.of(9, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final int CONSECUTIVE_MINUTES_THRESHOLD = 180;

    public ScoredResult score(List<Lecture> plan, TimetablePreference pref, ScoreWeights w) {
        List<String> reasons = new ArrayList<>();

        double emptyDays = emptyDaysScore(plan, reasons);
        double preferMatch = preferTimeMatchScore(plan, pref, reasons);
        double consecutivePenalty = consecutivePenalty(plan, reasons);
        double lunchBreak = lunchBreakScore(plan, reasons);
        double firstPeriodPenalty = firstPeriodPenalty(plan, reasons);
        double majorRatio = majorRatioScore(plan, reasons);

        double total = w.wEmptyDays() * emptyDays
                + w.wPreferTime() * preferMatch
                - w.wConsecutivePenalty() * consecutivePenalty
                + w.wLunchBreak() * lunchBreak
                - w.wFirstPeriodPenalty() * firstPeriodPenalty
                + w.wMajorRatio() * majorRatio;

        return new ScoredResult(total, reasons);
    }

    private double emptyDaysScore(List<Lecture> plan, List<String> reasons) {
        Set<DayOfWeekEnum> attendingDays = new HashSet<>();
        for (Lecture l : plan) {
            for (LectureSchedule s : l.getSchedules()) {
                attendingDays.add(s.getDayOfWeek());
            }
        }
        Set<DayOfWeekEnum> emptyWeekdays = new HashSet<>(WEEKDAYS);
        emptyWeekdays.removeAll(attendingDays);
        int empty = emptyWeekdays.size();
        if (empty > 0) {
            List<String> dayNames = emptyWeekdays.stream()
                    .sorted(Comparator.comparingInt(DayOfWeekEnum::ordinal))
                    .map(this::dayKor)
                    .toList();
            reasons.add("공강일 " + empty + "일 확보 (" + String.join(", ", dayNames) + ")");
        }
        return empty;
    }

    private double preferTimeMatchScore(List<Lecture> plan, TimetablePreference pref, List<String> reasons) {
        if (pref == null || pref.getTimeRanges() == null) {
            return 0.0;
        }
        List<PreferredTimeRange> preferredRanges = pref.getTimeRanges().stream()
                .filter(r -> r.getType() == TimeRangeType.PREFERRED)
                .toList();
        if (preferredRanges.isEmpty()) {
            return 0.0;
        }
        int totalSchedules = 0;
        int matched = 0;
        for (Lecture l : plan) {
            for (LectureSchedule s : l.getSchedules()) {
                totalSchedules++;
                if (overlapsAnyRange(s, preferredRanges)) {
                    matched++;
                }
            }
        }
        if (totalSchedules == 0) {
            return 0.0;
        }
        double ratio = (double) matched / totalSchedules;
        if (ratio > 0) {
            reasons.add("선호 시간대 매칭률 " + Math.round(ratio * 100) + "%");
        }
        return ratio * 5.0;
    }

    private double consecutivePenalty(List<Lecture> plan, List<String> reasons) {
        Map<DayOfWeekEnum, List<LectureSchedule>> byDay = groupByDay(plan);
        int penalty = 0;
        for (Map.Entry<DayOfWeekEnum, List<LectureSchedule>> e : byDay.entrySet()) {
            List<LectureSchedule> sorted = new ArrayList<>(e.getValue());
            sorted.sort(Comparator.comparing(LectureSchedule::getStartTime));
            int run = 0;
            boolean runPenalized = false;
            LocalTime prevEnd = null;
            for (LectureSchedule s : sorted) {
                if (prevEnd != null && !s.getStartTime().isAfter(prevEnd)) {
                    run += minutesBetween(s.getStartTime(), s.getEndTime());
                } else {
                    run = minutesBetween(s.getStartTime(), s.getEndTime());
                    runPenalized = false;
                }
                if (!runPenalized && run >= CONSECUTIVE_MINUTES_THRESHOLD) {
                    penalty++;
                    runPenalized = true;
                }
                prevEnd = s.getEndTime();
            }
        }
        return penalty;
    }

    private double lunchBreakScore(List<Lecture> plan, List<String> reasons) {
        Map<DayOfWeekEnum, List<LectureSchedule>> byDay = groupByDay(plan);
        if (byDay.isEmpty()) {
            return 0.0;
        }
        boolean allHaveLunch = true;
        for (List<LectureSchedule> schedules : byDay.values()) {
            for (LectureSchedule s : schedules) {
                if (s.getStartTime().isBefore(LUNCH_END) && LUNCH_START.isBefore(s.getEndTime())) {
                    allHaveLunch = false;
                    break;
                }
            }
            if (!allHaveLunch) break;
        }
        if (allHaveLunch) {
            reasons.add("매일 12~13시 점심시간 확보");
            return 1.0;
        }
        return 0.0;
    }

    private double firstPeriodPenalty(List<Lecture> plan, List<String> reasons) {
        int count = 0;
        for (Lecture l : plan) {
            for (LectureSchedule s : l.getSchedules()) {
                if (s.getStartTime().isBefore(FIRST_PERIOD_CUTOFF) || s.getStartTime().equals(FIRST_PERIOD_CUTOFF)) {
                    count++;
                }
            }
        }
        if (count == 0) {
            reasons.add("9시 이전 강의 없음");
        }
        return count;
    }

    private double majorRatioScore(List<Lecture> plan, List<String> reasons) {
        int total = 0;
        int major = 0;
        for (Lecture l : plan) {
            Integer credits = l.getCourse().getCredits();
            int c = credits != null ? credits : 0;
            total += c;
            String category = l.getCourse().getCategory();
            if (category != null && category.contains("전공")) {
                major += c;
            }
        }
        if (total == 0) {
            return 0.0;
        }
        double ratio = (double) major / total;
        return ratio;
    }

    private Map<DayOfWeekEnum, List<LectureSchedule>> groupByDay(List<Lecture> plan) {
        Map<DayOfWeekEnum, List<LectureSchedule>> byDay = new EnumMap<>(DayOfWeekEnum.class);
        for (Lecture l : plan) {
            for (LectureSchedule s : l.getSchedules()) {
                byDay.computeIfAbsent(s.getDayOfWeek(), k -> new ArrayList<>()).add(s);
            }
        }
        return byDay;
    }

    private boolean overlapsAnyRange(LectureSchedule s, List<PreferredTimeRange> ranges) {
        for (PreferredTimeRange r : ranges) {
            if (r.getDayOfWeek() != null && r.getDayOfWeek() != s.getDayOfWeek()) {
                continue;
            }
            if (s.getStartTime().isBefore(r.getEndTime()) && r.getStartTime().isBefore(s.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    private int minutesBetween(LocalTime a, LocalTime b) {
        return (b.getHour() * 60 + b.getMinute()) - (a.getHour() * 60 + a.getMinute());
    }

    private String dayKor(DayOfWeekEnum d) {
        return switch (d) {
            case MON -> "월";
            case TUE -> "화";
            case WED -> "수";
            case THU -> "목";
            case FRI -> "금";
            case SAT -> "토";
            case SUN -> "일";
        };
    }

    public record ScoredResult(double score, List<String> reasons) {
    }
}
