package com.campusfit.api.repository;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.Course;
import com.campusfit.api.domain.Lecture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

        List<Lecture> findByCourseAndYearAndTermSeason(Course course, Integer year, TermSeason termSeason);

        Optional<Lecture> findByLectureNumberAndYearAndTermSeason(String lectureNumber, Integer year,
                        TermSeason termSeason);

        @Query("SELECT DISTINCT l.dept FROM Lecture l WHERE l.dept IS NOT NULL AND l.year = :year AND l.termSeason = :term ORDER BY l.dept")
        List<String> findDistinctDepts(@Param("year") Integer year, @Param("term") TermSeason term);

        @Query("SELECT l FROM Lecture l JOIN l.course c WHERE c.university.id = :uniId AND l.year = :year AND l.termSeason = :term AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(l.professor) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (:category IS NULL OR c.category = :category) AND (:area IS NULL OR l.area = :area)")
        List<Lecture> searchLectures(@Param("uniId") Long uniId, @Param("year") Integer year,
                        @Param("term") TermSeason term, @Param("keyword") String keyword,
                        @Param("category") String category, @Param("area") String area);
}
