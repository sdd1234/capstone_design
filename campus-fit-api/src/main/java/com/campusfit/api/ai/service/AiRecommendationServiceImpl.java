package com.campusfit.api.ai.service;

import com.campusfit.api.academic.util.GradeInferrer;
import com.campusfit.api.ai.dto.RecommendationRequest;
import com.campusfit.api.ai.dto.RecommendationResponse;
import com.campusfit.api.ai.scoring.Persona;
import com.campusfit.api.ai.scoring.ScoreWeights;
import com.campusfit.api.ai.scoring.ScoredPlan;
import com.campusfit.api.ai.scoring.TimetableScorer;
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

/**
 * AI 시간표 추천 서비스.
 *
 * 동작 요약
 *  1. 학기/연도로 강의 풀 로드 (fetch join, 풀스캔 없음)
 *  2. AVOID 시간대 / 오전수업 제외 옵션으로 후보 풀 1차 필터링
 *  3. 페르소나(LIGHT_WEEK / MAJOR_FOCUS / RELAXED) 각각에 대해
 *     - 그리디 + 셔플로 N개 후보 생성
 *     - TimetableScorer로 점수화
 *     - 최고점 1개 선정
 *  4. 페르소나 간 Jaccard 유사도가 높으면 차순위로 교체해 다양성 보장
 *  5. 점수 + 추천 사유를 후보에 부착해 저장
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private static final int CANDIDATES_PER_PERSONA = 30;
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private final AiTimetableRecommendationRepository recommendationRepository;
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final TimetablePreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;
    private final TimetableScorer scorer;

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

        TimetablePreference pref = preferenceRepository
                .findByUserIdAndYearAndTermSeason(userId, request.year(), request.termSeason())
                .orElse(null);

        List<Lecture> pool = loadFilteredPool(request, pref);
        List<Lecture> desired = collectDesired(pool, request, pref);
        BuildContext ctx = buildContext(pref);

        List<ScoredPlan> picks = new ArrayList<>();
        for (Persona persona : Persona.values()) {
            ScoredPlan pick = pickForPersona(persona, pool, desired, pref, ctx, picks);
            if (pick != null) {
                picks.add(pick);
            }
        }

        for (int i = 0; i < picks.size(); i++) {
            ScoredPlan p = picks.get(i);
            int total = creditSum(p.lectures());
            rec.getCandidates().add(RecommendationCandidate.builder()
                    .recommendation(rec)
                    .rank(i + 1)
                    .totalCredits(total)
                    .persona(p.persona().name())
                    .personaLabel(p.persona().getLabel())
                    .score(p.score())
                    .reasonsText(String.join("\n", p.reasons()))
                    .lectures(new ArrayList<>(p.lectures()))
                    .build());
        }

        return RecommendationResponse.from(recommendationRepository.save(rec));
    }

    // ─────────────────────────────── 풀 로딩 & 필터 ───────────────────────────────

    private List<Lecture> loadFilteredPool(RecommendationRequest request, TimetablePreference pref) {
        Set<Long> excludeSet = (request.excludeLectureIds() != null && !request.excludeLectureIds().isEmpty())
                ? new HashSet<>(request.excludeLectureIds())
                : Collections.emptySet();
        Set<Long> occupiedIds = (request.occupiedLectureIds() != null && !request.occupiedLectureIds().isEmpty())
                ? new HashSet<>(request.occupiedLectureIds())
                : Collections.emptySet();

        List<Lecture> pool = new ArrayList<>(
                lectureRepository.findAllForRecommendation(request.year(), request.termSeason()).stream()
                        .filter(l -> !excludeSet.contains(l.getId()))
                        .filter(l -> !occupiedIds.contains(l.getId()))
                        // 현장실습/논문 등 고학점(>3) 또는 0학점 강의는 추천 풀에서 제외
                        .filter(l -> {
                            Integer c = l.getCourse().getCredits();
                            return c != null && c >= 1 && c <= 3;
                        })
                        .collect(Collectors.toList()));

        // 이미 등록된 강의(채플 등 학교 자동 배정 포함)의 시간대를 forbidden slot으로 → 충돌 강의 풀에서 제외
        if (!occupiedIds.isEmpty()) {
            List<LectureSchedule> occupiedSchedules = lectureRepository.findByIdsWithSchedules(occupiedIds).stream()
                    .flatMap(l -> l.getSchedules().stream())
                    .toList();
            if (!occupiedSchedules.isEmpty()) {
                pool.removeIf(l -> l.getSchedules().stream()
                        .anyMatch(cand -> occupiedSchedules.stream().anyMatch(occ -> overlapSchedules(cand, occ))));
            }
        }

        if (pref != null) {
            List<PreferredTimeRange> avoidRanges = pref.getTimeRanges().stream()
                    .filter(r -> r.getType() == TimeRangeType.AVOID).toList();
            if (!avoidRanges.isEmpty()) {
                pool.removeIf(l -> overlapsAvoidRange(l, avoidRanges));
            }
            if (pref.getPreferenceOption() != null
                    && Boolean.TRUE.equals(pref.getPreferenceOption().getExcludeMorning())) {
                pool.removeIf(l -> l.getSchedules().stream()
                        .anyMatch(s -> s.getStartTime().isBefore(LocalTime.of(10, 0))));
            }
        }
        return pool;
    }

    private boolean overlapSchedules(LectureSchedule a, LectureSchedule b) {
        if (a.getDayOfWeek() != b.getDayOfWeek()) return false;
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }

    private List<Lecture> collectDesired(List<Lecture> pool, RecommendationRequest request, TimetablePreference pref) {
        List<Lecture> desired = new ArrayList<>();
        if (pref != null) {
            Set<Long> desiredCourseIds = pref.getDesiredCourses().stream()
                    .map(DesiredCourse::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!desiredCourseIds.isEmpty()) {
                for (Lecture l : pool) {
                    if (desiredCourseIds.contains(l.getCourse().getId())) {
                        desired.add(l);
                    }
                }
            }
        }
        if (request.preferredLectureIds() != null) {
            for (Long id : request.preferredLectureIds()) {
                pool.stream().filter(l -> l.getId().equals(id)).findFirst()
                        .filter(l -> !desired.contains(l))
                        .ifPresent(desired::add);
            }
        }
        return desired;
    }

    private BuildContext buildContext(TimetablePreference pref) {
        BuildContext ctx = new BuildContext();
        ctx.targetCredits = 18;
        ctx.maxCredits = 21;
        ctx.maxDays = 5;
        if (pref != null && pref.getCreditPolicy() != null) {
            CreditPolicy cp = pref.getCreditPolicy();
            if (cp.getTargetCredits() != null) ctx.targetCredits = cp.getTargetCredits();
            if (cp.getMaxCredits() != null) ctx.maxCredits = cp.getMaxCredits();
            if (cp.getTargetMajorCredits() != null) ctx.targetMajorCredits = cp.getTargetMajorCredits();
            if (cp.getTargetGeneralCredits() != null) ctx.targetGeneralCredits = cp.getTargetGeneralCredits();
            if (cp.getTargetRemoteCredits() != null) ctx.targetRemoteCredits = cp.getTargetRemoteCredits();
        }
        if (pref != null && pref.getPreferenceOption() != null) {
            PreferenceOption po = pref.getPreferenceOption();
            if (po.getDept() != null && !po.getDept().isBlank()) ctx.preferredDept = po.getDept();
            if (po.getMaxDaysPerWeek() != null) ctx.maxDays = po.getMaxDaysPerWeek();
            ctx.grade = po.getGrade();
        }
        return ctx;
    }

    // ─────────────────────────────── 페르소나별 후보 선정 ───────────────────────────────

    private ScoredPlan pickForPersona(Persona persona, List<Lecture> pool, List<Lecture> desired,
            TimetablePreference pref, BuildContext ctx, List<ScoredPlan> alreadyPicked) {
        ScoreWeights weights = persona.weights();
        List<ScoredPlan> ranked = new ArrayList<>();

        // 매 호출마다 다른 결과를 내되, 페르소나마다 컨셉은 유지되도록 시드 분리
        Random rnd = new Random(System.nanoTime() ^ ((long) persona.ordinal() * 0x9E3779B97F4A7C15L));
        for (int attempt = 0; attempt < CANDIDATES_PER_PERSONA; attempt++) {
            List<Lecture> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled, rnd);

            List<Lecture> plan = buildGreedyPlan(shuffled, desired, ctx, persona);
            if (plan.isEmpty()) continue;

            TimetableScorer.ScoredResult sr = scorer.score(plan, pref, weights);
            ranked.add(new ScoredPlan(plan, persona, sr.score(), sr.reasons()));
        }

        ranked.sort(Comparator.comparingDouble(ScoredPlan::score).reversed());

        for (ScoredPlan cand : ranked) {
            if (alreadyPicked.stream().noneMatch(p -> jaccard(p.lectures(), cand.lectures()) > SIMILARITY_THRESHOLD)) {
                return cand;
            }
        }
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    // ─────────────────────────────── 그리디 후보 생성 ───────────────────────────────

    private List<Lecture> buildGreedyPlan(List<Lecture> shuffled, List<Lecture> desired,
            BuildContext ctx, Persona persona) {
        List<Lecture> plan = new ArrayList<>();
        Set<String> days = new HashSet<>();
        int credits = 0;

        // 1단계: 희망 강좌 최우선
        for (Lecture l : desired) {
            if (!plan.contains(l)) {
                credits = tryAddLecture(plan, days, l, credits, ctx.maxCredits, ctx.maxDays);
            }
        }

        // 2단계: 전공 학점 채우기 — 학년 strict 매칭
        int majorTarget = ctx.targetMajorCredits > 0 ? ctx.targetMajorCredits
                : (persona == Persona.MAJOR_FOCUS ? (ctx.targetCredits * 3) / 4 : (ctx.targetCredits * 2) / 3);
        credits = fillCategory(plan, days, credits, ctx,
                shuffled.stream()
                        .filter(this::isMajor)
                        .filter(l -> ctx.preferredDept == null || ctx.preferredDept.equals(l.getDept()))
                        .filter(l -> matchesGrade(l, ctx.grade, true))
                        .toList(),
                majorTarget, this::isMajor);

        // 3단계: 교양 — 원격은 4단계에서 별도 처리하므로 오프라인 교양만 풀에
        if (ctx.targetGeneralCredits > 0) {
            credits = fillCategory(plan, days, credits, ctx,
                    shuffled.stream()
                            .filter(this::isGeneral)
                            .filter(l -> !Boolean.TRUE.equals(l.getIsRemote()))
                            .toList(),
                    ctx.targetGeneralCredits,
                    l -> isGeneral(l) && !Boolean.TRUE.equals(l.getIsRemote()));
        }

        // 4단계: 원격 교양 (목표 > 0 일 때만) — 원격 강의는 교양 카테고리만 대상
        if (ctx.targetRemoteCredits > 0) {
            credits = fillCategory(plan, days, credits, ctx,
                    shuffled.stream()
                            .filter(l -> Boolean.TRUE.equals(l.getIsRemote()))
                            .filter(this::isGeneral)
                            .toList(),
                    ctx.targetRemoteCredits,
                    l -> Boolean.TRUE.equals(l.getIsRemote()) && isGeneral(l));
        }

        // 5단계: 목표 학점까지 나머지 채우기.
        //  - 전공이면: 본인 학과(strict) + 학년(strict) 매칭만 진입 — 타과 전공 차단
        //  - 비전공(교양 등): 학년/학과 무관 통과
        if (credits < ctx.targetCredits) {
            boolean excludeRemote = ctx.targetRemoteCredits == 0;
            List<Lecture> fillPool = shuffled.stream()
                    .filter(l -> !plan.contains(l))
                    .filter(l -> !excludeRemote || !Boolean.TRUE.equals(l.getIsRemote()))
                    .filter(l -> {
                        if (!isMajor(l)) return true;
                        boolean deptOk = ctx.preferredDept == null || ctx.preferredDept.equals(l.getDept());
                        return deptOk && matchesGrade(l, ctx.grade, true);
                    })
                    .sorted((a, b) -> Boolean.compare(isMajor(b), isMajor(a)))
                    .toList();
            for (Lecture l : fillPool) {
                if (credits >= ctx.targetCredits) break;
                credits = tryAddLecture(plan, days, l, credits, ctx.maxCredits, ctx.maxDays);
            }
        }
        return plan;
    }

    /**
     * 사용자 학년과 강의 대상학년 매칭.
     *  - userGrade == null (전학년 사용자) → 통과
     *  - targetGrade 명시 → 정확 일치만 통과
     *  - targetGrade null → 강의명 핵심어로 학년 추정 (자료구조→2 / 알고리즘→3 / 캡스톤(2)→4 등)
     *  - 추정도 안 되면: strict면 제외, 아니면 통과
     *
     * 참고: lectureNumber 첫 숫자는 학년이 아니라 단순 시퀀스이므로 사용하지 않음.
     */
    private boolean matchesGrade(Lecture l, Integer userGrade, boolean strict) {
        if (userGrade == null) return true;
        Integer tg = l.getTargetGrade();
        if (tg != null) return tg.equals(userGrade);
        Integer inferred = GradeInferrer.inferFromCourseName(
                l.getCourse() != null ? l.getCourse().getName() : null);
        if (inferred != null) return inferred.equals(userGrade);
        return !strict;
    }

    private int fillCategory(List<Lecture> plan, Set<String> days, int credits, BuildContext ctx,
            List<Lecture> categoryPool, int categoryTarget, java.util.function.Predicate<Lecture> matcher) {
        int done = creditSum(plan.stream().filter(matcher).toList());
        if (done >= categoryTarget) return credits;
        for (Lecture l : categoryPool) {
            if (done >= categoryTarget) break;
            if (plan.contains(l)) continue;
            int c = creditOf(l);
            int prev = credits;
            credits = tryAddLecture(plan, days, l, credits, ctx.maxCredits, ctx.maxDays);
            if (credits > prev) done += c;
        }
        return credits;
    }

    private int tryAddLecture(List<Lecture> plan, Set<String> days, Lecture l, int credits, int max, int maxDays) {
        int c = creditOf(l);
        if (credits + c > max) return credits;
        Set<String> lecDays = l.getSchedules().stream().map(s -> s.getDayOfWeek().name()).collect(Collectors.toSet());
        Set<String> combined = new HashSet<>(days);
        combined.addAll(lecDays);
        if (combined.size() > maxDays) return credits;
        if (hasConflict(plan, l)) return credits;
        plan.add(l);
        days.addAll(lecDays);
        return credits + c;
    }

    // ─────────────────────────────── 보조 ───────────────────────────────

    private int creditOf(Lecture l) {
        Integer c = l.getCourse().getCredits();
        return c != null ? c : 0;
    }

    private int creditSum(List<Lecture> lectures) {
        return lectures.stream().mapToInt(this::creditOf).sum();
    }

    private boolean isMajor(Lecture l) {
        String cat = l.getCourse().getCategory();
        return cat != null && cat.contains("전공");
    }

    private boolean isGeneral(Lecture l) {
        String cat = l.getCourse().getCategory();
        // "교직"은 특수과목이라 일반 학생 추천 대상 외 — 쿼리에서 이미 제외되지만 방어적 필터
        return cat != null && cat.contains("교양") && !cat.contains("교직");
    }

    private boolean overlapsAvoidRange(Lecture l, List<PreferredTimeRange> ranges) {
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

    private double jaccard(List<Lecture> a, List<Lecture> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<Long> setA = a.stream().map(Lecture::getId).collect(Collectors.toSet());
        Set<Long> setB = b.stream().map(Lecture::getId).collect(Collectors.toSet());
        Set<Long> inter = new HashSet<>(setA);
        inter.retainAll(setB);
        Set<Long> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
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

    private static class BuildContext {
        int targetCredits;
        int maxCredits;
        int maxDays;
        int targetMajorCredits;
        int targetGeneralCredits;
        int targetRemoteCredits;
        String preferredDept;
        Integer grade;
    }
}
