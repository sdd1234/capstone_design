import { useState, useEffect } from "react";
import {
  listLectures,
  getLecture,
  getPrerequisites,
  getAcademicCalendarEvents,
} from "../api/academic";

const DAY_LABELS = {
  MON: "월",
  TUE: "화",
  WED: "수",
  THU: "목",
  FRI: "금",
  SAT: "토",
  SUN: "일",
};
const SEASON_LABELS = {
  SPRING: "1학기",
  SUMMER: "여름학기",
  FALL: "2학기",
  WINTER: "겨울학기",
};

export default function LecturePage() {
  const [tab, setTab] = useState("lectures");
  const [lectures, setLectures] = useState([]);
  const [academicEvents, setAcademicEvents] = useState([]);
  const [search, setSearch] = useState({
    year: 2026,
    termSeason: "SPRING",
    keyword: "",
  });
  const [selected, setSelected] = useState(null);
  const [prerequisites, setPrerequisites] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (tab === "academic") loadAcademicCalendar();
  }, [tab]);

  const handleSearch = async () => {
    setLoading(true);
    try {
      const params = {
        universityId: 1,
        year: search.year,
        termSeason: search.termSeason,
      };
      if (search.keyword) params.keyword = search.keyword;
      const res = await listLectures(params);
      setLectures(res.data.data || []);
    } catch {
      setLectures([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectLecture = async (lec) => {
    setSelected(lec);
    setPrerequisites([]);
    try {
      const res = await getPrerequisites(lec.courseId);
      setPrerequisites(res.data.data || []);
    } catch {
      setPrerequisites([]);
    }
  };

  const loadAcademicCalendar = async () => {
    try {
      const res = await getAcademicCalendarEvents(2026);
      setAcademicEvents(res.data.data || []);
    } catch {
      setAcademicEvents([]);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>강의 검색</h2>
        <div className="tab-group">
          <button
            className={`tab ${tab === "lectures" ? "active" : ""}`}
            onClick={() => setTab("lectures")}
          >
            강의 목록
          </button>
          <button
            className={`tab ${tab === "academic" ? "active" : ""}`}
            onClick={() => setTab("academic")}
          >
            학사 일정
          </button>
        </div>
      </div>

      {tab === "lectures" && (
        <>
          <div className="card">
            <div className="search-row">
              <select
                value={search.year}
                onChange={(e) =>
                  setSearch({ ...search, year: parseInt(e.target.value) })
                }
              >
                <option value={2026}>2026</option>
                <option value={2025}>2025</option>
              </select>
              <select
                value={search.termSeason}
                onChange={(e) =>
                  setSearch({ ...search, termSeason: e.target.value })
                }
              >
                <option value="SPRING">1학기</option>
                <option value="SUMMER">여름학기</option>
                <option value="FALL">2학기</option>
                <option value="WINTER">겨울학기</option>
              </select>
              <input
                placeholder="강의명·교수명 검색"
                value={search.keyword}
                onChange={(e) =>
                  setSearch({ ...search, keyword: e.target.value })
                }
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              />
              <button
                className="btn-primary"
                onClick={handleSearch}
                disabled={loading}
              >
                {loading ? "검색 중..." : "검색"}
              </button>
            </div>
          </div>

          <div className="split-layout">
            <div className="list-panel">
              {lectures.length === 0 && (
                <p className="empty">검색 결과가 없습니다.</p>
              )}
              {lectures.map((l) => (
                <div
                  key={l.id}
                  className={`list-item clickable ${selected?.id === l.id ? "active" : ""}`}
                  onClick={() => handleSelectLecture(l)}
                >
                  <div className="item-main">
                    <strong>{l.courseName}</strong>
                    <span className="item-meta">
                      {l.professor || "미정"} · {l.credits}학점 · {l.room || ""}
                    </span>
                    <div className="schedule-tags">
                      {(l.schedules || []).map((s, i) => (
                        <span key={i} className="tag">
                          {DAY_LABELS[s.dayOfWeek]} {s.startTime}~{s.endTime}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {selected && (
              <div className="detail-panel">
                <h3>{selected.courseName}</h3>
                <table className="detail-table">
                  <tbody>
                    <tr>
                      <th>강좌번호</th>
                      <td>{selected.lectureNumber}</td>
                    </tr>
                    <tr>
                      <th>교수</th>
                      <td>{selected.professor || "-"}</td>
                    </tr>
                    <tr>
                      <th>학점</th>
                      <td>{selected.credits}</td>
                    </tr>
                    <tr>
                      <th>이수구분</th>
                      <td>{selected.category || "-"}</td>
                    </tr>
                    <tr>
                      <th>강의실</th>
                      <td>{selected.room || "-"}</td>
                    </tr>
                    <tr>
                      <th>캠퍼스</th>
                      <td>{selected.campus || "-"}</td>
                    </tr>
                    <tr>
                      <th>영역</th>
                      <td>{selected.area || "-"}</td>
                    </tr>
                    <tr>
                      <th>원격여부</th>
                      <td>{selected.isRemote ? "원격" : "대면"}</td>
                    </tr>
                    <tr>
                      <th>시간</th>
                      <td>
                        {(selected.schedules || []).map((s, i) => (
                          <span key={i}>
                            {DAY_LABELS[s.dayOfWeek]} {s.startTime}~
                            {s.endTime}{" "}
                          </span>
                        ))}
                      </td>
                    </tr>
                  </tbody>
                </table>
                {prerequisites.length > 0 && (
                  <>
                    <h4>선수과목</h4>
                    <ul>
                      {prerequisites.map((p) => (
                        <li key={p.id}>
                          {p.prerequisiteCourseName}{" "}
                          {p.required ? "(필수)" : "(권장)"}
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </div>
            )}
          </div>
        </>
      )}

      {tab === "academic" && (
        <div className="list-section">
          {academicEvents.length === 0 ? (
            <p className="empty">학사 일정이 없습니다.</p>
          ) : (
            academicEvents.map((ev) => (
              <div
                key={ev.id}
                className={`list-item academic-${ev.category?.toLowerCase()}`}
              >
                <div className="item-main">
                  <span className="badge">{ev.category}</span>
                  <strong>{ev.title}</strong>
                  <span className="item-meta">
                    {ev.startDate} ~ {ev.endDate}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
