import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { listTimetables } from "../api/timetable";
import {
  listPersonalBlocks,
  createPersonalBlock,
  deletePersonalBlock,
} from "../api/personalBlock";
import { getCurrentSemester } from "../utils/semester";

const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const DAYS_ORDER = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
const HOURS = Array.from({ length: 14 }, (_, i) => i + 9); // 9~22시
const CELL_H = 52;
const COL_W = 90;
const TIME_W = 60;
const COLORS = [
  "#4f9cf9", "#f97c4f", "#4fc97a", "#c94fb8", "#f9c74f",
  "#a04fc9", "#4fc9c9", "#f94f4f", "#9cf94f", "#4fc9f9",
];
const PB_COLORS = ["#f97c4f", "#4fc97a", "#4f9cf9", "#c94fb8", "#a04fc9", "#64748b"];

function LecBlock({ lec, idx }) {
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
        key={`lec-${lec.id}-${si}`}
        className="lecture-block"
        style={{
          top: `${top}px`,
          left: `${TIME_W + dayIndex * COL_W}px`,
          width: `${COL_W - 4}px`,
          height: `${height}px`,
          backgroundColor: COLORS[idx % COLORS.length],
          zIndex: 2,
        }}
      >
        <div className="lec-name">{lec.courseName}</div>
        <div className="lec-room">{lec.room || ""}</div>
      </div>
    );
  });
}

function PBlock({ block, onDelete }) {
  const dayIndex = DAYS_ORDER.indexOf(block.dayOfWeek);
  if (dayIndex < 0) return null;
  const [sh, sm] = block.startTime.split(":").map(Number);
  const [eh, em] = block.endTime.split(":").map(Number);
  const top = (sh - 9 + sm / 60) * CELL_H;
  const height = Math.max((eh - sh + (em - sm) / 60) * CELL_H, 24);
  if (top < 0 || top > HOURS.length * CELL_H) return null;
  return (
    <div
      className="lecture-block"
      style={{
        top: `${top}px`,
        left: `${TIME_W + dayIndex * COL_W}px`,
        width: `${COL_W - 4}px`,
        height: `${height}px`,
        backgroundColor: block.color || "#64748b",
        border: "2px dashed rgba(255,255,255,0.7)",
        zIndex: 3,
      }}
    >
      <button
        type="button"
        title="삭제"
        onClick={() => onDelete(block.id)}
        style={{
          position: "absolute", top: 1, right: 3, background: "transparent",
          border: "none", color: "#fff", cursor: "pointer", fontSize: "0.8rem", lineHeight: 1, padding: 0,
        }}
      >
        ✕
      </button>
      <div className="lec-name">{block.title}</div>
      <div className="lec-room">{block.startTime?.slice(0, 5)}~{block.endTime?.slice(0, 5)}</div>
    </div>
  );
}

function TimetableGrid({ lectures, personalBlocks, onDeleteBlock }) {
  return (
    <div className="timetable-grid">
      <div className="grid-header">
        <div className="time-col" style={{ width: TIME_W }} />
        {DAYS_ORDER.map((d) => (
          <div
            key={d}
            className="day-col"
            style={d === "SAT" ? { color: "#4f9cf9" } : d === "SUN" ? { color: "#f94f4f" } : undefined}
          >
            {DAY_LABELS[d]}
          </div>
        ))}
      </div>
      <div
        className="grid-body"
        style={{ position: "relative", height: `${HOURS.length * CELL_H}px`, minWidth: `${TIME_W + DAYS_ORDER.length * COL_W}px` }}
      >
        {HOURS.map((h) => (
          <div key={h} className="hour-row" style={{ top: `${(h - 9) * CELL_H}px` }}>
            <span className="hour-label">{h}:00</span>
          </div>
        ))}
        {(lectures || []).map((lec, idx) => (
          <LecBlock key={lec.id} lec={lec} idx={idx} />
        ))}
        {(personalBlocks || []).map((b) => (
          <PBlock key={`pb-${b.id}`} block={b} onDelete={onDeleteBlock} />
        ))}
      </div>
    </div>
  );
}

export default function TimetablePage() {
  const sem = getCurrentSemester();
  const [timetables, setTimetables] = useState([]);
  const [personalBlocks, setPersonalBlocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [blockForm, setBlockForm] = useState({
    title: "",
    dayOfWeek: "MON",
    startTime: "18:00",
    endTime: "22:00",
    color: PB_COLORS[0],
  });

  useEffect(() => {
    loadAll();
  }, []);

  const loadAll = async () => {
    try {
      const [ttRes, pbRes] = await Promise.all([
        listTimetables(),
        listPersonalBlocks(sem.year, sem.termSeason),
      ]);
      setTimetables(ttRes.data.data || []);
      setPersonalBlocks(pbRes.data.data || []);
    } catch (err) {
      const status = err.response?.status;
      const msg = err.response?.data?.message || err.message || "알 수 없는 오류";
      if (status === 401) setError("로그인이 필요합니다. (401)");
      else setError(`불러오지 못했습니다. [${status || "네트워크 오류"}] ${msg}`);
    } finally {
      setLoading(false);
    }
  };

  const reloadBlocks = async () => {
    try {
      const res = await listPersonalBlocks(sem.year, sem.termSeason);
      setPersonalBlocks(res.data.data || []);
    } catch {
      /* ignore */
    }
  };

  const handleCreateBlock = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await createPersonalBlock({
        year: sem.year,
        termSeason: sem.termSeason,
        title: blockForm.title.trim(),
        dayOfWeek: blockForm.dayOfWeek,
        startTime: blockForm.startTime,
        endTime: blockForm.endTime,
        color: blockForm.color,
      });
      setShowForm(false);
      setBlockForm((f) => ({ ...f, title: "" }));
      reloadBlocks();
    } catch (err) {
      setError(err.response?.data?.message || "개인 일정 추가 실패");
    }
  };

  const handleDeleteBlock = async (id) => {
    if (!window.confirm("이 개인 일정을 삭제할까요?")) return;
    try {
      await deletePersonalBlock(id);
      reloadBlocks();
    } catch (err) {
      setError(err.response?.data?.message || "삭제 실패");
    }
  };

  // 현재 학기 시간표 중 '대표' 우선, 없으면 가장 최근(목록은 서버에서 createdAt desc 정렬)
  const inSemester = timetables.filter(
    (t) => t.year === sem.year && t.termSeason === sem.termSeason,
  );
  const active = inSemester.find((t) => t.isPrimary) || inSemester[0] || null;
  const totalCredits = (active?.lectures || []).reduce((s, l) => s + (l.credits || 0), 0);

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>시간표</h2>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span className="sem-badge">{sem.label}</span>
          <button className="btn-primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? "취소" : "+ 개인 일정"}
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      {showForm && (
        <div className="card" style={{ marginBottom: 16 }}>
          <h3 style={{ marginTop: 0 }}>알바·개인 일정 추가</h3>
          <p style={{ fontSize: "0.8rem", color: "var(--muted)", marginTop: 0 }}>
            매주 반복되는 일정으로 추가됩니다. (강의 시간표가 없어도 추가 가능)
          </p>
          <form onSubmit={handleCreateBlock} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
            <div className="field" style={{ flex: "2 1 180px" }}>
              <label>제목</label>
              <input
                value={blockForm.title}
                onChange={(e) => setBlockForm({ ...blockForm, title: e.target.value })}
                placeholder="예: 카페 알바"
                required
              />
            </div>
            <div className="field">
              <label>요일</label>
              <select value={blockForm.dayOfWeek} onChange={(e) => setBlockForm({ ...blockForm, dayOfWeek: e.target.value })}>
                {DAYS_ORDER.map((d) => <option key={d} value={d}>{DAY_LABELS[d]}</option>)}
              </select>
            </div>
            <div className="field">
              <label>시작</label>
              <input type="time" value={blockForm.startTime} onChange={(e) => setBlockForm({ ...blockForm, startTime: e.target.value })} required />
            </div>
            <div className="field">
              <label>종료</label>
              <input type="time" value={blockForm.endTime} onChange={(e) => setBlockForm({ ...blockForm, endTime: e.target.value })} required />
            </div>
            <div className="field">
              <label>색상</label>
              <div style={{ display: "flex", gap: 6 }}>
                {PB_COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => setBlockForm({ ...blockForm, color: c })}
                    aria-label={`색상 ${c}`}
                    style={{
                      width: 22, height: 22, borderRadius: "50%", background: c, cursor: "pointer",
                      border: blockForm.color === c ? "3px solid var(--text)" : "2px solid var(--border)",
                    }}
                  />
                ))}
              </div>
            </div>
            <button type="submit" className="btn-primary">추가</button>
          </form>
        </div>
      )}

      {!loading && (
        <div className="timetable-view" style={{ marginTop: 8 }}>
          <h3 style={{ display: "flex", alignItems: "center", gap: 8 }}>
            {active ? (
              <>
                {active.title}
                {active.isPrimary && <span className="primary-badge">대표</span>}
                <span style={{ fontSize: "0.8rem", fontWeight: 400, color: "var(--muted)" }}>
                  · {(active.lectures || []).length}강의 · {totalCredits}학점
                </span>
              </>
            ) : (
              <span style={{ fontSize: "0.9rem", fontWeight: 400, color: "var(--muted)" }}>
                {sem.label} 강의 시간표 없음 — <Link to="/ai" style={{ color: "var(--primary)", fontWeight: 600 }}>AI 추천</Link> 또는{" "}
                <Link to="/profile" style={{ color: "var(--primary)", fontWeight: 600 }}>내 정보 &gt; 내 시간표</Link>에서 등록 · 아래 그리드에 개인 일정은 바로 추가할 수 있어요
              </span>
            )}
          </h3>
          <TimetableGrid
            lectures={active ? active.lectures : []}
            personalBlocks={personalBlocks}
            onDeleteBlock={handleDeleteBlock}
          />
        </div>
      )}
    </div>
  );
}
