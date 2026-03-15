package com.campusfit.api.repository;

import com.campusfit.api.domain.LectureImportLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LectureImportLogRepository extends JpaRepository<LectureImportLog, Long> {
    @EntityGraph(attributePaths = "university")
    List<LectureImportLog> findAllByOrderByImportedAtDesc();
}
