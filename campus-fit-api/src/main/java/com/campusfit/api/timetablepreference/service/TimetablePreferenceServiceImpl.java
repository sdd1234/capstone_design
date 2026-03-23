package com.campusfit.api.timetablepreference.service;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import com.campusfit.api.timetablepreference.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional
public class TimetablePreferenceServiceImpl implements TimetablePreferenceService {

        private final TimetablePreferenceRepository preferenceRepository;
        private final UserRepository userRepository;

        @Override
        public TimetablePreferenceResponse save(Long userId, TimetablePreferenceRequest req) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

                // 기존 선호 설정이 있으면 삭제 후 재생성
                preferenceRepository.findByUserIdAndYearAndTermSeason(userId, req.year(), req.termSeason())
                                .ifPresent(preferenceRepository::delete);
                preferenceRepository.flush();

                TimetablePreference pref = TimetablePreference.builder()
                                .user(user)
                                .year(req.year())
                                .termSeason(req.termSeason())
                                .build();

                if (req.timeRanges() != null) {
                        req.timeRanges().forEach(tr -> pref.getTimeRanges().add(
                                        PreferredTimeRange.builder()
                                                        .preference(pref)
                                                        .type(tr.type())
                                                        .dayOfWeek(tr.dayOfWeek())
                                                        .startTime(tr.startTime())
                                                        .endTime(tr.endTime())
                                                        .build()));
                }

                if (req.desiredCourses() != null) {
                        req.desiredCourses().forEach(dc -> pref.getDesiredCourses().add(
                                        DesiredCourse.builder()
                                                        .preference(pref)
                                                        .courseId(dc.courseId())
                                                        .rawText(dc.rawText())
                                                        .priority(dc.priority() != null ? dc.priority() : 0)
                                                        .build()));
                }

                if (req.creditPolicy() != null) {
                        CreditPolicy cp = CreditPolicy.builder()
                                        .preference(pref)
                                        .minCredits(req.creditPolicy().minCredits())
                                        .maxCredits(req.creditPolicy().maxCredits())
                                        .targetCredits(req.creditPolicy().targetCredits())
                                        .targetMajorCredits(req.creditPolicy().targetMajorCredits())
                                        .targetGeneralCredits(req.creditPolicy().targetGeneralCredits())
                                        .targetRemoteCredits(req.creditPolicy().targetRemoteCredits())
                                        .build();
                        pref.setCreditPolicy(cp);
                }

                if (req.options() != null) {
                        PreferenceOption po = PreferenceOption.builder()
                                        .preference(pref)
                                        .excludeMorning(Boolean.TRUE.equals(req.options().excludeMorning()))
                                        .allowGapsMinutes(req.options().allowGapsMinutes())
                                        .maxDaysPerWeek(req.options().maxDaysPerWeek())
                                        .dept(req.options().dept())
                                        .preferMajorOnly(Boolean.TRUE.equals(req.options().preferMajorOnly()))
                                        .grade(req.options().grade())
                                        .build();
                        pref.setPreferenceOption(po);
                }

                return TimetablePreferenceResponse.from(preferenceRepository.save(pref));
        }

        @Override
        @Transactional(readOnly = true)
        public TimetablePreferenceResponse get(Long userId, Integer year, String termSeason) {
                TermSeason ts = TermSeason.valueOf(termSeason);
                TimetablePreference pref = preferenceRepository.findByUserIdAndYearAndTermSeason(userId, year, ts)
                                .orElseThrow(() -> BusinessException.notFound("시간표 선호 설정을 찾을 수 없습니다."));
                return TimetablePreferenceResponse.from(pref);
        }
}
