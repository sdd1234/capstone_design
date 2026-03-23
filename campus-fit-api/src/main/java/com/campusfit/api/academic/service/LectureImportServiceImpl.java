package com.campusfit.api.academic.service;

import com.campusfit.api.common.enums.DayOfWeekEnum;
import com.campusfit.api.common.enums.TermSeason;
import com.campusfit.api.common.exception.BusinessException;
import com.campusfit.api.domain.*;
import com.campusfit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
@Transactional
public class LectureImportServiceImpl implements LectureImportService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final UniversityRepository universityRepository;
    private final LectureImportLogRepository importLogRepository;

    // 강의시간 파싱 패턴: "월10:00~11:45(N101)" 또는 "월10:00~11:45 수10:00~11:45"
    private static final Pattern SCHEDULE_PATTERN = Pattern
            .compile("([월화수목금토일])(\\d{1,2}:\\d{2})~(\\d{1,2}:\\d{2})(?:\\(([^)]*)\\))?");

    private static final Map<String, DayOfWeekEnum> DAY_MAP = Map.of(
            "월", DayOfWeekEnum.MON,
            "화", DayOfWeekEnum.TUE,
            "수", DayOfWeekEnum.WED,
            "목", DayOfWeekEnum.THU,
            "금", DayOfWeekEnum.FRI,
            "토", DayOfWeekEnum.SAT,
            "일", DayOfWeekEnum.SUN);

    @Override
    public int importFromExcel(MultipartFile file, Long universityId, Integer year, String termSeason) {
        try {
            return importFromExcel(file.getInputStream(),
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.xlsx",
                    universityId, year, termSeason);
        } catch (IOException e) {
            throw BusinessException.badRequest("엑셀 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public int importFromExcel(InputStream inputStream, String fileName, Long universityId, Integer year,
            String termSeason) {
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> BusinessException.notFound("대학교를 찾을 수 없습니다."));
        TermSeason ts = TermSeason.valueOf(termSeason);

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;

            // 1행은 헤더이므로 2행부터 시작 (index 1)
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null)
                    continue;

                String dept = getCellText(row, 1); // Col 2: 학과(학부)명
                String lectureNumber = getCellText(row, 2); // Col 3: 강좌번호
                String courseName = getCellText(row, 3); // Col 4: 교과목명
                String creditsStr = getCellText(row, 4); // Col 5: 학점
                String targetGradeStr = getCellText(row, 5); // Col 6: 대상학년
                String category = getCellText(row, 7); // Col 8: 이수구분
                String professor = getCellText(row, 9); // Col 10: 담당교수
                String scheduleRaw = getCellText(row, 13); // Col 14: 강의시간(강의실)
                String area = getCellText(row, 15); // Col 16: 영역
                String campus = getCellText(row, 16); // Col 17: 캠퍼스

                if (courseName.isBlank() || lectureNumber.isBlank())
                    continue;

                Integer credits = parseCredits(creditsStr);
                Integer targetGrade = parseGrade(targetGradeStr);

                // Course 조회 또는 생성 (같은 대학 + 같은 이름)
                Course course = courseRepository
                        .findByUniversityIdAndName(universityId, courseName)
                        .orElseGet(() -> courseRepository.save(
                                Course.builder()
                                        .university(university)
                                        .name(courseName)
                                        .credits(credits)
                                        .category(category)
                                        .build()));

                // 동일 강좌번호+연도+학기가 이미 있으면 건너뜀
                boolean exists = lectureRepository
                        .findByLectureNumberAndYearAndTermSeason(lectureNumber, year, ts)
                        .isPresent();
                if (exists)
                    continue;

                // 강의시간 파싱
                List<LectureSchedule> schedules = new ArrayList<>();
                String roomParsed = parseSchedules(scheduleRaw, schedules);

                Lecture lecture = Lecture.builder()
                        .course(course)
                        .university(university)
                        .year(year)
                        .termSeason(ts)
                        .professor(professor)
                        .dept(dept.isBlank() ? null : dept)
                        .targetGrade(targetGrade)
                        .room(roomParsed)
                        .lectureNumber(lectureNumber)
                        .area(area)
                        .campus(campus)
                        .build();

                schedules.forEach(s -> s.setLecture(lecture));
                lecture.getSchedules().addAll(schedules);
                lectureRepository.save(lecture);
                count++;
            }

            // 업로드 내역 저장
            importLogRepository.save(LectureImportLog.builder()
                    .university(university)
                    .year(year)
                    .termSeason(ts)
                    .fileName(fileName)
                    .importedCount(count)
                    .build());

            return count;
        } catch (IOException e) {
            throw BusinessException.badRequest("엑셀 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 강의시간 문자열을 파싱해 LectureSchedule 목록을 채우고, 첫 번째 강의실명을 반환합니다.
     * 예: "월10:00~11:45(N101) 수10:00~11:45(N101)"
     */
    private String parseSchedules(String raw, List<LectureSchedule> schedules) {
        if (raw == null || raw.isBlank())
            return null;
        Matcher m = SCHEDULE_PATTERN.matcher(raw);
        String firstRoom = null;
        while (m.find()) {
            String dayKor = m.group(1);
            String startStr = m.group(2);
            String endStr = m.group(3);
            String room = m.group(4);

            DayOfWeekEnum day = DAY_MAP.get(dayKor);
            if (day == null)
                continue;

            if (firstRoom == null && room != null)
                firstRoom = room;

            schedules.add(LectureSchedule.builder()
                    .dayOfWeek(day)
                    .startTime(LocalTime.parse(padTime(startStr)))
                    .endTime(LocalTime.parse(padTime(endStr)))
                    .build());
        }
        return firstRoom;
    }

    /** "9:00" → "09:00" 변환 */
    private String padTime(String t) {
        return t.length() == 4 ? "0" + t : t;
    }

    private Integer parseCredits(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** "1학년", "2", "1~2", "1,2" 등에서 첫 번째 숫자(1~4)만 추출 */
    private Integer parseGrade(String s) {
        if (s == null || s.isBlank())
            return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[1-4]").matcher(s.trim());
        if (m.find()) {
            int g = Integer.parseInt(m.group());
            return (g >= 1 && g <= 4) ? g : null;
        }
        return null;
    }

    private String getCellText(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
            }
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((int) cell.getNumericCellValue())
                    : cell.getStringCellValue().trim();
            default -> "";
        };
    }
}
