-- ============================================================
--  Campus Fit  DDL  (MySQL 8.x / MariaDB 10.x)
--  H2 개발 환경에서는 JPA ddl-auto: create-drop 으로 자동 생성됨
--  MySQL 전환 시 이 스크립트를 사용
-- ============================================================

CREATE TABLE IF NOT EXISTS lecture_import_logs (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    university_id  BIGINT,
    year           INT          NOT NULL,
    term_season    VARCHAR(10)  NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    imported_count INT          NOT NULL DEFAULT 0,
    imported_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    email          VARCHAR(254) NOT NULL,
    password_hash  VARCHAR(128) NOT NULL,
    name           VARCHAR(20)  NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    role           VARCHAR(10)  NOT NULL DEFAULT 'USER',
    marketing_agree TINYINT(1)  NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS files (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    original_name  VARCHAR(255) NOT NULL,
    stored_path    VARCHAR(500) NOT NULL,
    mime_type      VARCHAR(100),
    size           BIGINT,
    purpose        VARCHAR(30)  NOT NULL,
    uploaded_by    BIGINT,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_files_user FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_verifications (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    file_id           BIGINT       NOT NULL,
    verification_type VARCHAR(50)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reject_reason     VARCHAR(500),
    note              VARCHAR(500),
    reviewed_by       BIGINT,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sv_user     FOREIGN KEY (user_id)      REFERENCES users (id),
    CONSTRAINT fk_sv_file     FOREIGN KEY (file_id)      REFERENCES files (id),
    CONSTRAINT fk_sv_reviewer FOREIGN KEY (reviewed_by)  REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS universities (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    domain     VARCHAR(100),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS timetable_preferences (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    user_id     BIGINT  NOT NULL,
    year        INT     NOT NULL,
    term_season VARCHAR(10) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pref (user_id, year, term_season),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS preferred_time_ranges (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    preference_id BIGINT      NOT NULL,
    type          VARCHAR(10) NOT NULL,
    day_of_week   VARCHAR(5),
    start_time    TIME        NOT NULL,
    end_time      TIME        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ptr_pref FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS desired_courses (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    preference_id BIGINT       NOT NULL,
    course_id     BIGINT,
    raw_text      VARCHAR(200),
    priority      INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_dc_pref FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS credit_policies (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    preference_id BIGINT NOT NULL,
    min_credits   INT,
    max_credits   INT,
    target_credits INT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cp_pref (preference_id),
    CONSTRAINT fk_cp_pref FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS preference_options (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    preference_id     BIGINT      NOT NULL,
    exclude_morning   TINYINT(1)  NOT NULL DEFAULT 0,
    allow_gaps_minutes INT,
    max_days_per_week  INT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_po_pref (preference_id),
    CONSTRAINT fk_po_pref FOREIGN KEY (preference_id) REFERENCES timetable_preferences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS academic_calendar_events (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    university_id  BIGINT,
    title          VARCHAR(200) NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    category       VARCHAR(50),
    year           INT,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ace_uni FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS courses (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    university_id BIGINT,
    name          VARCHAR(200) NOT NULL,
    credits       INT,
    category      VARCHAR(50),
    dept_id       BIGINT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_uni FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_prerequisites (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    course_id             BIGINT NOT NULL,
    prerequisite_course_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_prereq FOREIGN KEY (prerequisite_course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lectures (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    course_id     BIGINT      NOT NULL,
    university_id BIGINT,
    year          INT         NOT NULL,
    term_season   VARCHAR(10) NOT NULL,
    professor      VARCHAR(100),
    room           VARCHAR(100),
    is_remote      TINYINT(1)  NOT NULL DEFAULT 0,
    dept_id        BIGINT,
    lecture_number VARCHAR(30),
    area           VARCHAR(100),
    campus         VARCHAR(50),
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_lec_course FOREIGN KEY (course_id)     REFERENCES courses (id),
    CONSTRAINT fk_lec_uni    FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lecture_schedules (
    id          BIGINT     NOT NULL AUTO_INCREMENT,
    lecture_id  BIGINT     NOT NULL,
    day_of_week VARCHAR(5) NOT NULL,
    start_time  TIME       NOT NULL,
    end_time    TIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ls_lec FOREIGN KEY (lecture_id) REFERENCES lectures (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_timetable_recommendations (
    id                  BIGINT     NOT NULL AUTO_INCREMENT,
    user_id             BIGINT     NOT NULL,
    year                INT        NOT NULL,
    term_season         VARCHAR(10) NOT NULL,
    request_params_json TEXT,
    created_at          DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_atr_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recommendation_candidates (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    recommendation_id BIGINT NOT NULL,
    rank              INT    NOT NULL,
    total_credits     INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_rc_rec FOREIGN KEY (recommendation_id) REFERENCES ai_timetable_recommendations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recommendation_candidate_lectures (
    candidate_id BIGINT NOT NULL,
    lecture_id   BIGINT NOT NULL,
    PRIMARY KEY (candidate_id, lecture_id),
    CONSTRAINT fk_rcl_candidate FOREIGN KEY (candidate_id) REFERENCES recommendation_candidates (id) ON DELETE CASCADE,
    CONSTRAINT fk_rcl_lecture   FOREIGN KEY (lecture_id)   REFERENCES lectures (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS timetables (
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                  BIGINT      NOT NULL,
    year                     INT         NOT NULL,
    term_season              VARCHAR(10) NOT NULL,
    title                    VARCHAR(100) NOT NULL,
    status                   VARCHAR(15) NOT NULL DEFAULT 'DRAFT',
    source_recommendation_id BIGINT,
    created_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tt_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS timetable_items (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    lecture_id   BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ti_tt  FOREIGN KEY (timetable_id) REFERENCES timetables (id) ON DELETE CASCADE,
    CONSTRAINT fk_ti_lec FOREIGN KEY (lecture_id)   REFERENCES lectures (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    start_at    DATETIME     NOT NULL,
    end_at      DATETIME     NOT NULL,
    all_day     TINYINT(1)   NOT NULL DEFAULT 0,
    description VARCHAR(1000),
    remind_at   DATETIME,
    color       VARCHAR(20),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ev_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tasks (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    title          VARCHAR(200) NOT NULL,
    status         VARCHAR(15)  NOT NULL DEFAULT 'TODO',
    scheduled_date DATE,
    due_at         DATETIME,
    category       VARCHAR(50),
    remind_at      DATETIME,
    linked_event_id BIGINT,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_task_user  FOREIGN KEY (user_id)         REFERENCES users (id),
    CONSTRAINT fk_task_event FOREIGN KEY (linked_event_id) REFERENCES events (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
