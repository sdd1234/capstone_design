package com.campusfit.api.academic.controller;

import com.campusfit.api.academic.dto.LectureImportLogResponse;
import com.campusfit.api.academic.dto.LectureResponse;
import com.campusfit.api.academic.dto.PrerequisiteResponse;
import com.campusfit.api.academic.service.LectureImportService;
import com.campusfit.api.academic.service.LectureService;
import com.campusfit.api.common.dto.ApiResponse;
import com.campusfit.api.repository.LectureImportLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Tag(name = "강의 관리", description = "강의 조회 및 엑셀 임포트 API")
@RestController
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;
    private final LectureImportService lectureImportService;
    private final LectureImportLogRepository importLogRepository;

    @Operation(summary = "강의 목록 검색", description = "대학교·연도·학기로 강의를 검색합니다. keyword로 교과목명/교수명, category로 이수구분, area로 영역 필터링 가능.")
    @GetMapping("/api/v1/lectures")
    public ResponseEntity<ApiResponse<List<LectureResponse>>> search(
            @Parameter(description = "대학교 ID (계명대학교 = 1)", example = "1") @RequestParam Long universityId,
            @Parameter(description = "연도", example = "2026") @RequestParam Integer year,
            @Parameter(description = "학기 (SPRING / FALL / SUMMER / WINTER)", example = "SPRING") @RequestParam String termSeason,
            @Parameter(description = "교과목명 또는 교수명 키워드 (선택)") @RequestParam(required = false) String keyword,
            @Parameter(description = "이수구분 필터 (예: 전공필수, 교양선택)") @RequestParam(required = false) String category,
            @Parameter(description = "영역 필터 (예: 인문사회, 자연과학)") @RequestParam(required = false) String area) {
        return ResponseEntity
                .ok(ApiResponse.ok(lectureService.search(universityId, year, termSeason, keyword, category, area)));
    }

    @Operation(summary = "강의 상세 조회", description = "강의 ID로 단건 조회합니다.")
    @GetMapping("/api/v1/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureResponse>> getById(
            @Parameter(description = "강의 ID") @PathVariable Long lectureId) {
        return ResponseEntity.ok(ApiResponse.ok(lectureService.getById(lectureId)));
    }

    @Operation(summary = "선수과목 조회", description = "특정 과목의 선수과목 목록을 반환합니다.")
    @GetMapping("/api/v1/courses/{courseId}/prerequisites")
    public ResponseEntity<ApiResponse<List<PrerequisiteResponse>>> getPrerequisites(
            @Parameter(description = "과목 ID") @PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(lectureService.getPrerequisites(courseId)));
    }

    @Operation(summary = "엑셀 파일로 강의 일괄 등록 [ADMIN]", description = """
            교양/전공 개설강좌 엑셀 파일(.xlsx)을 업로드하면 강의를 DB에 일괄 등록합니다.

            **엑셀 컬럼 구조 (A~S열)**:
            A=순번, B=개설학과, C=강좌번호, D=교과목명, E=학점, F=언어, G=성적,
            H=이수구분, I=구분, J=담당교수, K=수강대상, L=수강정원, M=수업운영형태,
            N=강의시간(강의실), O=수강여석, P=영역, Q=캠퍼스

            **기본값 (계명대학교)**:
            - universityId: 1
            - year: 2026
            - termSeason: SPRING
            """, security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping(value = "/api/v1/admin/lectures/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importLectures(
            @Parameter(description = "엑셀 파일 (.xlsx)", required = true, schema = @Schema(type = "string", format = "binary")) @RequestParam("file") MultipartFile file,
            @Parameter(description = "대학교 ID (계명대학교 = 1)", example = "1") @RequestParam Long universityId,
            @Parameter(description = "연도", example = "2026") @RequestParam Integer year,
            @Parameter(description = "학기 (SPRING / FALL / SUMMER / WINTER)", example = "SPRING") @RequestParam String termSeason) {
        int count = lectureImportService.importFromExcel(file, universityId, year, termSeason);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("imported", count)));
    }

    @Operation(summary = "엑셀 업로드 내역 조회 [ADMIN]", description = "강의 엑셀 임포트 이력을 최신 순으로 반환합니다.", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/api/v1/admin/lectures/import/history")
    public ResponseEntity<ApiResponse<List<LectureImportLogResponse>>> importHistory() {
        List<LectureImportLogResponse> logs = importLogRepository.findAllByOrderByImportedAtDesc()
                .stream().map(LectureImportLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
