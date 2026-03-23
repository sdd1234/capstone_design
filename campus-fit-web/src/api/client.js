import axios from "axios";

// Mock 모드 (true: 백엔드 없이 테스트 / false: 실제 백엔드 연결)
const MOCK_MODE = false;

const mockData = {
  user: {
    id: 1,
    email: "test@kmu.ac.kr",
    name: "테스트 사용자",
    role: "USER",
    status: "ACTIVE",
    marketingAgree: false,
  },
  timetables: [
    {
      id: 1,
      title: "2026 1학기",
      year: 2026,
      termSeason: "SPRING",
      items: [
        {
          id: 1,
          lecture: {
            id: 1,
            courseName: "Python 프로그래밍",
            professor: "김교수",
            room: "공학관 101",
            schedules: [
              { dayOfWeek: "MON", startTime: "09:00", endTime: "10:30" },
              { dayOfWeek: "WED", startTime: "09:00", endTime: "10:30" },
            ],
          },
        },
        {
          id: 2,
          lecture: {
            id: 2,
            courseName: "데이터베이스",
            professor: "이교수",
            room: "공학관 202",
            schedules: [
              { dayOfWeek: "TUE", startTime: "13:00", endTime: "14:30" },
              { dayOfWeek: "THU", startTime: "13:00", endTime: "14:30" },
            ],
          },
        },
      ],
      totalCredits: 6,
      createdAt: "2026-03-21T00:00:00",
    },
  ],
  lectures: [
    {
      id: 1,
      courseName: "Python 프로그래밍",
      professor: "김교수",
      room: "공학관 101",
      schedules: [],
    },
    {
      id: 2,
      courseName: "데이터베이스",
      professor: "이교수",
      room: "공학관 202",
      schedules: [],
    },
    {
      id: 3,
      courseName: "웹개발",
      professor: "박교수",
      room: "공학관 303",
      schedules: [],
    },
  ],
  academicCalendar: [
    {
      id: 1,
      title: "2026 1학기 시작",
      startDate: "2026-03-02",
      endDate: "2026-03-02",
      category: "SEMESTER",
    },
    {
      id: 2,
      title: "수강신청 기간",
      startDate: "2026-02-24",
      endDate: "2026-02-28",
      category: "REGISTRATION",
    },
    {
      id: 3,
      title: "중간고사",
      startDate: "2026-04-20",
      endDate: "2026-05-01",
      category: "EXAM",
    },
  ],
  events: [],
  tasks: [],
};

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 10000,
});

// Mock 응답 처리
if (MOCK_MODE) {
  apiClient.interceptors.response.use(
    (res) => res,
    (error) => {
      const url = error.config?.url || "";

      // 사용자 정보
      if (url.includes("/api/v1/users/me")) {
        return Promise.resolve({ data: { data: mockData.user } });
      }

      // 시간표 목록
      if (url === "/api/v1/timetables") {
        if (error.config?.method === "get") {
          return Promise.resolve({ data: { data: mockData.timetables } });
        }
        if (error.config?.method === "post") {
          const newTimetable = { ...mockData.timetables[0], id: Date.now() };
          mockData.timetables.push(newTimetable);
          return Promise.resolve({ data: { data: newTimetable } });
        }
      }

      // 시간표 상세
      if (url.match(/\/api\/v1\/timetables\/\d+/)) {
        return Promise.resolve({ data: { data: mockData.timetables[0] } });
      }

      // 강의 목록
      if (url.includes("/api/v1/lectures")) {
        return Promise.resolve({ data: { data: mockData.lectures } });
      }

      // 학사 일정
      if (url.includes("/api/v1/academic-calendar")) {
        return Promise.resolve({ data: { data: mockData.academicCalendar } });
      }

      // 이벤트
      if (url === "/api/v1/events") {
        if (error.config?.method === "get") {
          return Promise.resolve({ data: { data: mockData.events } });
        }
        if (error.config?.method === "post") {
          const newEvent = { id: Date.now(), ...error.config?.data };
          mockData.events.push(newEvent);
          return Promise.resolve({ data: { data: newEvent } });
        }
      }

      // 태스크
      if (url === "/api/v1/tasks") {
        if (error.config?.method === "get") {
          return Promise.resolve({ data: { data: mockData.tasks } });
        }
        if (error.config?.method === "post") {
          const newTask = { id: Date.now(), ...error.config?.data };
          mockData.tasks.push(newTask);
          return Promise.resolve({ data: { data: newTask } });
        }
      }

      return Promise.reject(error);
    },
  );
}

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 공통 에러 로깅
apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    console.error(
      `[API 오류] ${err.config?.method?.toUpperCase()} ${err.config?.url} → ${err.response?.status || "네트워크 오류"}`,
      err.response?.data || err.message,
    );
    return Promise.reject(err);
  },
);

export default apiClient;
