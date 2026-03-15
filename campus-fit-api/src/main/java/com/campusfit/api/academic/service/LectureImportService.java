package com.campusfit.api.academic.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface LectureImportService {
    /**
     * 엑셀 파일(MultipartFile)을 파싱해 강의를 DB에 저장합니다.
     */
    int importFromExcel(MultipartFile file, Long universityId, Integer year, String termSeason);

    /**
     * InputStream으로 엑셀을 파싱해 강의를 DB에 저장합니다. (서버 측 자동 임포트용)
     */
    int importFromExcel(InputStream inputStream, String fileName, Long universityId, Integer year, String termSeason);
}
