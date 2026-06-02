package com.campusfit.api.repository;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.PersonalBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PersonalBlockRepository extends JpaRepository<PersonalBlock, Long> {
    List<PersonalBlock> findByUserIdAndYearAndTermSeasonOrderByDayOfWeekAscStartTimeAsc(
            Long userId, Integer year, TermSeason termSeason);
}
