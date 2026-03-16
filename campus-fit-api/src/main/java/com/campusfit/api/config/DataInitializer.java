package com.campusfit.api.config;

import com.campusfit.api.academic.service.LectureImportService;
import com.campusfit.api.common.enums.UserRole;
import com.campusfit.api.common.enums.UserStatus;
import com.campusfit.api.domain.AcademicCalendarEvent;
import com.campusfit.api.domain.University;
import com.campusfit.api.domain.User;
import com.campusfit.api.repository.AcademicCalendarEventRepository;
import com.campusfit.api.repository.LectureImportLogRepository;
import com.campusfit.api.repository.UniversityRepository;
import com.campusfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final LectureImportLogRepository importLogRepository;
    private final LectureImportService lectureImportService;
    private final AcademicCalendarEventRepository calendarEventRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.excel.import-dir:../excel}")
    private String excelImportDir;

    private static final Long UNIVERSITY_ID = 1L;
    private static final int YEAR = 2026;
    private static final String TERM = "SPRING";

    @Override
    public void run(ApplicationArguments args) {
        // 1. 관리자 계정 초기화
        if (!userRepository.existsByEmail("admin@campusfit.com")) {
            User admin = User.builder()
                    .email("admin@campusfit.com")
                    .passwordHash(passwordEncoder.encode("admin1234!"))
                    .name("관리자")
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("✅ 관리자 계정 생성 완료: admin@campusfit.com / admin1234!");
        }

        // 2. 기본 대학교 초기화
        University univ;
        if (universityRepository.findByName("계명대학교").isEmpty()) {
            univ = universityRepository.save(University.builder()
                    .name("계명대학교")
                    .domain("kmu.ac.kr")
                    .build());
            log.info("✅ 대학교 등록 완료: 계명대학교 (id={})", univ.getId());
        }

        // 3. excel/ 폴더의 .xlsx 파일 자동 스캔 임포트
        importFromExcelDir();

        // 4. 학사 캘린더 데이터 초기화 (2026년)
        initAcademicCalendar();
    }

    /**
     * app.excel.import-dir 경로의 .xlsx 파일을 전부 스캔하여,
     * 아직 임포트되지 않은 파일만 DB에 적재합니다.
     */
    private void importFromExcelDir() {
        Path dir = Paths.get(excelImportDir);
        if (!Files.isDirectory(dir)) {
            log.warn("⚠️  Excel 폴더 없음 — 건너뜀: {}", dir.toAbsolutePath());
            return;
        }

        // 이미 임포트된 파일명 집합
        Set<String> imported = importLogRepository.findAllByOrderByImportedAtDesc()
                .stream().map(l -> l.getFileName()).collect(Collectors.toSet());

        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".xlsx"))
                    .sorted()
                    .forEach(file -> {
                        String fileName = file.getFileName().toString();
                        if (imported.contains(fileName)) {
                            log.info("⏭️  이미 임포트됨 — 건너뜀: {}", fileName);
                            return;
                        }
                        try (var is = Files.newInputStream(file)) {
                            int count = lectureImportService.importFromExcel(
                                    is, fileName, UNIVERSITY_ID, YEAR, TERM);
                            log.info("✅ 강의 자동 임포트 완료: {} → {}건", fileName, count);
                        } catch (IOException e) {
                            log.error("❌ 강의 임포트 실패: {} — {}", fileName, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("❌ Excel 폴더 스캔 실패: {}", e.getMessage());
        }
    }

    private void initAcademicCalendar() {
        // 이미 2026년 데이터가 있으면 건너뜀
        if (!calendarEventRepository.findByYear(2026).isEmpty()) {
            return;
        }

        // 계명대학교 (universityRepository에서 조회)
        University univ = universityRepository.findByName("계명대학교").orElse(null);

        Object[][] events = {
                { "수강신청 (1차)", "REGISTRATION", "2026-02-02", "2026-02-06" },
                { "수강신청 (2차)", "REGISTRATION", "2026-02-16", "2026-02-20" },
                { "1학기 개강", "SEMESTER", "2026-03-02", "2026-03-02" },
                { "수강변경 기간", "REGISTRATION", "2026-03-02", "2026-03-06" },
                { "중간고사", "EXAM", "2026-04-20", "2026-04-26" },
                { "수강취소 기간", "REGISTRATION", "2026-05-04", "2026-05-08" },
                { "기말고사", "EXAM", "2026-06-15", "2026-06-21" },
                { "1학기 종강", "SEMESTER", "2026-06-22", "2026-06-22" },
                { "여름학기 개강", "SEMESTER", "2026-07-01", "2026-07-01" },
                { "여름학기 종강", "SEMESTER", "2026-07-24", "2026-07-24" },
                { "2학기 수강신청 (1차)", "REGISTRATION", "2026-07-27", "2026-07-31" },
                { "2학기 개강", "SEMESTER", "2026-08-31", "2026-08-31" },
                { "추석 연휴", "HOLIDAY", "2026-09-24", "2026-09-27" },
                { "중간고사", "EXAM", "2026-10-19", "2026-10-25" },
                { "기말고사", "EXAM", "2026-12-14", "2026-12-20" },
                { "2학기 종강", "SEMESTER", "2026-12-21", "2026-12-21" },
                { "겨울학기 개강", "SEMESTER", "2026-12-28", "2026-12-28" },
        };

        for (Object[] e : events) {
            calendarEventRepository.save(AcademicCalendarEvent.builder()
                    .university(univ)
                    .title((String) e[0])
                    .category((String) e[1])
                    .startDate(LocalDate.parse((String) e[2]))
                    .endDate(LocalDate.parse((String) e[3]))
                    .year(2026)
                    .build());
        }
        log.info("✅ 학사 캘린더 초기화 완료: {}건", events.length);
    }
}
