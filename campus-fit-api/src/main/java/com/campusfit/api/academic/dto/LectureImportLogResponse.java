package com.campusfit.api.academic.dto;

import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.domain.LectureImportLog;
import java.time.LocalDateTime;

public record LectureImportLogResponse(
        Long id,
        String universityName,
        Integer year,
        TermSeason termSeason,
        String fileName,
        Integer importedCount,
        LocalDateTime importedAt) {

    public static LectureImportLogResponse from(LectureImportLog log) {
        return new LectureImportLogResponse(
                log.getId(),
                log.getUniversity() != null ? log.getUniversity().getName() : null,
                log.getYear(),
                log.getTermSeason(),
                log.getFileName(),
                log.getImportedCount(),
                log.getImportedAt());
    }
}
