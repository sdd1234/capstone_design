-- ============================================================
-- Campus Fit DDL  (MySQL 8.x / MariaDB 10.6+)
-- 실제 구현 코드(Hibernate Entity) 기준으로 작성
-- 마지막 업데이트: 2026-03-09
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ──────────────────────────────────────────
-- 1. 대학교
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS universities (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    domain      VARCHAR(100)          COMMENT '이메일 도메인 (예: kmu.ac.kr)',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 2. 회원
-- * university_id, service_agree, privacy_agree 는 현재 미구현
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(128) NOT NULL,
    name            VARCHAR(20)  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                        COMMENT 'PENDING_VERIFICATION | ACTIVE | REJECTED',
    role            VARCHAR(10)  NOT NULL DEFAULT 'USER'
                        COMMENT 'USER | ADMIN',
    marketing_agree TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 3. 파일
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS files (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    uploaded_by     BIGINT        NOT NULL  COMMENT 'users.id',
    original_name   VARCHAR(255)  NOT NULL,
    stored_path     VARCHAR(255)  NOT NULL,
    mime_type       VARCHAR(100),
    size            BIGINT,
    purpose         VARCHAR(30)   NOT NULL  COMMENT 'STUDENT_VERIFICATION',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_files_uploader FOREIGN KEY (uploaded_by)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 4. 재학생 인증
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_verifications (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    file_id             BIGINT      NOT NULL,
    verification_type   VARCHAR(50) NOT NULL
                            COMMENT 'STUDENT_ID_CARD | ENROLLMENT_CERTIFICATE | PORTAL_SCREENSHOT',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            COMMENT 'PENDING | APPROVED | REJECTED',
    note                VARCHAR(500),
    reject_reason       VARCHAR(500),
    reviewed_by         BIGINT               COMMENT '검토한 관리자 users.id',
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sv_user       FOREIGN KEY (user_id)     REFERENCES users  (id) ON DELETE CASCADE,
    CONSTRAINT fk_sv_file       FOREIGN KEY (file_id)     REFERENCES files  (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sv_reviewer   FOREIGN KEY (reviewed_by) REFERENCES users  (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 5. 과목 (Course = 강의계획서 단위)
-- * department_id 는 plain Long (FK 없음, 향후 departments 테이블 추가 예정)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    university_id   BIGINT,
    dept_id         BIGINT                COMMENT '학과 ID (비FK, 향후 departments 테이블 연동)',
    name            VARCHAR(200) NOT NULL,
    credits         INT,
    category        VARCHAR(50)           COMMENT '전공 | 교양 | 일반선택 등',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_university FOREIGN KEY (university_id)
        REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 6. 선수 과목
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS course_prerequisites (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    course_id               BIGINT NOT NULL,
    prerequisite_course_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cp_course      FOREIGN KEY (course_id)              REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_prerequisite FOREIGN KEY (prerequisite_course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 7. 강의 분반
-- * dept_id 는 plain Long (FK 없음)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lectures (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    course_id       BIGINT      NOT NULL,
    university_id   BIGINT,
    dept_id         BIGINT               COMMENT '학과 ID (비FK)',
    academic_year   INT         NOT NULL,
    term_season     VARCHAR(10) NOT NULL  COMMENT 'SPRING | SUMMER | FALL | WINTER',
    professor       VARCHAR(100),
    room            VARCHAR(100),
    is_remote       TINYINT(1)           DEFAULT 0,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_lectures_course     FOREIGN KEY (course_id)     REFERENCES courses      (id) ON DELETE CASCADE,
    CONSTRAINT fk_lectures_university FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 8. 강의 시간 슬롯
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lecture_schedules (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    lecture_id  BIGINT      NOT NULL,
    day_of_week VARCHAR(5)  NOT NULL  COMMENT 'MON | TUE | WED | THU | FRI | SAT | SUN',
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ls_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 9. 학사 일정
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS academic_calendar_events (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    university_id   BIGINT,
    title           VARCHAR(200) NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    category        VARCHAR(50)           COMMENT 'EXAM | VACATION | ENROLLMENT | HOLIDAY 등',
    academic_year   INT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ace_university FOREIGN KEY (university_id)
        REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 10. 시간표 선호 설정
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS timetable_preferences (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    academic_year   INT         NOT NULL,
    term_season     VARCHAR(10) NOT NULL  COMMENT 'SPRING | SUMMER | FALL | WINTER',
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 선호/기피 시간대 (요일 포함)
CREATE TABLE IF NOT EXISTS preferred_time_ranges (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    preference_id   BIGINT      NOT NULL,
    type            VARCHAR(10) NOT NULL  COMMENT 'PREFERRED | AVOID',
    day_of_week     VARCHAR(5)            COMMENT 'MON | TUE | WED | THU | FRI | SAT | SUN',
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ptr_preference FOREIGN KEY (preference_id)
        REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 희망 수강 과목
CREATE TABLE IF NOT EXISTS desired_courses (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    preference_id   BIGINT NOT NULL,
    course_id       BIGINT          COMMENT '과목 검색으로 선택한 경우',
    raw_text        VARCHAR(200)    COMMENT '자연어 입력',
    priority        INT             DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_dc_preference FOREIGN KEY (preference_id)
        REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 학점 정책
CREATE TABLE IF NOT EXISTS credit_policies (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    preference_id   BIGINT NOT NULL UNIQUE,
    min_credits     INT,
    max_credits     INT,
    target_credits  INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_cp2_preference FOREIGN KEY (preference_id)
        REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 시간표 옵션
CREATE TABLE IF NOT EXISTS preference_options (
    id                  BIGINT    NOT NULL AUTO_INCREMENT,
    preference_id       BIGINT    NOT NULL UNIQUE,
    exclude_morning     TINYINT(1) DEFAULT 0,
    allow_gaps_minutes  INT,
    max_days_per_week   INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_po_preference FOREIGN KEY (preference_id)
        REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 11. AI 시간표 추천
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_timetable_recommendations (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    academic_year       INT         NOT NULL,
    term_season         VARCHAR(10) NOT NULL  COMMENT 'SPRING | SUMMER | FALL | WINTER',
    request_params_json TEXT                  COMMENT '요청 파라미터 스냅샷(JSON)',
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_atr_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 추천 후보 시간표
CREATE TABLE IF NOT EXISTS recommendation_candidates (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    recommendation_id   BIGINT NOT NULL,
    rank                INT    NOT NULL,
    total_credits       INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_rc_recommendation FOREIGN KEY (recommendation_id)
        REFERENCES ai_timetable_recommendations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 추천 후보 강의 (N:M 조인 테이블)
CREATE TABLE IF NOT EXISTS recommendation_candidate_lectures (
    candidate_id    BIGINT NOT NULL,
    lecture_id      BIGINT NOT NULL,
    PRIMARY KEY (candidate_id, lecture_id),
    CONSTRAINT fk_rcl_candidate FOREIGN KEY (candidate_id) REFERENCES recommendation_candidates      (id) ON DELETE CASCADE,
    CONSTRAINT fk_rcl_lecture   FOREIGN KEY (lecture_id)   REFERENCES lectures                       (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 12. 시간표 (확정)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS timetables (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT       NOT NULL,
    source_recommendation_id    BIGINT                COMMENT 'AI 추천 기반인 경우',
    academic_year               INT          NOT NULL,
    term_season                 VARCHAR(10)  NOT NULL,
    title                       VARCHAR(100) NOT NULL,
    status                      VARCHAR(15)  NOT NULL DEFAULT 'DRAFT'
                                    COMMENT 'DRAFT | CONFIRMED',
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
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
-- 13. 개인 일정 (Event)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    category    VARCHAR(20)  NOT NULL
                    COMMENT 'CLASS | ASSIGNMENT | EXAM | PERSONAL | SCHOOL | PROJECT',
    start_at    TIMESTAMP    NOT NULL,
    end_at      TIMESTAMP    NOT NULL,
    all_day     TINYINT(1)            DEFAULT 0,
    description VARCHAR(1000),
    remind_at   TIMESTAMP,
    color       VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────
-- 14. 투두 (Task)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tasks (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    linked_event_id BIGINT                COMMENT '연결된 일정 events.id',
    title           VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                        COMMENT 'TODO | IN_PROGRESS | DONE',
    scheduled_date  DATE,
    due_at          TIMESTAMP,
    category        VARCHAR(50),
    remind_at       TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_user         FOREIGN KEY (user_id)         REFERENCES users  (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_linked_event FOREIGN KEY (linked_event_id) REFERENCES events (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
