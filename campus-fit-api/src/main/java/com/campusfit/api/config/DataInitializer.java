package com.campusfit.api.config;

import com.campusfit.api.academic.service.LectureImportService;
import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.enums.UserRole;
import com.campusfit.api.common.enums.UserStatus;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
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

        // 4. classpath:data/ 내장 Excel 임포트 (resources/data/*.xlsx)
        importFromClasspath();

        // 5. 강의 데이터가 없으면 샘플 데이터 생성
        if (lectureRepository.count() == 0) {
            initSampleLectures();
        }

        // 6. 학사 캘린더 데이터 초기화 (2026년)
        initAcademicCalendar();
    }

    /**
     * Excel 파일 없을 때 사용할 샘플 강의 데이터 (계명대학교 2026년 1학기)
     */
    private void initSampleLectures() {
        University univ = universityRepository.findByName("계명대학교").orElse(null);
        if (univ == null)
            return;

        // { 과목명, 학점, 카테고리, 교수명, 강의실, 강의번호, 요일1, 시작1, 종료1, 요일2, 시작2, 종료2 }
        // 요일2가 null이면 단일 요일 강의
        Object[][] data = {
                { "Python 프로그래밍", 3, "전공선택", "김민준", "공학관 301", "CS101", "MON", "09:00", "10:30", "WED", "09:00",
                        "10:30" },
                { "데이터베이스", 3, "전공필수", "이서연", "공학관 302", "CS102", "TUE", "10:30", "12:00", "THU", "10:30", "12:00" },
                { "알고리즘", 3, "전공필수", "박지훈", "공학관 303", "CS103", "MON", "13:30", "15:00", "WED", "13:30", "15:00" },
                { "운영체제", 3, "전공선택", "최수아", "공학관 201", "CS104", "TUE", "13:30", "15:00", "THU", "13:30", "15:00" },
                { "컴퓨터네트워크", 3, "전공선택", "정현우", "공학관 202", "CS105", "MON", "15:00", "16:30", "WED", "15:00", "16:30" },
                { "자료구조", 3, "전공필수", "강예은", "공학관 101", "CS106", "TUE", "09:00", "10:30", "THU", "09:00", "10:30" },
                { "웹프로그래밍", 3, "전공선택", "윤성민", "공학관 401", "CS107", "MON", "10:30", "12:00", "WED", "10:30", "12:00" },
                { "소프트웨어공학", 3, "전공선택", "임지원", "공학관 402", "CS108", "TUE", "15:00", "16:30", "THU", "15:00", "16:30" },
                { "인공지능", 3, "전공선택", "한동현", "공학관 403", "CS109", "WED", "10:30", "12:00", "FRI", "10:30", "12:00" },
                { "컴퓨터구조", 3, "전공필수", "오채린", "공학관 102", "CS110", "MON", "09:00", "10:30", "FRI", "09:00", "10:30" },
                { "모바일앱개발", 3, "전공선택", "서진호", "공학관 501", "CS111", "TUE", "10:30", "12:00", "THU", "10:30", "12:00" },
                { "클라우드컴퓨팅", 3, "전공선택", "권나은", "공학관 502", "CS112", "WED", "13:30", "15:00", "FRI", "13:30", "15:00" },
                { "보안공학", 3, "전공선택", "백승준", "공학관 503", "CS113", "MON", "13:30", "15:00", "THU", "13:30", "15:00" },
                { "딥러닝", 3, "전공선택", "남유진", "공학관 203", "CS114", "TUE", "15:00", "16:30", "FRI", "15:00", "16:30" },
                { "컴파일러", 3, "전공선택", "전민서", "공학관 204", "CS115", "WED", "09:00", "10:30", "FRI", "09:00", "10:30" },
                { "영어회화1", 2, "교양필수", "Smith John", "인문관 101", "GE101", "TUE", "09:00", "10:00", null, null, null },
                { "영어회화2", 2, "교양필수", "Jane Brown", "인문관 102", "GE102", "THU", "09:00", "10:00", null, null, null },
                { "대학수학", 3, "교양필수", "이상호", "이학관 201", "GE103", "MON", "09:00", "10:30", "WED", "09:00", "10:30" },
                { "글쓰기와의사소통", 2, "교양필수", "김혜림", "인문관 201", "GE104", "FRI", "10:30", "12:30", null, null, null },
                { "창의적사고", 2, "교양선택", "박지영", "인문관 301", "GE105", "TUE", "13:30", "15:30", null, null, null },
                { "취창업설계", 2, "교양선택", "조성현", "경상관 101", "GE106", "MON", "16:00", "18:00", null, null, null },
                { "경영학원론", 3, "교양선택", "황민철", "경상관 201", "BA101", "TUE", "10:30", "12:00", "THU", "10:30", "12:00" },
                { "마케팅원론", 3, "전공선택", "류소영", "경상관 202", "BA102", "MON", "13:30", "15:00", "WED", "13:30", "15:00" },
                { "회계원리", 3, "전공선택", "신동우", "경상관 301", "BA103", "TUE", "09:00", "10:30", "THU", "09:00", "10:30" },
                { "통계학", 3, "교양선택", "문하늘", "이학관 101", "ST101", "WED", "15:00", "16:30", "FRI", "15:00", "16:30" },
                { "선형대수학", 3, "전공선택", "안준혁", "이학관 202", "MA101", "MON", "10:30", "12:00", "THU", "10:30", "12:00" },
                { "물리학1", 3, "교양필수", "노수빈", "자연관 101", "PH101", "TUE", "13:30", "15:00", "FRI", "13:30", "15:00" },
                { "캡스톤디자인", 3, "전공필수", "장태양", "공학관 601", "CS200", "WED", "16:00", "19:00", null, null, null },
                { "졸업프로젝트", 2, "전공필수", "이미래", "공학관 602", "CS201", "THU", "16:00", "18:00", null, null, null },
                { "스타트업실습", 2, "교양선택", "홍기훈", "경상관 401", "GE201", "FRI", "13:30", "15:30", null, null, null },
        };

        int count = 0;
        for (Object[] row : data) {
            String courseName = (String) row[0];
            int credits = (int) row[1];
            String category = (String) row[2];
            String professor = (String) row[3];
            String room = (String) row[4];
            String lectureNum = (String) row[5];

            Course course = courseRepository.findByUniversityIdAndName(univ.getId(), courseName)
                    .orElseGet(() -> courseRepository.save(Course.builder()
                            .university(univ)
                            .name(courseName)
                            .credits(credits)
                            .category(category)
                            .build()));

            boolean exists = lectureRepository
                    .findByLectureNumberAndYearAndTermSeason(lectureNum, YEAR, TermSeason.valueOf(TERM)).isPresent();
            if (exists)
                continue;

            Lecture lecture = Lecture.builder()
                    .course(course)
                    .university(univ)
                    .year(YEAR)
                    .termSeason(TermSeason.valueOf(TERM))
                    .professor(professor)
                    .room(room)
                    .lectureNumber(lectureNum)
                    .build();

            String day1 = (String) row[6];
            String start1 = (String) row[7];
            String end1 = (String) row[8];
            String day2 = (String) row[9];
            String start2 = (String) row[10];
            String end2 = (String) row[11];

            if (day1 != null) {
                LectureSchedule s = LectureSchedule.builder()
                        .lecture(lecture)
                        .dayOfWeek(DayOfWeekEnum.valueOf(day1))
                        .startTime(LocalTime.parse(start1))
                        .endTime(LocalTime.parse(end1))
                        .build();
                lecture.getSchedules().add(s);
            }
            if (day2 != null) {
                LectureSchedule s = LectureSchedule.builder()
                        .lecture(lecture)
                        .dayOfWeek(DayOfWeekEnum.valueOf(day2))
                        .startTime(LocalTime.parse(start2))
                        .endTime(LocalTime.parse(end2))
                        .build();
                lecture.getSchedules().add(s);
            }

            lectureRepository.save(lecture);
            count++;
        }
        log.info("✅ 샘플 강의 데이터 생성 완료: {}건 (2026년 1학기)", count);
    }

    /**
     * classpath:data/*.xlsx 에 번들된 엑셀 파일을 임포트합니다.
     * (src/main/resources/data/ 폴더의 파일들)
     */
    private void importFromClasspath() {
        Set<String> imported = importLogRepository.findAllByOrderByImportedAtDesc()
                .stream().map(l -> l.getFileName()).collect(Collectors.toSet());

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/*.xlsx");
            for (Resource res : resources) {
                String fileName = res.getFilename();
                if (imported.contains(fileName)) {
                    log.info("⏭️  이미 임포트됨 — 건너뜀: {}", fileName);
                    continue;
                }
                try (var is = res.getInputStream()) {
                    int count = lectureImportService.importFromExcel(
                            is, fileName, UNIVERSITY_ID, YEAR, TERM);
                    log.info("✅ classpath 강의 임포트 완료: {} → {}건", fileName, count);
                } catch (IOException e) {
                    log.error("❌ classpath 강의 임포트 실패: {} — {}", fileName, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("⚠️  classpath:data/ 스캔 실패: {}", e.getMessage());
        }
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
