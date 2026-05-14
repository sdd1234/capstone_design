package com.campusfit.api.repository;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.Timetable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Timetable> findByUserIdAndYearAndTermSeason(Long userId, Integer year, TermSeason termSeason);

    /** items + lecture 까지 fetch join. schedules는 별도 batch (LectureRepository.findByIdsWithSchedules) 로 로딩 권장. */
    @Query("SELECT DISTINCT t FROM Timetable t "
            + "LEFT JOIN FETCH t.items i "
            + "LEFT JOIN FETCH i.lecture "
            + "WHERE t.user.id = :userId AND t.year = :year AND t.termSeason = :term")
    List<Timetable> findByUserIdAndYearAndTermSeasonWithLectures(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("term") TermSeason term);
}
