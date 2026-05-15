package com.campusfit.api.repository;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.Course;
import com.campusfit.api.domain.Lecture;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

        List<Lecture> findByCourseAndYearAndTermSeason(Course course, Integer year, TermSeason termSeason);

        @Query("SELECT l FROM Lecture l JOIN FETCH l.course WHERE l.targetGrade IS NULL")
        List<Lecture> findAllWithCourseWhereTargetGradeNull();

        @Query("SELECT COUNT(l) FROM Lecture l WHERE l.targetGrade IS NULL")
        long countByTargetGradeNull();

        Optional<Lecture> findByLectureNumberAndYearAndTermSeason(String lectureNumber, Integer year,
                        TermSeason termSeason);

        @Query("SELECT DISTINCT l.dept FROM Lecture l WHERE l.dept IS NOT NULL AND l.year = :year AND l.termSeason = :term ORDER BY l.dept")
        List<String> findDistinctDepts(@Param("year") Integer year, @Param("term") TermSeason term);

        @Query("SELECT l FROM Lecture l JOIN l.course c WHERE c.university.id = :uniId AND l.year = :year AND l.termSeason = :term AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(l.professor) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (:category IS NULL OR c.category = :category) AND (:area IS NULL OR l.area = :area)")
        List<Lecture> searchLectures(@Param("uniId") Long uniId, @Param("year") Integer year,
                        @Param("term") TermSeason term, @Param("keyword") String keyword,
                        @Param("category") String category, @Param("area") String area);

        /**
         * AI 추천용: 학기/연도로 강의 + course + schedules 한 번에 로딩 (N+1 방지).
         * 필터:
         *  - 학점 1~3 (캡스톤·졸업프로젝트 등 고학점 예외 과목 제외)
         *  - 1학년 필수교양(채플/커뮤니티잉글리쉬/대학생활과진로설계/교양세미나/기독교의이해)
         *    학교가 자동 배정하므로 추천 후보에서 제외
         */
        @Query("SELECT DISTINCT l FROM Lecture l "
                        + "JOIN FETCH l.course c "
                        + "LEFT JOIN FETCH l.schedules "
                        + "WHERE l.year = :year AND l.termSeason = :term "
                        + "AND ("
                        + "  c.credits BETWEEN 1 AND 3 "
                        + "  OR c.name LIKE '%캡스톤%' "
                        + "  OR c.name LIKE '%졸업프로젝트%' "
                        + "  OR c.name LIKE '%졸업작품%'"
                        + ") "
                        + "AND c.name NOT LIKE '%채플%' "
                        + "AND c.name NOT LIKE '%커뮤니티잉글리쉬%' "
                        + "AND c.name NOT LIKE '%커뮤니티 잉글리쉬%' "
                        + "AND c.name NOT LIKE '%대학생활과 진로설계%' "
                        + "AND c.name NOT LIKE '%대학생활과진로설계%' "
                        + "AND c.name NOT LIKE '%교양세미나%' "
                        + "AND c.name NOT LIKE '%기독교의 이해%' "
                        + "AND c.name NOT LIKE '%기독교의이해%' "
                        + "AND (c.category IS NULL OR c.category NOT LIKE '%교직%')")
        List<Lecture> findAllForRecommendation(@Param("year") Integer year, @Param("term") TermSeason term);

        /** 추천 시 시간 충돌 회피용: 사용자가 이미 등록한 강의들의 schedules를 한 번에 로딩 */
        @Query("SELECT DISTINCT l FROM Lecture l LEFT JOIN FETCH l.schedules WHERE l.id IN :ids")
        List<Lecture> findByIdsWithSchedules(@Param("ids") Collection<Long> ids);
}
