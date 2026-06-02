import { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { listTimetables } from "../api/timetable";
import {
  listPersonalBlocks,
  createPersonalBlock,
  deletePersonalBlock,
} from "../api/personalBlock";
import { getCurrentSemester } from "../utils/semester";

const DAY_LABELS = {
  MON: "월",
  TUE: "화",
  WED: "수",
  THU: "목",
  FRI: "금",
  SAT: "토",
  SUN: "일",
};
const DAYS_ORDER = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
const HOURS = Array.from({ length: 14 }, (_, i) => i + 9); // 9~22시
const CELL_H = 52;
const TIME_W = 60;
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
const PB_COLORS = [
  "#f97c4f",
  "#4fc97a",
  "#4f9cf9",
  "#c94fb8",
  "#a04fc9",
  "#64748b",
];

function LecBlock({ lec, idx, colW }) {
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
          left: `calc(${TIME_W}px + ${dayIndex} * ((100% - ${TIME_W}px) / 7))`,
          width: `calc((100% - ${TIME_W}px) / 7 - 4px)`,
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

function PBlock({ block, onDelete, colW }) {
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
        left: `calc(${TIME_W}px + ${dayIndex} * ((100% - ${TIME_W}px) / 7))`,
        width: `calc((100% - ${TIME_W}px) / 7 - 4px)`,
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
          position: "absolute",
          top: 1,
          right: 3,
          background: "transparent",
          border: "none",
          color: "#fff",
          cursor: "pointer",
          fontSize: "0.8rem",
          lineHeight: 1,
          padding: 0,
        }}
      >
        ✕
      </button>
      <div className="lec-name">{block.title}</div>
      <div className="lec-room">
        {block.startTime?.slice(0, 5)}~{block.endTime?.slice(0, 5)}
      </div>
    </div>
  );
}

function TimetableGrid({ lectures, personalBlocks, onDeleteBlock, colW }) {
  return (
    <div className="timetable-grid">
      <div className="grid-header" style={{ width: "100%" }}>
        <div style={{ width: `${TIME_W}px`, flexShrink: 0 }} />
        {DAYS_ORDER.map((d) => (
          <div
            key={d}
            className="day-col"
            style={{
              flex: 1,
              color:
                d === "SAT" ? "#4f9cf9" : d === "SUN" ? "#f94f4f" : undefined,
            }}
          >
            {DAY_LABELS[d]}
          </div>
        ))}
      </div>
      <div
        className="grid-body"
        style={{ width: "100%", height: `${HOURS.length * CELL_H}px` }}
      >
        <div
          style={{
            position: "absolute",
            width: `${TIME_W}px`,
            height: "100%",
            left: 0,
            backgroundColor: "#fafafa",
            borderRight: "1px solid var(--border)",
            zIndex: 1,
          }}
        />
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
          <LecBlock key={lec.id} lec={lec} idx={idx} colW={colW} />
        ))}
        {(personalBlocks || []).map((b) => (
          <PBlock
            key={`pb-${b.id}`}
            block={b}
            onDelete={onDeleteBlock}
            colW={colW}
          />
        ))}
      </div>
    </div>
  );
}

export default function TimetablePage() {
  const sem = getCurrentSemester();
  const containerRef = useRef(null);
  const [timetables, setTimetables] = useState([]);
  const [personalBlocks, setPersonalBlocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [colW, setColW] = useState(90); // 기본값
  const [currentTimetableIndex, setCurrentTimetableIndex] = useState(0);
  const [selectedSemester, setSelectedSemester] = useState(
    `${sem.year}-${sem.termSeason}`,
  );

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

  // 컨테이너 너비를 기반으로 COL_W 동적 계산
  useEffect(() => {
    if (!containerRef.current) return;

    const resizeObserver = new ResizeObserver((entries) => {
      for (let entry of entries) {
        const containerWidth = entry.contentRect.width;
        // 여유분 제외한 유효 너비 계산
        const availableWidth = containerWidth - TIME_W - 12; // 12px는 스크롤바 및 여유분
        const minColW = 70; // 최소 열 너비
        const maxColW = 160; // 최대 열 너비
        let newColW = Math.floor(availableWidth / DAYS_ORDER.length);
        newColW = Math.max(minColW, Math.min(maxColW, newColW));
        setColW(newColW);
      }
    });

    resizeObserver.observe(containerRef.current);
    return () => resizeObserver.disconnect();
  }, []);

  const loadAll = async () => {
    try {
      const [ttRes, pbRes] = await Promise.all([
        listTimetables(),
        listPersonalBlocks(sem.year, sem.termSeason),
      ]);
      let fetched = ttRes.data.data || [];
      setPersonalBlocks(pbRes.data.data || []);

      // 샘플 데이터 제거: 실제 저장된 timetables만 사용합니다.

      setTimetables(fetched);
    } catch (err) {
      const status = err.response?.status;
      const msg =
        err.response?.data?.message || err.message || "알 수 없는 오류";
      if (status === 401) setError("로그인이 필요합니다. (401)");
      else
        setError(`불러오지 못했습니다. [${status || "네트워크 오류"}] ${msg}`);
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

  // 선택된 학기(selectedSemester)에 해당하는 시간표들
  const [selYear, selTerm] = selectedSemester.split("-");
  const inSemester = timetables.filter(
    (t) =>
      String(t.year) === String(selYear) &&
      String(t.termSeason) === String(selTerm),
  );

  // 현재 인덱스가 유효한지 확인하고, 유효하지 않으면 0으로 리셋
  useEffect(() => {
    if (inSemester.length > 0 && currentTimetableIndex >= inSemester.length) {
      setCurrentTimetableIndex(0);
    }
  }, [inSemester, currentTimetableIndex]);

  const active = inSemester[currentTimetableIndex] || null;
  const totalCredits = (active?.lectures || []).reduce(
    (s, l) => s + (l.credits || 0),
    0,
  );

  const handlePrevTimetable = () => {
    setCurrentTimetableIndex((prev) =>
      prev === 0 ? inSemester.length - 1 : prev - 1,
    );
  };

  const handleNextTimetable = () => {
    setCurrentTimetableIndex((prev) =>
      prev === inSemester.length - 1 ? 0 : prev + 1,
    );
  };

  // 학기 드롭다운 옵션: 실제 `timetables`에 있는 학기만 표시
  const optMap = new Map();
  (timetables || []).forEach((t) => {
    const k = `${t.year}-${t.termSeason}`;
    if (!optMap.has(k)) {
      optMap.set(k, {
        key: k,
        year: Number(t.year),
        termSeason: Number(t.termSeason),
        label: t.label || `${t.year} ${t.termSeason}학기`,
      });
    }
  });

  let semesterOptions = Array.from(optMap.values());
  semesterOptions.sort((a, b) => {
    if (b.year !== a.year) return b.year - a.year;
    return (b.termSeason || 0) - (a.termSeason || 0);
  });

  // selectedSemester가 현재 옵션에 없으면 최신 옵션으로 설정
  useEffect(() => {
    if (semesterOptions.length === 0) return;
    const keys = semesterOptions.map((o) => o.key);
    if (!keys.includes(selectedSemester)) {
      setSelectedSemester(semesterOptions[0].key);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [timetables]);

  // selectedSemester 변경 시 인덱스 리셋
  useEffect(() => {
    setCurrentTimetableIndex(0);
  }, [selectedSemester]);

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>시간표</h2>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <select
            value={selectedSemester}
            onChange={(e) => setSelectedSemester(e.target.value)}
            style={{
              padding: "8px 14px",
              borderRadius: 8,
              border: "1.5px solid var(--primary)",
              background: "white",
              fontWeight: 600,
              height: 40,
              minWidth: 100,
              cursor: "pointer",
            }}
          >
            {semesterOptions.map((opt) => (
              <option key={opt.key} value={opt.key}>
                {opt.label}
              </option>
            ))}
          </select>
          <button
            className="btn-primary"
            onClick={() => setShowForm((v) => !v)}
            style={{
              height: 40,
              padding: "8px 14px",
              borderRadius: 8,
              boxSizing: "border-box",
              minWidth: 100,
              textAlign: "center",
            }}
          >
            {showForm ? "취소" : "+ 개인 일정"}
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      {showForm && (
        <div className="card" style={{ marginBottom: 16 }}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "flex-start",
              marginBottom: 16,
            }}
          >
            <div>
              <h3 style={{ marginTop: 0, marginBottom: 3 }}>
                알바·개인 일정 추가
              </h3>
              <p
                style={{
                  fontSize: "0.8rem",
                  color: "var(--muted)",
                  marginTop: 0,
                  marginBottom: 0,
                }}
              >
                매주 반복되는 일정으로 추가됩니다. (강의 시간표가 없어도 추가
                가능)
              </p>
            </div>
            <button
              type="submit"
              form="pbForm"
              className="btn-primary"
              style={{ flexShrink: 0, minWidth: 85 }}
            >
              추가
            </button>
          </div>

          <form
            id="pbForm"
            onSubmit={handleCreateBlock}
            style={{
              display: "flex",
              gap: 8,
              flexWrap: "wrap",
              alignItems: "flex-end",
            }}
          >
            <div className="field" style={{ flex: "2 1 180px" }}>
              <label>제목</label>
              <input
                value={blockForm.title}
                onChange={(e) =>
                  setBlockForm({ ...blockForm, title: e.target.value })
                }
                placeholder="예: 카페 알바"
                required
              />
            </div>
            <div className="field">
              <label>요일</label>
              <select
                value={blockForm.dayOfWeek}
                onChange={(e) =>
                  setBlockForm({ ...blockForm, dayOfWeek: e.target.value })
                }
              >
                {DAYS_ORDER.map((d) => (
                  <option key={d} value={d}>
                    {DAY_LABELS[d]}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>시작</label>
              <input
                type="time"
                value={blockForm.startTime}
                onChange={(e) =>
                  setBlockForm({ ...blockForm, startTime: e.target.value })
                }
                required
              />
            </div>
            <div className="field">
              <label>종료</label>
              <input
                type="time"
                value={blockForm.endTime}
                onChange={(e) =>
                  setBlockForm({ ...blockForm, endTime: e.target.value })
                }
                required
              />
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
                      width: 22,
                      height: 22,
                      borderRadius: "50%",
                      background: c,
                      cursor: "pointer",
                      border:
                        blockForm.color === c
                          ? "3px solid var(--text)"
                          : "2px solid var(--border)",
                    }}
                  />
                ))}
              </div>
            </div>
          </form>
        </div>
      )}

      {!loading && (
        <div
          className="timetable-view"
          style={{ marginTop: 8 }}
          ref={containerRef}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              marginBottom: 16,
            }}
          >
            <h3
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                margin: 0,
              }}
            >
              {active ? (
                <>
                  {active.title}
                  {active.isPrimary && (
                    <span className="primary-badge">대표</span>
                  )}
                  <span
                    style={{
                      fontSize: "0.8rem",
                      fontWeight: 400,
                      color: "var(--muted)",
                    }}
                  >
                    · {(active.lectures || []).length}강의 · {totalCredits}학점
                  </span>
                </>
              ) : (
                <span
                  style={{
                    fontSize: "0.9rem",
                    fontWeight: 400,
                    color: "var(--muted)",
                  }}
                >
                  {sem.label} 강의 시간표 없음 —{" "}
                  <Link
                    to="/ai"
                    style={{ color: "var(--primary)", fontWeight: 600 }}
                  >
                    AI 추천
                  </Link>{" "}
                  또는{" "}
                  <Link
                    to="/profile"
                    style={{ color: "var(--primary)", fontWeight: 600 }}
                  >
                    내 정보 &gt; 내 시간표
                  </Link>
                  에서 등록 · 아래 그리드에 개인 일정은 바로 추가할 수 있어요
                </span>
              )}
            </h3>
            {inSemester.length > 1 && (
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <button
                  className="btn-slider-nav"
                  onClick={handlePrevTimetable}
                  title="이전 시간표"
                  style={{
                    background: "none",
                    border: "1px solid var(--border)",
                    color: "var(--text)",
                    width: 32,
                    height: 32,
                    borderRadius: "6px",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "1rem",
                    transition: "all var(--transition)",
                  }}
                >
                  ‹
                </button>
                <span
                  style={{
                    fontSize: "0.85rem",
                    color: "var(--muted)",
                    minWidth: "40px",
                    textAlign: "center",
                  }}
                >
                  {currentTimetableIndex + 1} / {inSemester.length}
                </span>
                <button
                  className="btn-slider-nav"
                  onClick={handleNextTimetable}
                  title="다음 시간표"
                  style={{
                    background: "none",
                    border: "1px solid var(--border)",
                    color: "var(--text)",
                    width: 32,
                    height: 32,
                    borderRadius: "6px",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "1rem",
                    transition: "all var(--transition)",
                  }}
                >
                  ›
                </button>
              </div>
            )}
          </div>
          <TimetableGrid
            lectures={active ? active.lectures : []}
            personalBlocks={personalBlocks}
            onDeleteBlock={handleDeleteBlock}
            colW={colW}
          />
        </div>
      )}
    </div>
  );
}
