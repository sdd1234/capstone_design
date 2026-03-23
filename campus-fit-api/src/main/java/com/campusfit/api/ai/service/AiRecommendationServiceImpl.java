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
            // 9시 수업 제외 옵션 처리 (10시 이전에 시작하는 강의)
            if (pref.getPreferenceOption() != null
                    && Boolean.TRUE.equals(pref.getPreferenceOption().getExcludeMorning())) {
                candidates.removeIf(l -> l.getSchedules().stream()
                        .anyMatch(s -> s.getStartTime().isBefore(LocalTime.of(10, 0))));
            }
        }

        // 희망 강좌만 추출 (AI 알고리즘에서 1순위 배치)
        List<Lecture> desiredLectures = new ArrayList<>();
        if (prefOpt.isPresent()) {
            Set<Long> desiredCourseIds = prefOpt.get().getDesiredCourses().stream()
                    .map(DesiredCourse::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!desiredCourseIds.isEmpty()) {
                for (Lecture l : candidates) {
                    if (desiredCourseIds.contains(l.getCourse().getId())) {
                        desiredLectures.add(l);
                    }
                }
            }
        }
        if (request.preferredLectureIds() != null) {
            for (Long id : request.preferredLectureIds()) {
                candidates.stream().filter(l -> l.getId().equals(id)).findFirst()
                        .filter(l -> !desiredLectures.contains(l))
                        .ifPresent(desiredLectures::add);
            }
        }

        // 학점 정책 로드
        int targetCredits = 18;
        int maxCredits = 21;
        int targetMajorCredits = 0;
        int targetGeneralCredits = 0;
        int targetRemoteCredits = 0;
        if (prefOpt.isPresent() && prefOpt.get().getCreditPolicy() != null) {
            CreditPolicy cp = prefOpt.get().getCreditPolicy();
            if (cp.getTargetCredits() != null)
                targetCredits = cp.getTargetCredits();
            if (cp.getMaxCredits() != null)
                maxCredits = cp.getMaxCredits();
            if (cp.getTargetMajorCredits() != null)
                targetMajorCredits = cp.getTargetMajorCredits();
            if (cp.getTargetGeneralCredits() != null)
                targetGeneralCredits = cp.getTargetGeneralCredits();
            if (cp.getTargetRemoteCredits() != null)
                targetRemoteCredits = cp.getTargetRemoteCredits();
        }

        // 전공 우선 및 학과 필터 로드
        boolean preferMajorOnly = true; // 항상 전공 우선
        String preferredDept = null;
        int maxDays = 5;
        Integer grade = null;
        if (prefOpt.isPresent() && prefOpt.get().getPreferenceOption() != null) {
            PreferenceOption po = prefOpt.get().getPreferenceOption();
            if (po.getDept() != null && !po.getDept().isBlank())
                preferredDept = po.getDept();
            if (po.getMaxDaysPerWeek() != null)
                maxDays = po.getMaxDaysPerWeek();
            grade = po.getGrade();
        }

        // 우선순위별 강의 선택: 희망과목 → 전공 → 교양 → 원격 → 나머지 (3개 후보 생성)
        List<List<Lecture>> plans = buildCategorizedPlans(
                candidates, desiredLectures, targetCredits, maxCredits, maxDays,
                targetMajorCredits, targetGeneralCredits, targetRemoteCredits,
                preferMajorOnly, preferredDept, grade, 3);

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

    /**
     * 우선순위대로 시간표 후보를 maxPlans건 생성:
     * 1순위: 희망 수강과목 (desired)
     * 2순위: 전공 목표학점 채우기 (학과/학년 필터 적용)
     * 3순위: 교양 목표학점 채우기
     * 4순위: 원격 목표학점 채우기 (targetRemoteCredits > 0 일 때만)
     * 5순위: 나머지 학점 채우기 (원격 0목표면 원격 완전 제외)
     */
    private List<List<Lecture>> buildCategorizedPlans(
            List<Lecture> pool, List<Lecture> desired,
            int target, int max, int maxDays,
            int targetMajorCredits, int targetGeneralCredits, int targetRemoteCredits,
            boolean preferMajorOnly, String preferredDept, Integer grade, int maxPlans) {

        List<List<Lecture>> results = new ArrayList<>();
        List<Lecture> shuffled = new ArrayList<>(pool);

        for (int attempt = 0; attempt < maxPlans * 5 && results.size() < maxPlans; attempt++) {
            Collections.shuffle(shuffled);

            List<Lecture> plan = new ArrayList<>();
            int credits = 0;
            Set<String> days = new HashSet<>();

            // 1단계: 희망 수강과목 최우선 배치
            for (Lecture l : desired) {
                if (!plan.contains(l))
                    credits = tryAddLecture(plan, days, l, credits, max, maxDays);
            }

            // 2단계: 전공 목표 학점 채우기
            int majorFill = targetMajorCredits > 0 ? targetMajorCredits
                    : (preferMajorOnly ? (target * 2) / 3 : 0);
            if (majorFill > 0) {
                int majorDone = creditSum(plan.stream().filter(this::isMajor).collect(Collectors.toList()));
                if (majorDone < majorFill) {
                    final String dept = preferredDept;
                    List<Lecture> majorPool = shuffled.stream()
                            .filter(this::isMajor)
                            .filter(l -> dept == null || dept.equals(l.getDept()))
                            .filter(l -> grade == null || l.getTargetGrade() == null
                                    || l.getTargetGrade().equals(grade))
                            .filter(l -> !plan.contains(l))
                            .collect(Collectors.toList());
                    for (Lecture l : majorPool) {
                        if (majorDone >= majorFill)
                            break;
                        int c = l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0;
                        int prev = credits;
                        credits = tryAddLecture(plan, days, l, credits, max, maxDays);
                        if (credits > prev)
                            majorDone += c;
                    }
                }
            }

            // 3단계: 교양 목표 학점 채우기
            if (targetGeneralCredits > 0) {
                int genDone = creditSum(plan.stream().filter(this::isGeneral).collect(Collectors.toList()));
                if (genDone < targetGeneralCredits) {
                    List<Lecture> generalPool = shuffled.stream()
                            .filter(this::isGeneral)
                            .filter(l -> !plan.contains(l))
                            .collect(Collectors.toList());
                    for (Lecture l : generalPool) {
                        if (genDone >= targetGeneralCredits)
                            break;
                        int c = l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0;
                        int prev = credits;
                        credits = tryAddLecture(plan, days, l, credits, max, maxDays);
                        if (credits > prev)
                            genDone += c;
                    }
                }
            }

            // 4단계: 원격 목표 학점 채우기 (목표 > 0 일 때만)
            if (targetRemoteCredits > 0) {
                int remoteDone = creditSum(plan.stream()
                        .filter(l -> Boolean.TRUE.equals(l.getIsRemote())).collect(Collectors.toList()));
                if (remoteDone < targetRemoteCredits) {
                    List<Lecture> remotePool = shuffled.stream()
                            .filter(l -> Boolean.TRUE.equals(l.getIsRemote()))
                            .filter(l -> !plan.contains(l))
                            .collect(Collectors.toList());
                    for (Lecture l : remotePool) {
                        if (remoteDone >= targetRemoteCredits)
                            break;
                        int c = l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0;
                        int prev = credits;
                        credits = tryAddLecture(plan, days, l, credits, max, maxDays);
                        if (credits > prev)
                            remoteDone += c;
                    }
                }
            }

            // 5단계: 나머지 학점 채우기 (원격 목표 0이면 원격 완전 제외; 전공우선이면 전공 먼저)
            if (credits < target) {
                boolean excludeRemote = targetRemoteCredits == 0;
                List<Lecture> fillPool = shuffled.stream()
                        .filter(l -> !plan.contains(l))
                        .filter(l -> !excludeRemote || !Boolean.TRUE.equals(l.getIsRemote()))
                        .collect(Collectors.toList());
                if (preferMajorOnly) {
                    fillPool.sort((a, b) -> {
                        boolean am = isMajor(a), bm = isMajor(b);
                        return (am == bm) ? 0 : am ? -1 : 1;
                    });
                }
                for (Lecture l : fillPool) {
                    if (credits >= target)
                        break;
                    credits = tryAddLecture(plan, days, l, credits, max, maxDays);
                }
            }

            if (!plan.isEmpty()
                    && results.stream().noneMatch(r -> new HashSet<>(r).equals(new HashSet<>(plan)))) {
                results.add(plan);
            }
        }
        if (results.isEmpty())
            results.add(new ArrayList<>(pool.subList(0, Math.min(pool.size(), 6))));
        return results;
    }

    private int creditSum(List<Lecture> lectures) {
        return lectures.stream()
                .mapToInt(l -> l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0).sum();
    }

    /** 강의 추가 시도. 성공 시 누적 학점 반환, 실패 시 기존 학점 반환 */
    private int tryAddLecture(List<Lecture> plan, Set<String> days, Lecture l, int credits, int max, int maxDays) {
        int c = l.getCourse().getCredits() != null ? l.getCourse().getCredits() : 0;
        if (credits + c > max)
            return credits;
        Set<String> lecDays = l.getSchedules().stream().map(s -> s.getDayOfWeek().name()).collect(Collectors.toSet());
        Set<String> combined = new HashSet<>(days);
        combined.addAll(lecDays);
        if (combined.size() > maxDays)
            return credits;
        if (hasConflict(plan, l))
            return credits;
        plan.add(l);
        days.addAll(lecDays);
        return credits + c;
    }

    private boolean isMajor(Lecture l) {
        String cat = l.getCourse().getCategory();
        return cat != null && cat.contains("전공");
    }

    private boolean isGeneral(Lecture l) {
        String cat = l.getCourse().getCategory();
        return cat != null && (cat.contains("교양") || cat.contains("교직"));
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
