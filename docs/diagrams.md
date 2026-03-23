# CampusFit 시스템 다이어그램

---

## 1. ERD (Entity Relationship Diagram)

```mermaid
erDiagram

  users {
    BIGINT id PK
    VARCHAR email UK
    VARCHAR password_hash
    VARCHAR name
    VARCHAR status
    VARCHAR role
    TINYINT marketing_agree
    DATETIME created_at
    DATETIME updated_at
  }

  universities {
    BIGINT id PK
    VARCHAR name
    VARCHAR domain
    DATETIME created_at
  }

  files {
    BIGINT id PK
    VARCHAR original_name
    VARCHAR stored_path
    VARCHAR mime_type
    BIGINT size
    VARCHAR purpose
    BIGINT uploaded_by FK
    DATETIME created_at
  }

  student_verifications {
    BIGINT id PK
    BIGINT user_id FK
    BIGINT file_id FK
    VARCHAR verification_type
    VARCHAR status
    VARCHAR reject_reason
    BIGINT reviewed_by FK
    DATETIME created_at
    DATETIME updated_at
  }

  courses {
    BIGINT id PK
    BIGINT university_id FK
    VARCHAR name
    INT credits
    VARCHAR category
    BIGINT dept_id
    DATETIME created_at
  }

  course_prerequisites {
    BIGINT id PK
    BIGINT course_id FK
    BIGINT prerequisite_course_id FK
  }

  lectures {
    BIGINT id PK
    BIGINT course_id FK
    BIGINT university_id FK
    INT academic_year
    VARCHAR term_season
    VARCHAR professor
    VARCHAR room
    TINYINT is_remote
    VARCHAR dept
    INT target_grade
    VARCHAR lecture_number
    VARCHAR area
    VARCHAR campus
    DATETIME created_at
  }

  lecture_schedules {
    BIGINT id PK
    BIGINT lecture_id FK
    VARCHAR day_of_week
    TIME start_time
    TIME end_time
  }

  lecture_import_logs {
    BIGINT id PK
    BIGINT university_id
    INT year
    VARCHAR term_season
    VARCHAR file_name
    INT imported_count
    DATETIME imported_at
  }

  timetable_preferences {
    BIGINT id PK
    BIGINT user_id FK
    INT academic_year
    VARCHAR term_season
    DATETIME created_at
    DATETIME updated_at
  }

  preferred_time_ranges {
    BIGINT id PK
    BIGINT preference_id FK
    VARCHAR type
    VARCHAR day_of_week
    TIME start_time
    TIME end_time
  }

  desired_courses {
    BIGINT id PK
    BIGINT preference_id FK
    BIGINT course_id
    VARCHAR raw_text
    INT priority
  }

  credit_policies {
    BIGINT id PK
    BIGINT preference_id FK
    INT min_credits
    INT max_credits
    INT target_credits
    INT target_major_credits
    INT target_general_credits
    INT target_remote_credits
  }

  preference_options {
    BIGINT id PK
    BIGINT preference_id FK
    TINYINT exclude_morning
    INT allow_gaps_minutes
    INT max_days_per_week
    VARCHAR dept
    TINYINT prefer_major_only
    INT grade
  }

  ai_timetable_recommendations {
    BIGINT id PK
    BIGINT user_id FK
    INT year
    VARCHAR term_season
    TEXT request_params_json
    DATETIME created_at
  }

  recommendation_candidates {
    BIGINT id PK
    BIGINT recommendation_id FK
    INT rank
    INT total_credits
  }

  recommendation_candidate_lectures {
    BIGINT candidate_id FK
    BIGINT lecture_id FK
  }

  timetables {
    BIGINT id PK
    BIGINT user_id FK
    INT year
    VARCHAR term_season
    VARCHAR title
    VARCHAR status
    BIGINT source_recommendation_id
    DATETIME created_at
    DATETIME updated_at
  }

  timetable_items {
    BIGINT id PK
    BIGINT timetable_id FK
    BIGINT lecture_id FK
  }

  academic_calendar_events {
    BIGINT id PK
    BIGINT university_id FK
    VARCHAR title
    DATE start_date
    DATE end_date
    VARCHAR category
    INT year
    DATETIME created_at
  }

  events {
    BIGINT id PK
    BIGINT user_id FK
    VARCHAR title
    VARCHAR category
    DATETIME start_at
    DATETIME end_at
    TINYINT all_day
    VARCHAR description
    DATETIME remind_at
    VARCHAR color
    DATETIME created_at
  }

  tasks {
    BIGINT id PK
    BIGINT user_id FK
    VARCHAR title
    VARCHAR status
    DATE scheduled_date
    DATETIME due_at
    VARCHAR category
    BIGINT linked_event_id FK
    DATETIME created_at
  }

  users ||--o{ files                        : "uploads"
  users ||--o{ student_verifications        : "submits"
  users ||--o{ student_verifications        : "reviews"
  users ||--o{ timetable_preferences        : "has"
  users ||--o{ ai_timetable_recommendations : "requests"
  users ||--o{ timetables                   : "owns"
  users ||--o{ events                       : "creates"
  users ||--o{ tasks                        : "creates"

  universities ||--o{ courses                  : "offers"
  universities ||--o{ lectures                 : "holds"
  universities ||--o{ academic_calendar_events : "publishes"

  courses ||--o{ lectures             : "realized as"
  courses ||--o{ course_prerequisites : "requires"
  courses ||--o{ course_prerequisites : "is prerequisite of"

  lectures ||--o{ lecture_schedules                 : "has schedule"
  lectures ||--o{ recommendation_candidate_lectures : "included in"
  lectures ||--o{ timetable_items                   : "placed in"

  timetable_preferences ||--o{ preferred_time_ranges : "avoids/prefers"
  timetable_preferences ||--o{ desired_courses        : "wants"
  timetable_preferences ||--|| credit_policies        : "has"
  timetable_preferences ||--|| preference_options     : "has"

  ai_timetable_recommendations ||--o{ recommendation_candidates          : "generates"
  recommendation_candidates    ||--o{ recommendation_candidate_lectures  : "contains"

  timetables ||--o{ timetable_items : "contains"

  events ||--o{ tasks : "linked to"
  files  ||--|| student_verifications : "attached to"
```

---

## 2. DB 스키마 클래스 다이어그램

```mermaid
classDiagram
  direction TB

  class users {
    +BIGINT id
    +VARCHAR email
    +VARCHAR password_hash
    +VARCHAR name
    +VARCHAR status
    +VARCHAR role
    +TINYINT marketing_agree
    +DATETIME created_at
    +DATETIME updated_at
  }

  class universities {
    +BIGINT id
    +VARCHAR name
    +VARCHAR domain
  }

  class courses {
    +BIGINT id
    +BIGINT university_id
    +VARCHAR name
    +INT credits
    +VARCHAR category
    +BIGINT dept_id
  }

  class lectures {
    +BIGINT id
    +BIGINT course_id
    +BIGINT university_id
    +INT academic_year
    +VARCHAR term_season
    +VARCHAR professor
    +VARCHAR room
    +TINYINT is_remote
    +VARCHAR dept
    +INT target_grade
  }

  class lecture_schedules {
    +BIGINT id
    +BIGINT lecture_id
    +VARCHAR day_of_week
    +TIME start_time
    +TIME end_time
  }

  class timetable_preferences {
    +BIGINT id
    +BIGINT user_id
    +INT academic_year
    +VARCHAR term_season
  }

  class credit_policies {
    +BIGINT id
    +BIGINT preference_id
    +INT min_credits
    +INT max_credits
    +INT target_credits
    +INT target_major_credits
    +INT target_general_credits
    +INT target_remote_credits
  }

  class preference_options {
    +BIGINT id
    +BIGINT preference_id
    +TINYINT exclude_morning
    +INT allow_gaps_minutes
    +INT max_days_per_week
    +VARCHAR dept
    +TINYINT prefer_major_only
    +INT grade
  }

  class preferred_time_ranges {
    +BIGINT id
    +BIGINT preference_id
    +VARCHAR type
    +VARCHAR day_of_week
    +TIME start_time
    +TIME end_time
  }

  class desired_courses {
    +BIGINT id
    +BIGINT preference_id
    +BIGINT course_id
    +VARCHAR raw_text
    +INT priority
  }

  class ai_timetable_recommendations {
    +BIGINT id
    +BIGINT user_id
    +INT year
    +VARCHAR term_season
    +TEXT request_params_json
  }

  class recommendation_candidates {
    +BIGINT id
    +BIGINT recommendation_id
    +INT rank
    +INT total_credits
  }

  class recommendation_candidate_lectures {
    +BIGINT candidate_id
    +BIGINT lecture_id
  }

  class timetables {
    +BIGINT id
    +BIGINT user_id
    +INT year
    +VARCHAR term_season
    +VARCHAR title
    +VARCHAR status
  }

  class timetable_items {
    +BIGINT id
    +BIGINT timetable_id
    +BIGINT lecture_id
  }

  class events {
    +BIGINT id
    +BIGINT user_id
    +VARCHAR title
    +VARCHAR category
    +DATETIME start_at
    +DATETIME end_at
    +TINYINT all_day
  }

  class tasks {
    +BIGINT id
    +BIGINT user_id
    +VARCHAR title
    +VARCHAR status
    +DATE scheduled_date
    +BIGINT linked_event_id
  }

  class files {
    +BIGINT id
    +VARCHAR original_name
    +VARCHAR stored_path
    +VARCHAR purpose
    +BIGINT uploaded_by
  }

  class student_verifications {
    +BIGINT id
    +BIGINT user_id
    +BIGINT file_id
    +VARCHAR verification_type
    +VARCHAR status
    +BIGINT reviewed_by
  }

  users "1" --> "0..*" timetable_preferences : has
  users "1" --> "0..*" ai_timetable_recommendations : requests
  users "1" --> "0..*" timetables : owns
  users "1" --> "0..*" events : creates
  users "1" --> "0..*" tasks : creates
  users "1" --> "0..*" files : uploads
  users "1" --> "0..*" student_verifications : submits

  universities "1" --> "0..*" courses : offers
  universities "1" --> "0..*" lectures : holds

  courses "1" --> "0..*" lectures : realized as
  lectures "1" --> "0..*" lecture_schedules : has

  timetable_preferences "1" --> "1" credit_policies : has
  timetable_preferences "1" --> "1" preference_options : has
  timetable_preferences "1" --> "0..*" preferred_time_ranges : avoids/prefers
  timetable_preferences "1" --> "0..*" desired_courses : wants

  ai_timetable_recommendations "1" --> "0..*" recommendation_candidates : generates
  recommendation_candidates "1" --> "0..*" recommendation_candidate_lectures : contains
  recommendation_candidate_lectures --> lectures : refers to

  timetables "1" --> "0..*" timetable_items : contains
  timetable_items --> lectures : refers to

  events "1" --> "0..*" tasks : linked to
  files "1" --> "1" student_verifications : attached to
```

---

## 3. 백엔드 아키텍처 컴포넌트 다이어그램

```mermaid
graph TD
  FE["🖥️ React Frontend\n:5173"]

  subgraph Security["🔒 Security Layer"]
    CORS[CorsConfig]
    JF[JwtAuthFilter]
    JU[JwtUtil]
    SC[SecurityConfig\npermitAll / authenticated]
  end

  subgraph API["🌐 REST Controllers  /api/v1/"]
    AC["AuthController\n/auth/**"]
    UC["UserController\n/users/**"]
    LC["LectureController\n/lectures/**"]
    PC["PreferenceController\n/timetable-preferences"]
    AIC["AiController\n/ai/**"]
    TTC["TimetableController\n/timetables/**"]
    CC["CalendarController\n/calendar/**"]
    ADC["AdminController\n/admin/**"]
    STC["StorageController\n/files/**"]
  end

  subgraph SVC["⚙️ Services"]
    AS[AuthService]
    US[UserService]
    LS[LectureService]
    PS[TimetablePreferenceService]
    AIS[AiRecommendationService]
    TTS[TimetableService]
    CS[CalendarService]
    ADS[AdminService]
    STS[StorageService]
  end

  subgraph REPO["🗄️ Repositories (JPA)"]
    UserR[UserRepository]
    LecR[LectureRepository]
    CourseR[CourseRepository]
    PrefR[TimetablePreferenceRepository]
    AiR[AiRecommendationRepository]
    TTR[TimetableRepository]
    CalR[EventRepository / TaskRepository]
  end

  subgraph DOM["📦 Domain Entities (22 Tables)"]
    UE[User]
    LE["Lecture\nLectureSchedule"]
    CE["Course\nCoursePrerequisite"]
    PE["TimetablePreference\nCreditPolicy\nPreferenceOption\nDesiredCourse\nPreferredTimeRange"]
    AE["AiTimetableRecommendation\nRecommendationCandidate"]
    TE["Timetable\nTimetableItem"]
    EVE["Event\nTask"]
    FE2["FileEntity\nStudentVerification"]
    UNI[University]
  end

  DB[("🐬 MySQL :3306\ncampusfit")]

  FE -->|"JWT Bearer"| Security
  Security --> API
  AC --> AS --> UserR
  UC --> US --> UserR
  LC --> LS --> LecR
  LC --> LS --> CourseR
  PC --> PS --> PrefR
  AIC --> AIS --> LecR
  AIC --> AIS --> PrefR
  AIC --> AIS --> AiR
  TTC --> TTS --> TTR
  CC --> CS --> CalR
  ADC --> ADS --> UserR
  STC --> STS

  REPO --> DOM
  DOM --> DB
```

---

## 4. 프론트엔드 컴포넌트 다이어그램

```mermaid
graph TD
  subgraph Entry["⚡ 진입점"]
    Main["main.jsx\nReactDOM.createRoot"]
  end

  subgraph AppShell["🏠 App.jsx  (React Router v6)"]
    Layout["Layout.jsx\n공통 헤더 · 네비게이션"]
    PR["ProtectedRoute.jsx\nJWT 인증 가드"]
  end

  subgraph Public["🔓 공개 페이지"]
    Login[LoginPage]
    Signup[SignupPage]
  end

  subgraph Protected["🔐 인증 필요 페이지"]
    Profile[ProfilePage]
    Timetable[TimetablePage]
    AiRec[AiRecommendationPage]
    Pref[PreferencePage]
    Lecture[LecturePage]
    Calendar[CalendarPage]
    Admin[AdminPage]
  end

  subgraph ApiLayer["📡 API Layer  (axios)"]
    Client["client.js\napiClient\nJWT interceptor\ntoken refresh"]
    AuthApi[auth.js]
    UserApi[user.js]
    AcadApi[academic.js]
    TimetApi[timetable.js]
    PrefApi[preference.js]
    AiApi[ai.js]
    CalApi[calendar.js]
    AdminApi[admin.js]
  end

  BE["☁️ Spring Boot API\n:8080"]

  Main --> AppShell
  AppShell --> Layout
  AppShell --> PR
  AppShell --> Public

  PR --> Protected

  Login --> AuthApi
  Signup --> AuthApi

  Profile --> UserApi
  Timetable --> TimetApi
  AiRec --> AiApi
  AiRec --> PrefApi
  Pref --> PrefApi
  Pref --> AcadApi
  Lecture --> AcadApi
  Calendar --> CalApi
  Admin --> AdminApi
  Admin --> AcadApi

  AuthApi --> Client
  UserApi --> Client
  AcadApi --> Client
  TimetApi --> Client
  PrefApi --> Client
  AiApi --> Client
  CalApi --> Client
  AdminApi --> Client

  Client -->|"REST / JWT Bearer"| BE
```

---

## 5. AI 추천 흐름 시퀀스 다이어그램

```mermaid
sequenceDiagram
  actor User as 사용자
  participant FE as React Frontend
  participant API as Spring Boot API
  participant AI as AiRecommendationService
  participant DB as MySQL

  User->>FE: AI 추천 요청 클릭
  FE->>API: POST /api/v1/ai/recommend\n{year, termSeason}
  API->>DB: 선호 설정 조회\n(timetable_preferences + 연관 테이블)
  DB-->>API: 선호 설정 반환
  API->>DB: 강의 목록 조회\n(lectures + schedules, 해당 연도/학기)
  DB-->>API: 강의 목록 반환

  Note over API,AI: AI 알고리즘 실행
  AI->>AI: 1. AVOID 시간대 강의 제거
  AI->>AI: 2. 9시 수업 제외 (옵션)
  AI->>AI: 3. 희망 과목 우선 배치
  AI->>AI: 4. 전공 강의 채우기 (target_major_credits)
  AI->>AI: 5. 교양 강의 채우기 (target_general_credits)
  AI->>AI: 6. 원격 강의 채우기 (target_remote_credits)
  AI->>AI: 7. 나머지 학점 채우기
  AI->>AI: 3개 후보 시간표 생성

  API->>DB: 추천 결과 저장\n(ai_timetable_recommendations\n recommendation_candidates)
  DB-->>API: 저장된 ID 반환
  API-->>FE: 추천 결과 3개 반환
  FE-->>User: 시간표 3개 시각화 표시

  User->>FE: 시간표 확정
  FE->>API: POST /api/v1/timetables\n{candidateId, title}
  API->>DB: timetables + timetable_items 저장
  DB-->>API: 완료
  API-->>FE: 확정된 시간표 반환
  FE-->>User: 내 시간표에 저장 완료
```

---

## 6. 인증 흐름 시퀀스 다이어그램

```mermaid
sequenceDiagram
  actor User as 사용자
  participant FE as React Frontend
  participant API as Spring Boot API
  participant JWT as JwtUtil
  participant DB as MySQL

  User->>FE: 로그인 (이메일/비밀번호)
  FE->>API: POST /api/v1/auth/login
  API->>DB: 사용자 조회 (email)
  DB-->>API: User 엔티티
  API->>JWT: Access Token 생성 (30분)
  API->>JWT: Refresh Token 생성 (14일)
  JWT-->>API: 토큰 페어
  API-->>FE: {accessToken, refreshToken, user}
  FE->>FE: localStorage 저장

  Note over FE,API: 인증 필요 API 호출 시
  FE->>API: 요청 + Authorization: Bearer {accessToken}
  API->>JWT: 토큰 검증
  JWT-->>API: userId 추출
  API->>DB: 비즈니스 로직 처리
  DB-->>API: 결과
  API-->>FE: 응답

  Note over FE,API: 토큰 만료 시
  FE->>API: 요청 (만료된 토큰)
  API-->>FE: 401 Unauthorized
  FE->>API: POST /api/v1/auth/refresh\n{refreshToken}
  API->>JWT: Refresh Token 검증
  JWT-->>API: 유효
  API-->>FE: 새 Access Token
  FE->>FE: localStorage 갱신
  FE->>API: 원래 요청 재시도
```

---

## 7. 시스템 배포 구성도

```mermaid
graph LR
  subgraph Dev["💻 개발 환경 (localhost)"]
    subgraph FrontEnd["Frontend"]
      Vite["Vite Dev Server\n:5173 / :5174"]
      Browser["Chrome / 브라우저"]
    end

    subgraph BackEnd["Backend"]
      Spring["Spring Boot\n:8080\n(Java 21 + Maven)"]
      Upload["uploads/\n(파일 저장소)"]
    end

    subgraph Data["Database"]
      MySQL["MySQL 8.4\n:3306\nDB: campusfit\nUser: campus_user"]
    end
  end

  Browser -->|"HTTP :5173"| Vite
  Vite -->|"HMR / ESBuild"| Browser
  Vite -->|"Proxy /api → :8080"| Spring
  Spring -->|"JDBC"| MySQL
  Spring -->|"파일 I/O"| Upload
  Spring -->|"엑셀 임포트"| MySQL
```

---

## 8. 테이블 그룹 마인드맵

```mermaid
mindmap
  root((campusfit DB\n22 Tables))
    사용자
      users
      files
      student_verifications
    대학·강의
      universities
      courses
      course_prerequisites
      lectures
      lecture_schedules
      lecture_import_logs
    AI 시간표 추천
      timetable_preferences
        credit_policies
        preference_options
        preferred_time_ranges
        desired_courses
      ai_timetable_recommendations
        recommendation_candidates
          recommendation_candidate_lectures
    확정 시간표
      timetables
      timetable_items
    학사 일정
      academic_calendar_events
    개인 일정
      events
      tasks
```
