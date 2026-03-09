-- ============================================================
-- Campus Fit DDL  (MySQL 8.x / MariaDB 10.6+)
-- 실행 순서: 위에서 아래로 순서대로 실행
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ──────────────────────────────────────────
-- 1. 대학교
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS universities (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    domain      VARCHAR(200)          COMMENT '이메일 도메인 (예: kmu.ac.kr)',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_universities_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 2. 회원
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    university_id   BIGINT                COMMENT '소속 대학교',
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(20)  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                        COMMENT 'PENDING_VERIFICATION | ACTIVE | REJECTED',
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER'
                        COMMENT 'USER | ADMIN',
    service_agree   TINYINT(1)   NOT NULL,
    privacy_agree   TINYINT(1)   NOT NULL,
    marketing_agree TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT fk_users_university FOREIGN KEY (university_id)
        REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 3. 파일
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS files (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    uploader_id     BIGINT        NOT NULL,
    original_name   VARCHAR(255)  NOT NULL,
    stored_path     VARCHAR(500)  NOT NULL,
    mime_type       VARCHAR(100),
    size_bytes      BIGINT,
    purpose         VARCHAR(30)   NOT NULL COMMENT 'STUDENT_VERIFICATION',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_files_uploader FOREIGN KEY (uploader_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 4. 재학생 인증
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_verifications (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    file_id             BIGINT      NOT NULL,
    verification_type   VARCHAR(40) NOT NULL
                            COMMENT 'STUDENT_ID_CARD | ENROLLMENT_CERTIFICATE | PORTAL_SCREENSHOT',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            COMMENT 'PENDING | APPROVED | REJECTED',
    note                TEXT,
    reject_reason       TEXT,
    reviewed_at         TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_sv_user (user_id),
    CONSTRAINT fk_sv_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_sv_file FOREIGN KEY (file_id)
        REFERENCES files (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 5. 학과
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    university_id   BIGINT       NOT NULL,
    name            VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_departments_university FOREIGN KEY (university_id)
        REFERENCES universities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 6. 과목 (Course = 강의계획서 단위)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    university_id   BIGINT       NOT NULL,
    department_id   BIGINT,
    name            VARCHAR(200) NOT NULL,
    default_credits INT          NOT NULL DEFAULT 3,
    category        VARCHAR(30)           COMMENT '전공 | 교양 | 일반선택 등',
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_university   FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE CASCADE,
    CONSTRAINT fk_courses_department   FOREIGN KEY (department_id) REFERENCES departments  (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 7. 선수 과목
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS course_prerequisites (
    course_id           BIGINT NOT NULL,
    prerequisite_id     BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_cp_course      FOREIGN KEY (course_id)       REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_prerequisite FOREIGN KEY (prerequisite_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 8. 강의 분반
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lectures (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    course_id       BIGINT      NOT NULL,
    university_id   BIGINT      NOT NULL,
    department_id   BIGINT,
    year            INT         NOT NULL,
    term_season     VARCHAR(10) NOT NULL COMMENT 'SPRING | SUMMER | FALL | WINTER',
    professor       VARCHAR(100),
    credits         INT         NOT NULL DEFAULT 3,
    category        VARCHAR(30),
    is_remote       TINYINT(1)  NOT NULL DEFAULT 0,
    capacity        INT,
    enrolled        INT                  DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_lectures_course      FOREIGN KEY (course_id)     REFERENCES courses     (id) ON DELETE CASCADE,
    CONSTRAINT fk_lectures_university  FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE,
    CONSTRAINT fk_lectures_department  FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 9. 강의 시간 슬롯
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lecture_schedules (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    lecture_id  BIGINT      NOT NULL,
    day_of_week VARCHAR(5)  NOT NULL COMMENT 'MON | TUE | WED | THU | FRI | SAT | SUN',
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    room        VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT fk_ls_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 10. 학사 일정 (크롤링)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS academic_calendar_events (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    university_id   BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE,
    category        VARCHAR(30)           COMMENT 'EXAM | VACATION | ENROLLMENT | HOLIDAY 등',
    year            INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ace_university FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 11. 시간표 선호 설정
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS timetable_preferences (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT      NOT NULL,
    university_id           BIGINT      NOT NULL,
    year                    INT         NOT NULL,
    term_season             VARCHAR(10) NOT NULL,
    min_credits             INT,
    max_credits             INT,
    target_credits          INT,
    exclude_morning_classes TINYINT(1)           DEFAULT 0,
    allow_gaps_minutes      INT,
    max_days_per_week       INT,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tp_user_term (user_id, university_id, year, term_season),
    CONSTRAINT fk_tp_user       FOREIGN KEY (user_id)       REFERENCES users        (id) ON DELETE CASCADE,
    CONSTRAINT fk_tp_university FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS timetable_preference_days (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    preference_id   BIGINT      NOT NULL,
    day_of_week     VARCHAR(5)  NOT NULL,
    day_type        VARCHAR(10) NOT NULL COMMENT 'PREFERRED | AVOID',
    PRIMARY KEY (id),
    CONSTRAINT fk_tpd_preference FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS timetable_preference_time_ranges (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    preference_id   BIGINT      NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    range_type      VARCHAR(10) NOT NULL COMMENT 'PREFERRED | AVOID',
    PRIMARY KEY (id),
    CONSTRAINT fk_tptr_preference FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS desired_courses (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    preference_id   BIGINT NOT NULL,
    course_id       BIGINT          COMMENT '과목 검색으로 선택한 경우',
    raw_text        TEXT            COMMENT '자연어 입력 NLP용',
    priority        INT             COMMENT '1~5',
    PRIMARY KEY (id),
    CONSTRAINT fk_dc_preference FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE,
    CONSTRAINT fk_dc_course     FOREIGN KEY (course_id)     REFERENCES courses               (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 12. AI 추천
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_recommendations (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    year             INT         NOT NULL,
    term_season      VARCHAR(10) NOT NULL,
    major_id         BIGINT,
    grade            INT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         COMMENT 'PENDING | DONE | FAILED',
    request_snapshot JSON                 COMMENT '요청 파라미터 스냅샷',
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_air_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_recommendation_candidates (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    recommendation_id   BIGINT NOT NULL,
    rank                INT    NOT NULL,
    total_credits       INT,
    score               FLOAT,
    PRIMARY KEY (id),
    CONSTRAINT fk_airc_rec FOREIGN KEY (recommendation_id)
        REFERENCES ai_recommendations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_recommendation_candidate_items (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    candidate_id    BIGINT NOT NULL,
    lecture_id      BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_airci_candidate FOREIGN KEY (candidate_id) REFERENCES ai_recommendation_candidates (id) ON DELETE CASCADE,
    CONSTRAINT fk_airci_lecture   FOREIGN KEY (lecture_id)   REFERENCES lectures                      (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 13. 시간표 (확정)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS timetables (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT       NOT NULL,
    source_recommendation_id    BIGINT                COMMENT 'AI 추천 기반인 경우',
    year                        INT          NOT NULL,
    term_season                 VARCHAR(10)  NOT NULL,
    title                       VARCHAR(100),
    status                      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                                    COMMENT 'DRAFT | CONFIRMED',
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tt_user    FOREIGN KEY (user_id)                  REFERENCES users             (id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_src_rec FOREIGN KEY (source_recommendation_id) REFERENCES ai_recommendations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS timetable_items (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    timetable_id    BIGINT NOT NULL,
    lecture_id      BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ti_timetable_lecture (timetable_id, lecture_id),
    CONSTRAINT fk_ti_timetable FOREIGN KEY (timetable_id) REFERENCES timetables (id) ON DELETE CASCADE,
    CONSTRAINT fk_ti_lecture   FOREIGN KEY (lecture_id)   REFERENCES lectures   (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 14. 개인 일정 (Event)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    category    VARCHAR(30)  NOT NULL
                    COMMENT 'CLASS | ASSIGNMENT | EXAM | PERSONAL | SCHOOL | PROJECT',
    start_at    TIMESTAMP    NOT NULL,
    end_at      TIMESTAMP    NOT NULL,
    all_day     TINYINT(1)   NOT NULL DEFAULT 0,
    description TEXT,
    remind_at   TIMESTAMP,
    color       VARCHAR(7)            COMMENT '#RRGGBB',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 15. 투두 (Task)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tasks (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    linked_event_id BIGINT                COMMENT '연결된 일정',
    title           VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                        COMMENT 'TODO | IN_PROGRESS | DONE',
    scheduled_date  DATE,
    due_at          TIMESTAMP,
    category        VARCHAR(30),
    remind_at       TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_user         FOREIGN KEY (user_id)         REFERENCES users  (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_linked_event FOREIGN KEY (linked_event_id) REFERENCES events (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
