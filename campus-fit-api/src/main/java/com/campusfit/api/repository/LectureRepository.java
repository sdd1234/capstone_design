package com.campusfit.api.repository;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.Course;
import com.campusfit.api.domain.Lecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findByCourseAndYearAndTermSeason(Course course, Integer year, TermSeason termSeason);

    @Query("SELECT l FROM Lecture l JOIN l.course c WHERE c.university.id = :uniId AND l.year = :year AND l.termSeason = :term AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Lecture> searchLectures(@Param("uniId") Long uniId, @Param("year") Integer year,
            @Param("term") TermSeason term, @Param("keyword") String keyword);
}
