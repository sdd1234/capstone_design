import { useState, useEffect } from "react";
import {
  listTimetables,
  createTimetable,
  deleteTimetable,
} from "../api/timetable";
import { listLectures } from "../api/academic";

const SEMESTER_OPTIONS = [
  { label: "2026 1학기", year: 2026, termSeason: "SPRING" },
  { label: "2026 여름방학", year: 2026, termSeason: "SUMMER" },
  { label: "2026 2학기", year: 2026, termSeason: "FALL" },
  { label: "2026 겨울방학", year: 2026, termSeason: "WINTER" },
  { label: "2025 2학기", year: 2025, termSeason: "FALL" },
  { label: "2025 1학기", year: 2025, termSeason: "SPRING" },
];
const TERM_LABELS = {
  SPRING: "1학기",
  SUMMER: "여름방학",
  FALL: "2학기",
  WINTER: "겨울방학",
};
const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금" };
const DAYS_ORDER = ["MON", "TUE", "WED", "THU", "FRI"];
const HOURS = Array.from({ length: 14 }, (_, i) => i + 9); // 9~22시
const CELL_H = 52;
const COLORS = [
  "#4f9cf9",
  "#f97c4f",
  "#4fc97a",
  "#c94fb8",
  "#f9c74f",
  "#a04fc9",
  "#4fc9c9",
  "#f94f4f",
  "#9cf94f",
  "#4fc9f9",
];

function LecBlock({ lec, idx, opacity = 1, dashed = false, prefix = "lec" }) {
  return (lec.schedules || []).map((s, si) => {
    const dayIndex = DAYS_ORDER.indexOf(s.dayOfWeek);
    if (dayIndex < 0) return null;
    const [sh, sm] = s.startTime.split(":").map(Number);
    const [eh, em] = s.endTime.split(":").map(Number);
    const top = (sh - 9 + sm / 60) * CELL_H;
    const height = Math.max((eh - sh + (em - sm) / 60) * CELL_H, 24);
    if (top < 0 || top > HOURS.length * CELL_H) return null;
    return (
      <div
        key={`${prefix}-${lec.id}-${si}`}
        className="lecture-block"
        style={{
          top: `${top}px`,
          left: `${60 + dayIndex * 90}px`,
          width: "86px",
          height: `${height}px`,
          backgroundColor: COLORS[idx % COLORS.length],
          opacity,
          border: dashed ? "2px dashed rgba(0,0,0,0.3)" : "none",
          zIndex: opacity < 1 ? 5 : 2,
        }}
      >
        <div className="lec-name">{lec.courseName}</div>
        <div className="lec-room">{lec.room || ""}</div>
      </div>
    );
  });
}

function TimetableGrid({ lectures, previewLectures = [], hoverLec = null }) {
  const baseCount = (lectures || []).length;
  return (
    <div className="timetable-grid">
      <div className="grid-header">
        <div className="time-col" />
        {["월", "화", "수", "목", "금"].map((d) => (
          <div key={d} className="day-col">
            {d}
          </div>
        ))}
      </div>
      <div
        className="grid-body"
        style={{ position: "relative", height: `${HOURS.length * CELL_H}px` }}
      >
        {HOURS.map((h) => (
          <div
            key={h}
            className="hour-row"
            style={{ top: `${(h - 9) * CELL_H}px` }}
          >
            <span className="hour-label">{h}:00</span>
          </div>
        ))}
        {(lectures || []).map((lec, idx) => (
          <LecBlock key={lec.id} lec={lec} idx={idx} />
        ))}
        {previewLectures.map((lec, i) => (
          <LecBlock
            key={lec.id}
            lec={lec}
            idx={baseCount + i}
            opacity={0.55}
            dashed={true}
            prefix="checked"
          />
        ))}
        {hoverLec && (
          <LecBlock
            key={hoverLec.id}
            lec={hoverLec}
            idx={baseCount + previewLectures.length}
            opacity={0.35}
            dashed={true}
            prefix="hover"
          />
        )}
      </div>
    </div>
  );
}

function parseSemKey(k) {
  try {
    const [y, t] = k.split("_");
    return { year: parseInt(y), termSeason: t };
  } catch {
    return { year: 2026, termSeason: "SPRING" };
  }
}
function makeSemKey(year, termSeason) {
  return `${year}_${termSeason}`;
}

export default function TimetablePage() {
  const initKey = localStorage.getItem("tt_semkey") || "2026_SPRING";
  const initSem = parseSemKey(initKey);
  const [timetables, setTimetables] = useState([]);
  const [semKey, setSemKey] = useState(initKey);
  const { year: selYear, termSeason: selectedTerm } = parseSemKey(semKey);
  const [selected, setSelected] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    year: initSem.year,
    termSeason: initSem.termSeason,
  });
  const [lectures, setLectures] = useState([]);
  const [selectedLecIds, setSelectedLecIds] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [hoverLec, setHoverLec] = useState(null);
  const [error, setError] = useState("");

  const filtered = timetables.filter(
    (t) => t.year === selYear && t.termSeason === selectedTerm,
  );

  useEffect(() => {
    loadTimetables();
  }, []);

  useEffect(() => {
    const savedId = localStorage.getItem(`tt_sel_${selYear}_${selectedTerm}`);
    const match =
      filtered.find((t) => String(t.id) === savedId) || filtered[0] || null;
    setSelected(match);
  }, [semKey, timetables]); // eslint-disable-line

  const loadTimetables = async () => {
    try {
      const res = await listTimetables();
      setTimetables(res.data.data || []);
    } catch (err) {
      const status = err.response?.status;
      const msg =
        err.response?.data?.message || err.message || "알 수 없는 오류";
      console.error("시간표 로드 실패:", err);
      if (status === 401) {
        setError("로그인이 필요합니다. (401)");
      } else {
        setError(
          `시간표를 불러오지 못했습니다. [${status || "네트워크 오류"}] ${msg}`,
        );
      }
    }
  };

  const handleSemChange = (key) => {
    setSemKey(key);
    localStorage.setItem("tt_semkey", key);
    const sem = parseSemKey(key);
    setForm((f) => ({ ...f, year: sem.year, termSeason: sem.termSeason }));
  };

  const handleSelectTt = (t) => {
    setSelected(t);
    localStorage.setItem(`tt_sel_${t.year}_${t.termSeason}`, String(t.id));
  };

  const handleSearchLectures = async () => {
    try {
      const res = await listLectures({
        universityId: 1,
        year: form.year,
        termSeason: form.termSeason,
        keyword: searchKeyword || undefined,
      });
      setLectures(res.data.data || []);
    } catch {
      setLectures([]);
    }
  };

  const toggleLecture = (id) => {
    setSelectedLecIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id],
    );
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const res = await createTimetable({
        ...form,
        lectureIds: selectedLecIds,
      });
      const created = res.data?.data;
      setShowForm(false);
      setSelectedLecIds([]);
      setLectures([]);
      setHoverLec(null);
      const newKey = makeSemKey(form.year, form.termSeason);
      setSemKey(newKey);
      localStorage.setItem("tt_semkey", newKey);
      if (created) {
        localStorage.setItem(
          `tt_sel_${form.year}_${form.termSeason}`,
          String(created.id),
        );
        setTimetables((prev) => [
          created,
          ...prev.filter((t) => t.id !== created.id),
        ]);
      }
    } catch (err) {
      setError(err.response?.data?.message || "시간표 생성 실패");
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await deleteTimetable(id);
      const next = timetables.filter((t) => t.id !== id);
      setTimetables(next);
      if (selected?.id === id) {
        const nextFiltered = next.filter(
          (t) => t.year === selYear && t.termSeason === selectedTerm,
        );
        setSelected(nextFiltered[0] || null);
      }
    } catch {
      setError("삭제 실패");
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>시간표</h2>
        <button className="btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "취소" : "+ 새 시간표"}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {/* Semester Selector */}
      <div className="semester-selector">
        <label className="sem-label">학기 선택</label>
        <select
          className="sem-select"
          value={semKey}
          onChange={(e) => handleSemChange(e.target.value)}
        >
          {SEMESTER_OPTIONS.map(({ label, year, termSeason }) => (
            <option
              key={`${year}_${termSeason}`}
              value={`${year}_${termSeason}`}
            >
              {label}
            </option>
          ))}
        </select>
      </div>

      {showForm && (
        <div className="card">
          <h3>새 시간표 만들기</h3>
          <form onSubmit={handleCreate} className="form-grid">
            <div className="field">
              <label>연도</label>
              <input
                type="number"
                value={form.year}
                onChange={(e) =>
                  setForm({ ...form, year: parseInt(e.target.value) })
                }
              />
            </div>
            <div className="field">
              <label>학기</label>
              <select
                value={form.termSeason}
                onChange={(e) =>
                  setForm({ ...form, termSeason: e.target.value })
                }
              >
                <option value="SPRING">1학기</option>
                <option value="SUMMER">여름방학</option>
                <option value="FALL">2학기</option>
                <option value="WINTER">겨울방학</option>
              </select>
            </div>
          </form>
          <div className="lecture-search">
            <div className="search-row">
              <input
                placeholder="강의명 검색"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
              />
              <button className="btn-secondary" onClick={handleSearchLectures}>
                검색
              </button>
            </div>
            {lectures.length > 0 && (
              <table className="table">
                <thead>
                  <tr>
                    <th>선택</th>
                    <th>강의명</th>
                    <th>교수</th>
                    <th>학점</th>
                    <th>시간</th>
                  </tr>
                </thead>
                <tbody>
                  {lectures.map((l) => (
                    <tr
                      key={l.id}
                      className={
                        selectedLecIds.includes(l.id) ? "selected" : ""
                      }
                      onMouseEnter={() => setHoverLec(l)}
                      onMouseLeave={() => setHoverLec(null)}
                    >
                      <td>
                        <input
                          type="checkbox"
                          checked={selectedLecIds.includes(l.id)}
                          onChange={() => toggleLecture(l.id)}
                        />
                      </td>
                      <td>{l.courseName}</td>
                      <td>{l.professor || "-"}</td>
                      <td>{l.credits}</td>
                      <td>
                        {(l.schedules || []).map((s, i) => (
                          <span key={i}>
                            {DAY_LABELS[s.dayOfWeek]} {s.startTime}~
                            {s.endTime}{" "}
                          </span>
                        ))}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <div className="form-actions">
            <button
              type="submit"
              className="btn-primary"
              onClick={handleCreate}
            >
              저장
            </button>
          </div>
        </div>
      )}

      <div className="timetable-layout">
        <div className="timetable-list">
          {filtered.map((t) => (
            <div
              key={t.id}
              className={`timetable-item ${selected?.id === t.id ? "active" : ""}`}
              onClick={() => handleSelectTt(t)}
            >
              <div className="tt-title">{t.title}</div>
              <div className="tt-meta">
                {t.year} {TERM_LABELS[t.termSeason] || t.termSeason} ·{" "}
                {(t.lectures || []).length}강의
              </div>
              <button
                className="btn-danger-sm"
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(t.id);
                }}
              >
                삭제
              </button>
            </div>
          ))}
          {filtered.length === 0 && (
            <p className="empty">이 학기의 시간표가 없습니다.</p>
          )}
        </div>
        <div className="timetable-view">
          {selected && <h3>{selected.title}</h3>}
          <TimetableGrid
            lectures={selected ? selected.lectures : []}
            previewLectures={
              showForm
                ? lectures.filter((l) => selectedLecIds.includes(l.id))
                : []
            }
            hoverLec={
              showForm && hoverLec && !selectedLecIds.includes(hoverLec.id)
                ? hoverLec
                : null
            }
          />
        </div>
      </div>
    </div>
  );
}
