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
        // 이미 충분한 2026년 데이터가 있으면 건너뜀
        if (calendarEventRepository.findByYear(2026).size() == 56) {
            return;
        }

        // 기존 2026년 데이터 삭제 후 재등록
        calendarEventRepository.deleteAll(calendarEventRepository.findByYear(2026));

        University univ = universityRepository.findByName("계명대학교").orElse(null);

        Object[][] events = {
                // ─── 1학기 ───────────────────────────────────────────
                { "1학기 개시일", "SEMESTER", "2026-03-01", "2026-03-01" },
                { "대체공휴일(3·1절)", "HOLIDAY", "2026-03-02", "2026-03-02" },
                { "1학기 개강", "SEMESTER", "2026-03-03", "2026-03-03" },
                { "수강정정", "REGISTRATION", "2026-03-03", "2026-03-05" },
                { "1학기 수업일수 ¼선", "SEMESTER", "2026-03-30", "2026-03-30" },
                { "고난 주간", "SCHOOL", "2026-03-30", "2026-04-04" },
                { "부활절", "HOLIDAY", "2026-04-05", "2026-04-05" },
                { "1학기 수업일수 ⅓선", "SEMESTER", "2026-04-06", "2026-04-06" },
                { "부활절 예배", "SCHOOL", "2026-04-09", "2026-04-09" },
                { "1학기 수업일수 ½선", "SEMESTER", "2026-04-24", "2026-04-24" },
                { "근로자의날 (휴업일)", "HOLIDAY", "2026-05-01", "2026-05-01" },
                { "어린이날 (공휴일)", "HOLIDAY", "2026-05-05", "2026-05-05" },
                { "1학기 수업일수 ⅔선", "SEMESTER", "2026-05-11", "2026-05-11" },
                { "창립기념일 (휴업일)", "HOLIDAY", "2026-05-20", "2026-05-20" },
                { "대체공휴일(부처님오신날)", "HOLIDAY", "2026-05-25", "2026-05-25" },
                { "2026 지방선거", "HOLIDAY", "2026-06-03", "2026-06-03" },
                { "보강일 (근로자의날)", "SCHOOL", "2026-06-09", "2026-06-09" },
                { "보강일 (어린이날)", "SCHOOL", "2026-06-10", "2026-06-10" },
                { "보강일 (창립기념일)", "SCHOOL", "2026-06-11", "2026-06-11" },
                { "보강일 (부처님오신날)", "SCHOOL", "2026-06-12", "2026-06-12" },
                { "보강일 (지방선거)", "SCHOOL", "2026-06-15", "2026-06-15" },
                { "1학기 정기시험", "EXAM", "2026-06-16", "2026-06-22" },
                { "하계방학 및 계절학기 시작", "SEMESTER", "2026-06-23", "2026-06-23" },
                // ─── 여름 ─────────────────────────────────────────────
                { "2학기 재입학 신청(1차)", "REGISTRATION", "2026-07-01", "2026-07-07" },
                { "2학기 복학 신청(1차)", "REGISTRATION", "2026-07-01", "2026-07-15" },
                // ─── 2학기 ───────────────────────────────────────────
                { "대체공휴일(광복절)", "HOLIDAY", "2026-08-17", "2026-08-17" },
                { "후기 학부 학위수여일", "SCHOOL", "2026-08-20", "2026-08-20" },
                { "후기 대학원 학위수여일", "SCHOOL", "2026-08-20", "2026-08-20" },
                { "2학기 등록금 수납", "REGISTRATION", "2026-08-24", "2026-08-27" },
                { "2학기 개강 예배", "SCHOOL", "2026-08-26", "2026-08-26" },
                { "2학기 개시일", "SEMESTER", "2026-09-01", "2026-09-01" },
                { "추석 연휴", "HOLIDAY", "2026-09-24", "2026-09-26" },
                { "2학기 수업일수 ¼선", "SEMESTER", "2026-09-28", "2026-09-28" },
                { "대체공휴일(개천절)", "HOLIDAY", "2026-10-05", "2026-10-05" },
                { "2학기 수업일수 ⅓선", "SEMESTER", "2026-10-06", "2026-10-06" },
                { "한글날 (공휴일)", "HOLIDAY", "2026-10-09", "2026-10-09" },
                { "2학기 수업일수 ½선", "SEMESTER", "2026-10-23", "2026-10-23" },
                { "2학기 수업일수 ⅔선", "SEMESTER", "2026-11-09", "2026-11-09" },
                { "추수감사 예배", "SCHOOL", "2026-11-19", "2026-11-19" },
                { "보강일 (추석 9/24)", "SCHOOL", "2026-12-08", "2026-12-08" },
                { "보강일 (추석 9/25)", "SCHOOL", "2026-12-09", "2026-12-09" },
                { "보강일 (개천절)", "SCHOOL", "2026-12-10", "2026-12-10" },
                { "보강일 (한글날)", "SCHOOL", "2026-12-11", "2026-12-11" },
                { "2학기 정기시험", "EXAM", "2026-12-14", "2026-12-18" },
                { "동계방학 및 계절학기 시작", "SEMESTER", "2026-12-21", "2026-12-21" },
                { "성탄절 (공휴일)", "HOLIDAY", "2026-12-25", "2026-12-25" },
                // ─── 2027년 (2026 학년도 계속) ────────────────────────
                { "신정", "HOLIDAY", "2027-01-01", "2027-01-01" },
                { "1학기 재입학 신청(1차)", "REGISTRATION", "2027-01-04", "2027-01-08" },
                { "1학기 복학 신청(1차)", "REGISTRATION", "2027-01-04", "2027-01-15" },
                { "전기 학부 학위수여식", "SCHOOL", "2027-02-18", "2027-02-18" },
                { "전기 대학원 학위수여식", "SCHOOL", "2027-02-19", "2027-02-19" },
                { "1학기 등록금 수납", "REGISTRATION", "2027-02-22", "2027-02-25" },
                { "전체 교수회", "SCHOOL", "2027-02-23", "2027-02-23" },
                { "1학기 개강 예배", "SCHOOL", "2027-02-24", "2027-02-24" },
                { "입학식", "SCHOOL", "2027-02-26", "2027-02-26" },
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
