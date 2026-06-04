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
// 소프트 톤 팔레트 — 같은 이름(내용)은 항상 같은 색, 다른 이름은 다른 색
const PALETTE = [
  { bg: "#ECECFE", fg: "#4B45C7", bar: "#6C63F0" }, // indigo
  { bg: "#E6F0FD", fg: "#2563C0", bar: "#4C8DF5" }, // blue
  { bg: "#E0F2F1", fg: "#127E78", bar: "#22A89F" }, // teal
  { bg: "#E6F4EA", fg: "#1E8A4C", bar: "#34A865" }, // green
  { bg: "#FBF0DC", fg: "#A9760E", bar: "#E0A226" }, // amber
  { bg: "#FCEBE0", fg: "#B85C1C", bar: "#EE8B47" }, // orange
  { bg: "#FCE7EE", fg: "#BC3A6E", bar: "#E76A9C" }, // rose
  { bg: "#F2E9FB", fg: "#7C3FCB", bar: "#A874E8" }, // violet
];
// 등장 순서대로 팔레트 색 배정 — 같은 이름은 같은 색, 다른 이름은 (8개까진) 항상 다른 색
let TONE_MAP = new Map();
function buildToneMap(lectures, personalBlocks) {
  const m = new Map();
  let i = 0;
  const add = (name) => {
    if (!name || m.has(name)) return;
    m.set(name, PALETTE[i % PALETTE.length]);
    i++;
  };
  (lectures || []).forEach((l) => add(l.courseName));
  (personalBlocks || []).forEach((b) => add(b.title));
  TONE_MAP = m;
}
function toneFor(name) {
  if (TONE_MAP.has(name)) return TONE_MAP.get(name);
  // 폴백: 맵에 없으면 해시 (이론상 도달 안 함)
  let h = 0;
  const s = name || "";
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return PALETTE[h % PALETTE.length];
}

function LecBlock({ lec, onSelect }) {
  return (lec.schedules || []).map((s, si) => {
    const dayIndex = DAYS_ORDER.indexOf(s.dayOfWeek);
    if (dayIndex < 0) return null;
    const [sh, sm] = s.startTime.split(":").map(Number);
    const [eh, em] = s.endTime.split(":").map(Number);
    const top = (sh - 9 + sm / 60) * CELL_H;
    const height = Math.max((eh - sh + (em - sm) / 60) * CELL_H, 24);
    if (top < 0 || top > HOURS.length * CELL_H) return null;
    const tone = toneFor(lec.courseName);
    return (
      <div
        key={`lec-${lec.id}-${si}`}
        className="lecture-block"
        onClick={() => onSelect({ kind: "lec", lec })}
        style={{
          top: `${top}px`,
          left: `calc(${TIME_W}px + ${dayIndex} * ((100% - ${TIME_W}px) / 7))`,
          width: `calc((100% - ${TIME_W}px) / 7 - 4px)`,
          height: `${height}px`,
          backgroundColor: tone.bg,
          color: tone.fg,
          zIndex: 2,
        }}
      >
        <div className="lec-name">{lec.courseName}</div>
      </div>
    );
  });
}

function PBlock({ block, onDelete, onSelect }) {
  const dayIndex = DAYS_ORDER.indexOf(block.dayOfWeek);
  if (dayIndex < 0) return null;
  const [sh, sm] = block.startTime.split(":").map(Number);
  const [eh, em] = block.endTime.split(":").map(Number);
  const top = (sh - 9 + sm / 60) * CELL_H;
  const height = Math.max((eh - sh + (em - sm) / 60) * CELL_H, 24);
  if (top < 0 || top > HOURS.length * CELL_H) return null;
  const tone = toneFor(block.title);
  return (
    <div
      className="lecture-block"
      onClick={() => onSelect({ kind: "pb", block })}
      style={{
        top: `${top}px`,
        left: `calc(${TIME_W}px + ${dayIndex} * ((100% - ${TIME_W}px) / 7))`,
        width: `calc((100% - ${TIME_W}px) / 7 - 4px)`,
        height: `${height}px`,
        backgroundColor: tone.bg,
        color: tone.fg,
        zIndex: 3,
      }}
    >
      <button
        type="button"
        title="삭제"
        onClick={(e) => {
          e.stopPropagation();
          onDelete(block.id);
        }}
        style={{
          position: "absolute",
          top: 1,
          right: 3,
          background: "transparent",
          border: "none",
          color: "var(--text-3)",
          cursor: "pointer",
          fontSize: "0.8rem",
          lineHeight: 1,
          padding: 0,
        }}
      >
        ✕
      </button>
      <div className="lec-name">{block.title}</div>
    </div>
  );
}

function TimetableGrid({ lectures, personalBlocks, onDeleteBlock, onSelect, colW }) {
  buildToneMap(lectures, personalBlocks);
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
                d === "SAT" ? "#3E76D4" : d === "SUN" ? "#D9486B" : undefined,
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
        {(lectures || []).map((lec) => (
          <LecBlock key={lec.id} lec={lec} onSelect={onSelect} />
        ))}
        {(personalBlocks || []).map((b) => (
          <PBlock
            key={`pb-${b.id}`}
            block={b}
            onDelete={onDeleteBlock}
            onSelect={onSelect}
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
  const [detail, setDetail] = useState(null); // 블록 클릭 시 상세 팝업
  const [currentTimetableIndex, setCurrentTimetableIndex] = useState(0);
  const [selectedSemester, setSelectedSemester] = useState(
    `${sem.year}-${sem.termSeason}`,
  );

  // 현재 보고 있는 학기 (개인일정 생성·조회는 이 학기 기준)
  const viewSem = {
    year: Number(selectedSemester.split("-")[0]),
    termSeason: selectedSemester.split("-")[1],
  };

  const [showForm, setShowForm] = useState(false);
  const [blockForm, setBlockForm] = useState({
    title: "",
    days: ["MON"], // 선택된 요일들 (여러 개 가능)
    startTime: "18:00",
    endTime: "22:00",
  });

  const toggleDay = (d) =>
    setBlockForm((f) => ({
      ...f,
      days: f.days.includes(d)
        ? f.days.filter((x) => x !== d)
        : [...f.days, d],
    }));

  useEffect(() => {
    loadAll();
  }, []);

  // 보고 있는 학기가 바뀌면 개인일정 다시 로드
  useEffect(() => {
    reloadBlocks();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedSemester]);

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
      const ttRes = await listTimetables();
      let fetched = ttRes.data.data || [];

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
      const res = await listPersonalBlocks(viewSem.year, viewSem.termSeason);
      setPersonalBlocks(res.data.data || []);
    } catch {
      /* ignore */
    }
  };

  const handleCreateBlock = async (e) => {
    e.preventDefault();
    setError("");
    if (!blockForm.days.length) {
      setError("반복할 요일을 하나 이상 선택하세요.");
      return;
    }
    const ns = blockForm.startTime.slice(0, 5);
    const ne = blockForm.endTime.slice(0, 5);
    if (ne <= ns) {
      setError("종료 시간은 시작 시간보다 늦어야 합니다.");
      return;
    }

    // 같은 요일·시간대에 이미 강의/개인일정이 있으면 그 요일은 등록 불가
    const overlaps = (s1, e1, s2, e2) => s1 < e2 && s2 < e1;
    const hm = (t) => (t || "").slice(0, 5);
    const lecturesNow = active?.lectures || [];
    const hasConflict = (day) => {
      const pbHit = personalBlocks.some(
        (b) =>
          b.dayOfWeek === day &&
          overlaps(ns, ne, hm(b.startTime), hm(b.endTime)),
      );
      const lecHit = lecturesNow.some((l) =>
        (l.schedules || []).some(
          (s) =>
            s.dayOfWeek === day &&
            overlaps(ns, ne, hm(s.startTime), hm(s.endTime)),
        ),
      );
      return pbHit || lecHit;
    };

    const sortedDays = DAYS_ORDER.filter((d) => blockForm.days.includes(d));
    const okDays = sortedDays.filter((d) => !hasConflict(d));
    const conflictDays = sortedDays
      .filter((d) => hasConflict(d))
      .map((d) => DAY_LABELS[d]);

    if (!okDays.length) {
      setError(
        `${conflictDays.join("·")}요일은 같은 시간대에 이미 일정이 있어 등록할 수 없습니다.`,
      );
      return;
    }

    try {
      // 겹치지 않는 요일만 매주 반복 블록으로 생성
      for (const day of okDays) {
        await createPersonalBlock({
          year: viewSem.year,
          termSeason: viewSem.termSeason,
          title: blockForm.title.trim(),
          dayOfWeek: day,
          startTime: blockForm.startTime,
          endTime: blockForm.endTime,
        });
      }
      await reloadBlocks();
      if (conflictDays.length) {
        // 일부만 등록됨 — 폼은 열어둔 채 어떤 요일이 빠졌는지 안내
        setError(
          `${conflictDays.join("·")}요일은 겹치는 일정이 있어 제외하고 등록했습니다.`,
        );
      } else {
        setShowForm(false);
        setBlockForm((f) => ({ ...f, title: "" }));
      }
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
  const inSemester = timetables
    .filter(
      (t) =>
        String(t.year) === String(selYear) &&
        String(t.termSeason) === String(selTerm),
    )
    .sort((a, b) => a.id - b.id); // 먼저 만든 시간표가 1페이지로

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
          {semesterOptions.length > 1 && (
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
          )}
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
                선택한 요일마다 매주 반복됩니다. 여러 요일을 고르면 그만큼
                한 번에 추가돼요. (강의 시간표가 없어도 추가 가능)
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
            <div className="field" style={{ flex: "1 1 100%" }}>
              <label>반복 요일 (여러 개 선택 가능)</label>
              <div style={{ display: "flex", gap: 6, marginBottom: 6 }}>
                <button
                  type="button"
                  className="btn-day-quick"
                  onClick={() => setBlockForm((f) => ({ ...f, days: [...DAYS_ORDER] }))}
                >
                  매일
                </button>
                <button
                  type="button"
                  className="btn-day-quick"
                  onClick={() =>
                    setBlockForm((f) => ({
                      ...f,
                      days: ["MON", "TUE", "WED", "THU", "FRI"],
                    }))
                  }
                >
                  주중(월~금)
                </button>
                <button
                  type="button"
                  className="btn-day-quick"
                  onClick={() => setBlockForm((f) => ({ ...f, days: [] }))}
                >
                  비우기
                </button>
              </div>
              <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                {DAYS_ORDER.map((d) => {
                  const on = blockForm.days.includes(d);
                  return (
                    <button
                      key={d}
                      type="button"
                      onClick={() => toggleDay(d)}
                      className={`day-chip${on ? " on" : ""}`}
                    >
                      {DAY_LABELS[d]}
                    </button>
                  );
                })}
              </div>
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
            onSelect={setDetail}
            colW={colW}
          />
        </div>
      )}

      {detail && (
        <BlockDetail detail={detail} onClose={() => setDetail(null)} />
      )}
    </div>
  );
}

// 블록 클릭 시 자세한 시간/강의실 팝업
function BlockDetail({ detail, onClose }) {
  const isLec = detail.kind === "lec";
  const lec = detail.lec;
  const block = detail.block;
  const title = isLec ? lec.courseName : block.title;

  const rows = isLec
    ? (lec.schedules || []).map((s) => ({
        day: DAY_LABELS[s.dayOfWeek] || s.dayOfWeek,
        time: `${s.startTime?.slice(0, 5)} ~ ${s.endTime?.slice(0, 5)}`,
      }))
    : [
        {
          day: DAY_LABELS[block.dayOfWeek] || block.dayOfWeek,
          time: `${block.startTime?.slice(0, 5)} ~ ${block.endTime?.slice(0, 5)}`,
        },
      ];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ width: "100%" }}>
        <div className="modal-header">
          <h3 style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span
              style={{
                width: 10,
                height: 10,
                borderRadius: 3,
                background: toneFor(title).bar,
                flexShrink: 0,
              }}
            />
            {title}
          </h3>
          <button className="btn-icon-close" onClick={onClose}>
            ✕
          </button>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          {rows.map((r, i) => (
            <div
              key={i}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "10px 12px",
                background: "var(--bg)",
                borderRadius: 10,
              }}
            >
              <span style={{ fontWeight: 700, color: "var(--primary)", minWidth: 20 }}>
                {r.day}
              </span>
              <span style={{ fontWeight: 600 }}>{r.time}</span>
            </div>
          ))}

          {isLec && (lec.professor || lec.room || lec.credits != null) && (
            <div style={{ fontSize: "0.88rem", color: "var(--text-secondary)", lineHeight: 1.7, marginTop: 2 }}>
              {lec.room && <div>📍 {lec.room}</div>}
              {lec.professor && <div>👤 {lec.professor}</div>}
              {lec.credits != null && <div>🎓 {lec.credits}학점</div>}
            </div>
          )}
          {!isLec && (
            <div style={{ fontSize: "0.82rem", color: "var(--muted)", marginTop: 2 }}>
              매주 반복되는 개인 일정
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
