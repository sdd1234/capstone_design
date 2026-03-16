package com.campusfit.api.ai.service;

import com.campusfit.api.ai.dto.RecommendationRequest;
import com.campusfit.api.ai.dto.RecommendationResponse;
import com.campusfit.api.common.enums.TimeRangeType;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final AiTimetableRecommendationRepository recommendationRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final TimetablePreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendationResponse create(Long userId, RecommendationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            paramsJson = "{}";
        }

        AiTimetableRecommendation rec = AiTimetableRecommendation.builder()
                .user(user)
                .year(request.year())
                .termSeason(request.termSeason())
                .requestParamsJson(paramsJson)
                .build();

        // 해당 학기 선호 설정 로드 (없으면 기본 추천)
        Optional<TimetablePreference> prefOpt = preferenceRepository
                .findByUserIdAndYearAndTermSeason(userId, request.year(), request.termSeason());

        // 해당 학기 전체 강의 후보 (명시적 제외 포함)
        Set<Long> excludeSet = (request.excludeLectureIds() != null && !request.excludeLectureIds().isEmpty())
                ? new HashSet<>(request.excludeLectureIds())
                : Collections.emptySet();
        List<Lecture> candidates = new ArrayList<>(lectureRepository.findAll().stream()
                .filter(l -> l.getYear().equals(request.year()) && l.getTermSeason() == request.termSeason())
                .filter(l -> !excludeSet.contains(l.getId()))
                .collect(Collectors.toList()));

        // 선호 설정 기반 필터링
        if (prefOpt.isPresent()) {
            TimetablePreference pref = prefOpt.get();
            // AVOID 시간대 강의 제거
            List<PreferredTimeRange> avoidRanges = pref.getTimeRanges().stream()
                    .filter(r -> r.getType() == TimeRangeType.AVOID).toList();
            if (!avoidRanges.isEmpty()) {
                candidates.removeIf(l -> hasScheduleInRanges(l, avoidRanges));
            }
            // 오전 제외 옵션 처리 (오전 = 12시 이전에 시작하는 강의)
            if (pref.getPreferenceOption() != null
                    && Boolean.TRUE.equals(pref.getPreferenceOption().getExcludeMorning())) {
                candidates.removeIf(l -> l.getSchedules().stream()
                        .anyMatch(s -> s.getStartTime().isBefore(LocalTime.NOON)));
            }
        }

        // 희망 강좌 우선 선택
        List<Lecture> prioritized = new ArrayList<>();
        if (prefOpt.isPresent()) {
            Set<Long> desiredCourseIds = prefOpt.get().getDesiredCourses().stream()
                    .map(DesiredCourse::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!desiredCourseIds.isEmpty()) {
                for (Lecture l : candidates) {
                    if (desiredCourseIds.contains(l.getCourse().getId())) {
                        prioritized.add(l);
                    }
                }
            }
        }
        if (request.preferredLectureIds() != null) {
            for (Long id : request.preferredLectureIds()) {
                candidates.stream().filter(l -> l.getId().equals(id)).findFirst()
                        .filter(l -> !prioritized.contains(l))
                        .ifPresent(prioritized::add);
            }
        }

        // 나머지 강의에서 충돌 없이 추가
        for (Lecture l : candidates) {
            if (!prioritized.contains(l) && !hasConflict(prioritized, l)) {
                prioritized.add(l);
            }
        }

        // 학점 정책 적용
        int targetCredits = 18;
        int maxCredits = 21;
        if (prefOpt.isPresent() && prefOpt.get().getCreditPolicy() != null) {
            CreditPolicy cp = prefOpt.get().getCreditPolicy();
            if (cp.getTargetCredits() != null)
                targetCredits = cp.getTargetCredits();
            if (cp.getMaxCredits() != null)
                maxCredits = cp.getMaxCredits();
        }

        // 최대 수강 일수 적용
        int maxDays = 5;
        if (prefOpt.isPresent() && prefOpt.get().getPreferenceOption() != null
                && prefOpt.get().getPreferenceOption().getMaxDaysPerWeek() != null) {
            maxDays = prefOpt.get().getPreferenceOption().getMaxDaysPerWeek();
        }

        // 목표 학점에 맞게 강의 선택 (3개 후보 생성)
        List<List<Lecture>> plans = buildPlans(prioritized, targetCredits, maxCredits, maxDays, 5);

        for (int i = 0; i < plans.size(); i++) {
            List<Lecture> plan = plans.get(i);
            int total = plan.stream().mapToInt(l -> l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0)
                    .sum();
            rec.getCandidates().add(RecommendationCandidate.builder()
                    .recommendation(rec)
                    .rank(i + 1)
                    .totalCredits(total)
                    .lectures(new ArrayList<>(plan))
                    .build());
        }

        return RecommendationResponse.from(recommendationRepository.save(rec));
    }

    /** 시간 충돌 없이 목표 학점에 근접하는 강의 조합 최대 maxPlans개 생성 */
    private List<List<Lecture>> buildPlans(List<Lecture> pool, int target, int max, int maxDays, int maxPlans) {
        List<List<Lecture>> results = new ArrayList<>();
        // 셔플로 다양한 후보 생성
        List<Lecture> shuffled = new ArrayList<>(pool);
        for (int attempt = 0; attempt < maxPlans * 3 && results.size() < maxPlans; attempt++) {
            Collections.shuffle(shuffled);
            List<Lecture> plan = new ArrayList<>();
            int credits = 0;
            Set<String> days = new HashSet<>();
            for (Lecture l : shuffled) {
                int c = l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0;
                if (credits + c > max)
                    continue;
                Set<String> lecDays = l.getSchedules().stream().map(s -> s.getDayOfWeek().name())
                        .collect(Collectors.toSet());
                Set<String> combined = new HashSet<>(days);
                combined.addAll(lecDays);
                if (combined.size() > maxDays)
                    continue;
                if (!hasConflict(plan, l)) {
                    plan.add(l);
                    credits += c;
                    days.addAll(lecDays);
                }
                if (credits >= target)
                    break;
            }
            if (!plan.isEmpty() && results.stream().noneMatch(r -> new HashSet<>(r).equals(new HashSet<>(plan)))) {
                results.add(plan);
            }
        }
        // 후보가 없으면 빈 리스트 하나라도 반환
        if (results.isEmpty())
            results.add(new ArrayList<>(pool.subList(0, Math.min(pool.size(), 6))));
        return results;
    }

    private boolean hasScheduleInRanges(Lecture l, List<PreferredTimeRange> ranges) {
        for (LectureSchedule s : l.getSchedules()) {
            for (PreferredTimeRange r : ranges) {
                if ((r.getDayOfWeek() == null || r.getDayOfWeek() == s.getDayOfWeek())
                        && s.getStartTime().isBefore(r.getEndTime())
                        && r.getStartTime().isBefore(s.getEndTime())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasConflict(List<Lecture> existing, Lecture newL) {
        for (Lecture e : existing) {
            for (LectureSchedule es : e.getSchedules()) {
                for (LectureSchedule ns : newL.getSchedules()) {
                    if (es.getDayOfWeek() == ns.getDayOfWeek()
                            && es.getStartTime().isBefore(ns.getEndTime())
                            && ns.getStartTime().isBefore(es.getEndTime())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> list(Long userId) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(RecommendationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse get(Long userId, Long recommendationId) {
        AiTimetableRecommendation rec = findOwned(userId, recommendationId);
        return RecommendationResponse.from(rec);
    }

    @Override
    public void delete(Long userId, Long recommendationId) {
        AiTimetableRecommendation rec = findOwned(userId, recommendationId);
        recommendationRepository.delete(rec);
    }

    private AiTimetableRecommendation findOwned(Long userId, Long recId) {
        AiTimetableRecommendation rec = recommendationRepository.findById(recId)
                .orElseThrow(() -> BusinessException.notFound("추천 결과를 찾을 수 없습니다."));
        if (!rec.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        return rec;
    }
}
