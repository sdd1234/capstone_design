package com.campusfit.api.repository;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.TimetablePreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetablePreferenceRepository extends JpaRepository<TimetablePreference, Long> {
    Optional<TimetablePreference> findByUserIdAndYearAndTermSeason(Long userId, Integer year, TermSeason termSeason);
}
