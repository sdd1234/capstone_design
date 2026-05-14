package com.campusfit.api.timemanagement.service;

import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.ActivityGoal;
import com.campusfit.api.domain.AppliedAllocation;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.ActivityGoalRepository;
import com.campusfit.api.repository.AppliedAllocationRepository;
import com.campusfit.api.repository.UserRepository;
import com.campusfit.api.timemanagement.dto.AppliedAllocationResponse;
import com.campusfit.api.timemanagement.dto.ApplyAllocationsRequest;
import com.campusfit.api.timemanagement.dto.WeeklySummaryResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppliedAllocationService {

    private final AppliedAllocationRepository repository;
    private final ActivityGoalRepository goalRepository;
    private final UserRepository userRepository;

    /** 주의 시작 = 그 주 월요일. ISO 표준. */
    public static LocalDate weekStartOf(LocalDate date) {
        int dow = date.getDayOfWeek().getValue(); // MON=1 .. SUN=7
        return date.minusDays(dow - 1);
    }

    public static LocalDate currentWeekStart() {
        return weekStartOf(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<AppliedAllocationResponse> listForWeek(Long userId, LocalDate weekStart) {
        LocalDate week = weekStart != null ? weekStart : currentWeekStart();
        return repository.findByUserAndWeek(userId, week).stream()
                .map(AppliedAllocationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppliedAllocationResponse> listAll(Long userId) {
        return repository.findAllByUserIdWithGoal(userId).stream()
                .map(AppliedAllocationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * (weekStart, goalId) 단위 replace: 요청에 포함된 (weekStart, goalId) 조합의 기존 적용분만 삭제 후 신규 저장.
     * 다른 주 또는 다른 goal의 적용분은 보존 → 완료 history 영속.
     */
    public List<AppliedAllocationResponse> applyAll(Long userId, ApplyAllocationsRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        LocalDate week = req.weekStart() != null ? req.weekStart() : currentWeekStart();

        Map<Long, ActivityGoal> goalCache = new HashMap<>();
        Set<Long> goalIdsInRequest = new HashSet<>();
        for (ApplyAllocationsRequest.Item it : req.items()) {
            goalIdsInRequest.add(it.goalId());
            if (goalCache.containsKey(it.goalId())) continue;
            ActivityGoal g = goalRepository.findById(it.goalId())
                    .orElseThrow(() -> BusinessException.notFound("활동 목표 ID " + it.goalId() + " 없음"));
            if (!g.getUser().getId().equals(userId)) {
                throw BusinessException.forbidden("본인 활동 목표만 적용 가능합니다.");
            }
            goalCache.put(it.goalId(), g);
        }

        for (Long goalId : goalIdsInRequest) {
            repository.deleteByUserIdAndWeekStartAndGoalId(userId, week, goalId);
        }
        repository.flush();

        req.items().forEach(it ->
                repository.save(AppliedAllocation.builder()
                        .user(user)
                        .goal(goalCache.get(it.goalId()))
                        .weekStart(week)
                        .dayOfWeek(it.dayOfWeek())
                        .startTime(it.startTime())
                        .endTime(it.endTime())
                        .isCompleted(false)
                        .build()));

        return repository.findByUserAndWeek(userId, week).stream()
                .map(AppliedAllocationResponse::from)
                .collect(Collectors.toList());
    }

    public AppliedAllocationResponse toggleCompleted(Long userId, Long allocationId, boolean completed) {
        AppliedAllocation a = repository.findById(allocationId)
                .orElseThrow(() -> BusinessException.notFound("적용분을 찾을 수 없습니다."));
        if (!a.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("본인 적용분만 수정 가능합니다.");
        }
        a.setIsCompleted(completed);
        a.setCompletedAt(completed ? LocalDateTime.now() : null);
        return AppliedAllocationResponse.from(a);
    }

    @Transactional(readOnly = true)
    public WeeklySummaryResponse weeklySummary(Long userId, LocalDate weekStart) {
        LocalDate week = weekStart != null ? weekStart : currentWeekStart();
        List<AppliedAllocation> rows = repository.findByUserAndWeek(userId, week);

        Map<Long, List<AppliedAllocation>> byGoal = rows.stream()
                .collect(Collectors.groupingBy(a -> a.getGoal().getId()));

        List<WeeklySummaryResponse.GoalProgress> byGoalList = new ArrayList<>();
        int totalApplied = 0, totalCompleted = 0;
        for (Map.Entry<Long, List<AppliedAllocation>> entry : byGoal.entrySet()) {
            int applied = entry.getValue().stream()
                    .mapToInt(a -> (int) Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                    .sum();
            int completed = entry.getValue().stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCompleted()))
                    .mapToInt(a -> (int) Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                    .sum();
            totalApplied += applied;
            totalCompleted += completed;
            ActivityGoal g = entry.getValue().get(0).getGoal();
            int pct = applied > 0 ? (int) Math.round(100.0 * completed / applied) : 0;
            byGoalList.add(new WeeklySummaryResponse.GoalProgress(
                    g.getId(), g.getTitle(), g.getCategory(), g.getTargetHoursPerWeek(),
                    applied, completed, pct));
        }
        byGoalList.sort((a, b) -> b.appliedMinutes() - a.appliedMinutes());
        return new WeeklySummaryResponse(week, totalApplied, totalCompleted, byGoalList);
    }

    /** 주 단위 wipe — 한 주의 모든 적용분 삭제. */
    public void clearWeek(Long userId, LocalDate weekStart) {
        LocalDate week = weekStart != null ? weekStart : currentWeekStart();
        for (AppliedAllocation a : repository.findByUserAndWeek(userId, week)) {
            repository.delete(a);
        }
    }

    public void clearAll(Long userId) {
        repository.deleteByUserId(userId);
    }
}
