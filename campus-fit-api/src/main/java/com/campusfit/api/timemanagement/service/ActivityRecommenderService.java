package com.campusfit.api.timemanagement.service;

import com.campusfit.api.common.enums.ActivityCategory;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.PreferredMode;
import com.campusfit.api.common.enums.TaskStatus;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.ActivityGoal;
import com.campusfit.api.domain.Task;
import com.campusfit.api.repository.ActivityGoalRepository;
import com.campusfit.api.repository.AppliedAllocationRepository;
import com.campusfit.api.repository.TaskRepository;
import com.campusfit.api.timemanagement.dto.ActivityAllocationResponse;
import com.campusfit.api.timemanagement.dto.BlankSlotResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룰 기반 활동 배치 추천기.
 *
 * 동작
 *  1. 빈 슬롯 조회 (시간표 + FIXED 모드 ActivityGoal + AppliedAllocation 점유 제외)
 *  2. WINDOW 모드 활동만 추천 대상 (FIXED는 이미 commit된 시간이라 추천 X)
 *  3. 활동별 배치:
 *     - 카테고리 default window (또는 사용자 지정 window) 안에 들어가는 빈 슬롯
 *     - **요일 round-robin**: 매 패스마다 각 요일에 한 세션씩 배치 → 같은 요일 몰빵 방지
 *     - 더 이상 새 요일에 배치 못하면 (모든 요일 한 번씩 다 받음) 같은 요일에 추가 chunk
 *     - **15분 buffer**: 점유 시간(강의/applied) 직후 15분 휴식 보장 (윈도우 경계엔 적용 X)
 *
 * ChatGPT 연동 전 베이스라인 — LLM은 이 결과 위에 자연어 설명/이유 보강 layer로 추가 예정.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityRecommenderService {

    private static final int MIN_BLOCK_MINUTES = 30;
    private static final int MAX_BLOCK_MINUTES = 120;
    private static final int BUFFER_MINUTES = 15;

    private static final Map<ActivityCategory, LocalTime[]> CATEGORY_DEFAULT_WINDOW = Map.of(
            ActivityCategory.STUDY,    new LocalTime[]{ LocalTime.of(14, 0), LocalTime.of(18, 0) },
            ActivityCategory.EXERCISE, new LocalTime[]{ LocalTime.of(18, 0), LocalTime.of(21, 0) },
            ActivityCategory.MEAL,     new LocalTime[]{ LocalTime.of(12, 0), LocalTime.of(13, 0) },
            ActivityCategory.HOBBY,    new LocalTime[]{ LocalTime.of(19, 0), LocalTime.of(22, 0) },
            ActivityCategory.REST,     new LocalTime[]{ LocalTime.of(15, 0), LocalTime.of(17, 0) },
            ActivityCategory.OTHER,    new LocalTime[]{ LocalTime.of(9,  0), LocalTime.of(22, 0) });

    private final BlankSlotService blankSlotService;
    private final ActivityGoalRepository goalRepository;
    private final AppliedAllocationRepository appliedRepository;
    private final TaskRepository taskRepository;

    /** 기존 호출자 호환 — weekStart=현재 주 default */
    public List<ActivityAllocationResponse> recommend(Long userId, Integer year, TermSeason termSeason) {
        return recommend(userId, year, termSeason, AppliedAllocationService.currentWeekStart());
    }

    public List<ActivityAllocationResponse> recommend(Long userId, Integer year, TermSeason termSeason, LocalDate weekStart) {
        // 1. 빈 슬롯 → 점유 트래커 (FIXED ActivityGoal + 해당 주 AppliedAllocation은 이미 occupied로 빠진 상태)
        List<BlankSlotResponse> blanks = blankSlotService.getBlankSlots(userId, year, termSeason, weekStart);
        Map<DayOfWeekEnum, List<TimeRange>> available = new EnumMap<>(DayOfWeekEnum.class);
        for (BlankSlotResponse b : blanks) {
            available.computeIfAbsent(b.dayOfWeek(), k -> new ArrayList<>())
                    .add(new TimeRange(b.startTime(), b.endTime()));
        }

        // 2. WINDOW 모드 + 해당 주에 아직 적용 안 된 goal만 추천 대상
        //    (FIXED는 commit, 해당 주 applied된 건 확정 — 다른 주는 별개로 추천 가능)
        Set<Long> appliedGoalIdsThisWeek = appliedRepository.findByUserAndWeek(userId, weekStart).stream()
                .map(a -> a.getGoal().getId())
                .collect(Collectors.toCollection(HashSet::new));
        List<ActivityGoal> goals = goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<ActivityGoal> pending = new ArrayList<>();
        for (ActivityGoal g : goals) {
            if (g.getPreferredMode() == PreferredMode.FIXED) continue;
            if (appliedGoalIdsThisWeek.contains(g.getId())) continue;
            pending.add(g);
        }

        // 3. 사용자 명시가 더 구체적인 활동부터 (요일 명시 > 시간 명시 > 무명시)
        pending.sort(Comparator.comparingInt(this::specificityRank).reversed());

        List<ActivityAllocationResponse> result = new ArrayList<>();
        for (ActivityGoal g : pending) {
            result.addAll(allocateOne(g, available));
        }

        // 4. 일회성 Task 추천 (마감 임박 우선) — Goal 배치 후 남은 슬롯 활용
        result.addAll(allocateTasks(userId, weekStart, available));

        return result;
    }

    /**
     * Task 추천 — status != DONE, dueAt 이번 주 끝 이내, estimatedMinutes 명시된 것만.
     * 마감 임박 순으로 정렬, 빈 시간에 배치 (해당 task의 마감 이전 날짜만).
     */
    private List<ActivityAllocationResponse> allocateTasks(Long userId, LocalDate weekStart,
                                                            Map<DayOfWeekEnum, List<TimeRange>> available) {
        LocalDateTime weekEnd = weekStart.plusDays(7).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        List<Task> candidates = taskRepository.findByUserId(userId).stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getEstimatedMinutes() != null && t.getEstimatedMinutes() > 0)
                .filter(t -> t.getDueAt() != null && t.getDueAt().isAfter(now) && t.getDueAt().isBefore(weekEnd))
                .sorted(Comparator.comparing(Task::getDueAt))
                .collect(Collectors.toList());

        List<ActivityAllocationResponse> placed = new ArrayList<>();
        for (Task task : candidates) {
            placed.addAll(allocateOneTask(task, weekStart, available));
        }
        return placed;
    }

    private List<ActivityAllocationResponse> allocateOneTask(Task task, LocalDate weekStart,
                                                              Map<DayOfWeekEnum, List<TimeRange>> available) {
        List<ActivityAllocationResponse> placed = new ArrayList<>();
        int remainingMinutes = task.getEstimatedMinutes();
        LocalDate dueDate = task.getDueAt().toLocalDate();
        LocalTime[] dw = CATEGORY_DEFAULT_WINDOW.get(ActivityCategory.OTHER);
        LocalTime windowStart = dw[0], windowEnd = dw[1];

        // 마감 이전 요일들만 후보 (이번 주 안에서)
        List<DayOfWeekEnum> dayCandidates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            if (!date.isAfter(dueDate)) {
                dayCandidates.add(DayOfWeekEnum.values()[date.getDayOfWeek().getValue() - 1]);
            }
        }

        // round-robin
        while (remainingMinutes > 0) {
            boolean placedAny = false;
            for (DayOfWeekEnum day : dayCandidates) {
                if (remainingMinutes <= 0) break;
                List<TimeRange> dayList = available.get(day);
                if (dayList == null) continue;

                TimeRange picked = null;
                for (TimeRange r : dayList) {
                    TimeRange clipped = clipWithBuffer(r, windowStart, windowEnd);
                    if (clipped != null && minutesOf(clipped) >= MIN_BLOCK_MINUTES) {
                        picked = clipped;
                        break;
                    }
                }
                if (picked == null) continue;

                int slotMins = Math.min(minutesOf(picked), Math.min(remainingMinutes, MAX_BLOCK_MINUTES));
                slotMins = (slotMins / 30) * 30;
                if (slotMins < MIN_BLOCK_MINUTES) continue;

                LocalTime allocStart = picked.start;
                LocalTime allocEnd = allocStart.plusMinutes(slotMins);
                subtract(available.get(day), new TimeRange(allocStart, allocEnd));

                long daysLeft = Duration.between(LocalDateTime.now(), task.getDueAt()).toDays();
                String reason = "📋 " + task.getTitle() + " — " + dayKor(day) + " " + allocStart + "~" + allocEnd
                        + " (마감 D-" + daysLeft + ")";
                LocalDate dateOfDay = weekStart.plusDays(dayCandidates.indexOf(day));
                placed.add(ActivityAllocationResponse.forTask(
                        task.getId(), task.getTitle(), day, dateOfDay, allocStart, allocEnd, slotMins, reason));
                remainingMinutes -= slotMins;
                placedAny = true;
            }
            if (!placedAny) break;
        }

        if (remainingMinutes > 0) {
            placed.add(new ActivityAllocationResponse(
                    ActivityAllocationResponse.SourceType.TASK, null, task.getId(),
                    "📋 " + task.getTitle(), ActivityCategory.OTHER,
                    null, null, null, 0, null,
                    "마감 전까지 빈 슬롯 부족 (남은 " + remainingMinutes + "분)"));
        }
        return placed;
    }

    private int specificityRank(ActivityGoal g) {
        int rank = 0;
        if (g.getPreferredDayOfWeek() != null) rank += 2;
        if (g.getPreferredStartTime() != null && g.getPreferredEndTime() != null) rank += 1;
        return rank;
    }

    private List<ActivityAllocationResponse> allocateOne(ActivityGoal goal,
                                                          Map<DayOfWeekEnum, List<TimeRange>> available) {
        List<ActivityAllocationResponse> placed = new ArrayList<>();
        int remainingMinutes = goal.getTargetHoursPerWeek() * 60;
        if (remainingMinutes <= 0) return placed;

        LocalTime windowStart = goal.getPreferredStartTime();
        LocalTime windowEnd = goal.getPreferredEndTime();
        boolean usingDefault = (windowStart == null || windowEnd == null);
        if (usingDefault) {
            LocalTime[] dw = CATEGORY_DEFAULT_WINDOW.getOrDefault(goal.getCategory(),
                    CATEGORY_DEFAULT_WINDOW.get(ActivityCategory.OTHER));
            windowStart = dw[0];
            windowEnd = dw[1];
        }

        List<DayOfWeekEnum> dayCandidates = goal.getPreferredDayOfWeek() != null
                ? List.of(goal.getPreferredDayOfWeek())
                : List.of(DayOfWeekEnum.MON, DayOfWeekEnum.TUE, DayOfWeekEnum.WED,
                        DayOfWeekEnum.THU, DayOfWeekEnum.FRI, DayOfWeekEnum.SAT, DayOfWeekEnum.SUN);

        // 라운드 로빈: 매 패스마다 각 요일에 한 세션씩만 배치
        // 한 패스에서 아무 요일에도 배치 못하면 종료 (무한루프 방지)
        while (remainingMinutes > 0) {
            boolean placedAny = false;
            for (DayOfWeekEnum day : dayCandidates) {
                if (remainingMinutes <= 0) break;
                List<TimeRange> dayList = available.get(day);
                if (dayList == null) continue;

                // 윈도우+버퍼 적용 후 첫 번째 사용 가능 슬롯 (가장 빠른 시각 = 가장 일찍 배치)
                TimeRange picked = null;
                for (TimeRange r : dayList) {
                    TimeRange clipped = clipWithBuffer(r, windowStart, windowEnd);
                    if (clipped != null && minutesOf(clipped) >= MIN_BLOCK_MINUTES) {
                        picked = clipped;
                        break;
                    }
                }
                if (picked == null) continue;

                int slotMins = Math.min(minutesOf(picked), Math.min(remainingMinutes, MAX_BLOCK_MINUTES));
                slotMins = (slotMins / 30) * 30;
                if (slotMins < MIN_BLOCK_MINUTES) continue;

                LocalTime allocStart = picked.start;
                LocalTime allocEnd = allocStart.plusMinutes(slotMins);

                subtract(available.get(day), new TimeRange(allocStart, allocEnd));

                String reason = buildReason(goal, day, allocStart, allocEnd, usingDefault);
                placed.add(ActivityAllocationResponse.forGoal(
                        goal.getId(), goal.getTitle(), goal.getCategory(),
                        day, allocStart, allocEnd, slotMins, reason));
                remainingMinutes -= slotMins;
                placedAny = true;
            }
            if (!placedAny) break;
        }

        if (remainingMinutes > 0) {
            placed.add(ActivityAllocationResponse.forGoal(
                    goal.getId(), goal.getTitle(), goal.getCategory(),
                    null, null, null, 0,
                    "선호 시간대 안에 빈 슬롯이 부족합니다 (남은 " + remainingMinutes + "분). 선호 시간대를 넓혀보세요."));
        }
        return placed;
    }

    /**
     * 윈도우로 자르기 + 양쪽 buffer 적용.
     * 단, 윈도우 경계가 raw slot 경계보다 안쪽이면 (즉 사용자가 좁힌 경우) 그쪽엔 버퍼 미적용.
     * 점유 시간 옆 경계에만 15분 휴식 보장.
     */
    private TimeRange clipWithBuffer(TimeRange r, LocalTime ws, LocalTime we) {
        LocalTime s = r.start.isBefore(ws) ? ws : r.start.plusMinutes(BUFFER_MINUTES);
        LocalTime e = r.end.isAfter(we) ? we : r.end.minusMinutes(BUFFER_MINUTES);
        return s.isBefore(e) ? new TimeRange(s, e) : null;
    }

    private void subtract(List<TimeRange> ranges, TimeRange used) {
        if (ranges == null) return;
        for (int i = 0; i < ranges.size(); i++) {
            TimeRange r = ranges.get(i);
            if (!r.start.isBefore(used.end) || !used.start.isBefore(r.end)) continue;
            ranges.remove(i);
            if (r.start.isBefore(used.start)) ranges.add(i++, new TimeRange(r.start, used.start));
            if (used.end.isBefore(r.end)) ranges.add(i, new TimeRange(used.end, r.end));
            break;
        }
    }

    private int minutesOf(TimeRange r) {
        return (int) Duration.between(r.start, r.end).toMinutes();
    }

    private String buildReason(ActivityGoal g, DayOfWeekEnum day, LocalTime s, LocalTime e, boolean usingDefault) {
        String win = usingDefault ? "카테고리 기본 시간대" : "선호 시간대";
        return g.getTitle() + " — " + dayKor(day) + " " + s + "~" + e + " (" + win + " 매칭)";
    }

    private String dayKor(DayOfWeekEnum d) {
        return switch (d) {
            case MON -> "월"; case TUE -> "화"; case WED -> "수";
            case THU -> "목"; case FRI -> "금"; case SAT -> "토"; case SUN -> "일";
        };
    }

    private record TimeRange(LocalTime start, LocalTime end) {}
}
