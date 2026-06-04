import { useState, useEffect } from "react";
import {
  listGoals,
  createGoal,
  updateGoal,
  deleteGoal,
  getBlankSlots,
  getActivityRecommendations,
  listAppliedAllocations,
  getWeeklySummary,
  applyAllocations,
  toggleAllocationCompleted,
  clearAppliedAllocationsForWeek,
} from "../api/timeManagement";
import { listTimetables, getTimetable } from "../api/timetable";
import { getAcademicCalendarEvents } from "../api/academic";

const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
const HOURS = Array.from({ length: 14 }, (_, i) => i + 9); // 9~22
const ROW_H = 24;
const COL_W = 72;

const CATEGORIES = [
  { value: "STUDY", label: "공부" },
  { value: "EXERCISE", label: "운동" },
  { value: "MEAL", label: "식사" },
  { value: "HOBBY", label: "취미" },
  { value: "REST", label: "휴식" },
  { value: "OTHER", label: "기타" },
];

const CATEGORY_COLORS = {
  STUDY: "#4f9cf9",
  EXERCISE: "#f97c4f",
  MEAL: "#f9c74f",
  HOBBY: "#a04fc9",
  REST: "#4fc97a",
  OTHER: "#888",
};

// (deprecated — 자연어 입력으로 대체)

function timeToMin(t) {
  if (!t) return null;
  const [h, m] = t.split(":").map(Number);
  return h * 60 + m;
}

/** 그 주의 월요일 (LocalDate ISO 문자열) */
function mondayOf(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const dow = d.getDay() || 7; // SUN=0→7
  d.setDate(d.getDate() - (dow - 1));
  return d.toISOString().slice(0, 10);
}

function fmtWeekRange(weekStart) {
  const start = new Date(weekStart);
  const end = new Date(start);
  end.setDate(end.getDate() + 6);
  const fmt = (d) => `${d.getMonth() + 1}/${d.getDate()}`;
  return `${fmt(start)} ~ ${fmt(end)}`;
}

function shiftWeek(weekStart, deltaWeeks) {
  const d = new Date(weekStart);
  d.setDate(d.getDate() + deltaWeeks * 7);
  return d.toISOString().slice(0, 10);
}

/**
 * 자연어 한 줄을 파싱해 활동 목표 payload 생성.
 * 룰 기반 베이스라인 (ChatGPT 연동 시 더 정교하게 대체 예정).
 *
 * 인식하는 패턴:
 *  - 카테고리 키워드 (운동/공부/식사/취미/휴식)
 *  - "N시간" / "N H" / "주 N시간" — 주당 시간 (default 3)
 */
const CATEGORY_KEYWORDS = [
  [/운동|헬스|러닝|조깅|요가|필라테스|등산|수영|스트레칭|크로스핏/, "EXERCISE"],
  [/공부|학습|복습|예습|코딩|개발|알고리즘|토익|영어|독학|과제|시험준비|독서실/, "STUDY"],
  [/식사|밥|점심|저녁|아침|식단|요리|식이|브런치/, "MEAL"],
  [/독서|책읽기|취미|그림|드로잉|음악|영화|드라마|게임|악기|기타|피아노/, "HOBBY"],
  [/휴식|수면|낮잠|쉼|명상|산책|힐링/, "REST"],
];

function parseGoalFromText(text) {
  const t = text.trim();
  if (!t) return null;

  let category = "OTHER";
  for (const [re, cat] of CATEGORY_KEYWORDS) {
    if (re.test(t)) { category = cat; break; }
  }

  // "N시간" 패턴 — N에 한국어 숫자 일부 + 아라비아 숫자
  const hourMatch = t.match(/(\d+)\s*(?:시간|H|h)/);
  let hours = hourMatch ? parseInt(hourMatch[1]) : 3;
  hours = Math.max(1, Math.min(40, hours));

  return {
    title: t,
    category,
    targetHoursPerWeek: hours,
    preferredDayOfWeek: null,
    preferredStartTime: null,
    preferredEndTime: null,
    preferredMode: "WINDOW",
    rawInput: t, // ChatGPT 연동 시 더 정교하게 재파싱할 원본 보존
  };
}

export default function TimeManagementPage() {
  const [year, setYear] = useState(2026);
  const [termSeason, setTermSeason] = useState("SPRING");
  const [weekStart, setWeekStart] = useState(mondayOf(new Date()));
  const [goals, setGoals] = useState([]);
  const [blanks, setBlanks] = useState([]);
  const [allocations, setAllocations] = useState([]);
  const [appliedAllocations, setAppliedAllocations] = useState([]);
  const [summary, setSummary] = useState(null);
  const [lectures, setLectures] = useState([]); // 사용자 시간표의 강의들
  const [examEvents, setExamEvents] = useState([]); // 학사 캘린더 시험 일정
  const [nlInput, setNlInput] = useState("");
  const [adding, setAdding] = useState(false);
  const [loading, setLoading] = useState(false);
  const [applying, setApplying] = useState(false);
  const [error, setError] = useState("");
  const [msg, setMsg] = useState("");

  useEffect(() => {
    loadAll();
  }, [year, termSeason, weekStart]);

  const loadAll = async () => {
    setError("");
    try {
      const [gRes, bRes, ttRes, calRes, aRes, sRes] = await Promise.all([
        listGoals(),
        getBlankSlots(year, termSeason, weekStart),
        listTimetables(),
        getAcademicCalendarEvents(year),
        listAppliedAllocations(weekStart),
        getWeeklySummary(weekStart),
      ]);
      setGoals(gRes.data.data || []);
      setBlanks(bRes.data.data || []);
      setAppliedAllocations(aRes.data.data || []);
      setSummary(sRes.data.data || null);

      // 현재 학기 시간표 → 강의 schedules 수집
      const myTts = (ttRes.data.data || []).filter(
        (t) => t.year === year && t.termSeason === termSeason,
      );
      const lecs = [];
      for (const t of myTts) {
        if (Array.isArray(t.lectures)) {
          lecs.push(...t.lectures);
        } else {
          try {
            const detail = await getTimetable(t.id);
            const ls = detail.data.data?.lectures || [];
            lecs.push(...ls);
          } catch { /* ignore */ }
        }
      }
      // 중복 제거 (id 기준)
      const seen = new Set();
      const unique = lecs.filter((l) => {
        if (seen.has(l.id)) return false;
        seen.add(l.id);
        return true;
      });
      setLectures(unique);

      // 학사 캘린더 → 중간/기말/종강/방학 D-Day 추출
      // 학기에 맞는 월만 (5월에 12월 동계방학 D-Day 띄우는 거 방지)
      //  - SPRING(1학기): 3-8월 일정 (중간/기말/종강/하계방학)
      //  - FALL(2학기): 9-12 + 1-2월 일정 (중간/기말/종강/동계방학)
      //  - SUMMER/WINTER(계절학기): 필터 X
      const termMonths = termSeason === "SPRING"
        ? new Set([3, 4, 5, 6, 7, 8])
        : termSeason === "FALL"
        ? new Set([9, 10, 11, 12, 1, 2])
        : null;
      const inTerm = (e) => {
        if (!termMonths) return true;
        const m = new Date(e.startDate).getMonth() + 1;
        return termMonths.has(m);
      };
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const dayDiff = (dateStr) =>
        Math.ceil((new Date(dateStr) - today) / (1000 * 60 * 60 * 24));
      const all = (calRes.data.data || []).filter(inTerm);
      const highlights = [];

      // 중간고사 = "수업일수 ½선" (또는 "1/2선") — 한국 대학 일반 관례
      all
        .filter((e) => (e.title || "").includes("½선") || (e.title || "").includes("1/2선"))
        .forEach((e) =>
          highlights.push({
            id: `mid-${e.id}`, label: "중간고사", title: e.title,
            startDate: e.startDate, endDate: e.endDate, daysLeft: dayDiff(e.startDate),
            color: "#f9c74f",
          }),
        );

      // 기말고사 = EXAM 카테고리 또는 정기시험
      const finals = all.filter(
        (e) => e.category === "EXAM" || (e.title || "").includes("정기시험"),
      );
      finals.forEach((e) => {
        highlights.push({
          id: `fin-${e.id}`, label: "기말고사", title: e.title,
          startDate: e.startDate, endDate: e.endDate, daysLeft: dayDiff(e.startDate),
          color: "#f97c4f",
        });
        // 종강 = 정기시험 endDate
        if (e.endDate && e.endDate !== e.startDate) {
          highlights.push({
            id: `end-${e.id}`, label: "종강", title: e.title.replace("정기시험", "종강"),
            startDate: e.endDate, endDate: e.endDate, daysLeft: dayDiff(e.endDate),
            color: "#4fc97a",
          });
        }
      });

      // 방학 = "방학" 키워드
      all
        .filter((e) => (e.title || "").includes("방학"))
        .forEach((e) =>
          highlights.push({
            id: `brk-${e.id}`, label: "방학 시작", title: e.title,
            startDate: e.startDate, endDate: e.endDate, daysLeft: dayDiff(e.startDate),
            color: "#4f9cf9",
          }),
        );

      const filtered = highlights
        .filter((e) => e.daysLeft >= -7) // 지난 일정은 7일까지만
        .sort((a, b) => a.daysLeft - b.daysLeft);
      setExamEvents(filtered);
    } catch (err) {
      setError(err.response?.data?.message || "불러오기 실패");
    }
  };

  const handleNaturalLanguageAdd = async (e) => {
    e.preventDefault();
    setError("");
    setMsg("");
    const text = nlInput.trim();
    if (!text) {
      setError("자연어로 활동을 한 줄 적어주세요. 예: '운동 주 3시간'");
      return;
    }
    const payload = parseGoalFromText(text);
    if (!payload) {
      setError("파싱 실패 — 다시 시도해주세요.");
      return;
    }
    setAdding(true);
    try {
      await createGoal(payload, year, termSeason);
      setMsg(`✓ "${payload.title}" 추가됨 (${payload.category}, 주 ${payload.targetHoursPerWeek}시간)`);
      setNlInput("");
      loadAll();
      setTimeout(() => setMsg(""), 3000);
    } catch (err) {
      setError(err.response?.data?.message || "저장 실패");
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await deleteGoal(id);
      loadAll();
    } catch (err) {
      setError(err.response?.data?.message || "삭제 실패");
    }
  };

  const handleRecommend = async () => {
    setError("");
    setLoading(true);
    try {
      const res = await getActivityRecommendations(year, termSeason, weekStart);
      setAllocations(res.data.data || []);
      const bRes = await getBlankSlots(year, termSeason, weekStart);
      setBlanks(bRes.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || "추천 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async () => {
    // GOAL 타입만 AppliedAllocation으로 적용 — TASK는 별도 흐름(현재는 미리보기만)
    const goalPlaced = allocations.filter(
      (a) => a.sourceType === "GOAL" && a.dayOfWeek && a.startTime && a.endTime,
    );
    if (goalPlaced.length === 0) {
      setError("적용할 활동 추천이 없습니다. (Task는 미리보기만 가능)");
      return;
    }
    if (!window.confirm(
      `${fmtWeekRange(weekStart)} 주에 활동 추천 ${goalPlaced.length}건 적용합니다. (대상 활동의 기존 적용분만 교체) 계속할까요?`,
    )) return;
    setError("");
    setApplying(true);
    try {
      const items = goalPlaced.map((a) => ({
        goalId: a.goalId,
        dayOfWeek: a.dayOfWeek,
        startTime: a.startTime,
        endTime: a.endTime,
      }));
      await applyAllocations(items, weekStart);
      setAllocations([]);
      setMsg(`${goalPlaced.length}건 적용 완료`);
      loadAll();
      setTimeout(() => setMsg(""), 2500);
    } catch (err) {
      setError(err.response?.data?.message || "적용 실패");
    } finally {
      setApplying(false);
    }
  };

  const handleClearApplied = async () => {
    if (!window.confirm(`${fmtWeekRange(weekStart)} 주의 모든 적용분을 초기화합니다. 계속할까요?`)) return;
    setError("");
    try {
      await clearAppliedAllocationsForWeek(weekStart);
      setMsg("적용분 초기화 완료");
      loadAll();
      setTimeout(() => setMsg(""), 2000);
    } catch (err) {
      setError(err.response?.data?.message || "초기화 실패");
    }
  };

  const handleToggleCompleted = async (allocationId, completed) => {
    try {
      await toggleAllocationCompleted(allocationId, completed);
      // 즉시 로컬 state 업데이트(낙관적) + summary 재조회
      setAppliedAllocations((prev) =>
        prev.map((a) => (a.id === allocationId ? { ...a, isCompleted: completed } : a)),
      );
      const sRes = await getWeeklySummary(weekStart);
      setSummary(sRes.data.data || null);
    } catch (err) {
      setError(err.response?.data?.message || "체크 실패");
    }
  };

  const totalGoalHours = goals.reduce(
    (s, g) => s + (g.targetHoursPerWeek || 0),
    0,
  );
  const placedAllocations = allocations.filter(
    (a) => a.dayOfWeek && a.startTime && a.endTime,
  );
  const unplacedAllocations = allocations.filter(
    (a) => !a.dayOfWeek || !a.startTime || !a.endTime,
  );
  // FIXED goals → 그리드에 commit된 것처럼 렌더 (applied와 같은 스타일)
  const fixedRenders = goals
    .filter(
      (g) =>
        g.preferredMode === "FIXED" &&
        g.preferredDayOfWeek &&
        g.preferredStartTime &&
        g.preferredEndTime,
    )
    .map((g) => ({
      goalId: g.id,
      goalTitle: g.title,
      category: g.category,
      dayOfWeek: g.preferredDayOfWeek,
      startTime: g.preferredStartTime,
      endTime: g.preferredEndTime,
      isFixed: true,
    }));
  const allCommitted = [...appliedAllocations, ...fixedRenders];

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>시간 관리</h2>
      </div>

      {error && <p className="error">{error}</p>}
      {msg && <p className="success">{msg}</p>}

      <div className="card">
        <div className="form-row">
          <div className="field">
            <label>연도</label>
            <input
              type="number"
              value={year}
              onChange={(e) => setYear(parseInt(e.target.value) || year)}
            />
          </div>
          <div className="field">
            <label>학기</label>
            <select
              value={termSeason}
              onChange={(e) => setTermSeason(e.target.value)}
            >
              <option value="SPRING">1학기</option>
              <option value="SUMMER">여름학기</option>
              <option value="FALL">2학기</option>
              <option value="WINTER">겨울학기</option>
            </select>
          </div>
          <div className="field" style={{ minWidth: 220 }}>
            <label>주 선택</label>
            <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
              <button type="button" className="btn-secondary-sm" onClick={() => setWeekStart(shiftWeek(weekStart, -1))}>← 지난주</button>
              <button type="button" className="btn-secondary-sm" onClick={() => setWeekStart(mondayOf(new Date()))}>이번주</button>
              <button type="button" className="btn-secondary-sm" onClick={() => setWeekStart(shiftWeek(weekStart, 1))}>다음주 →</button>
            </div>
            <p style={{ margin: "2px 0 0", fontSize: "0.7rem", color: "var(--muted)" }}>
              {fmtWeekRange(weekStart)} (월~일)
            </p>
          </div>
          <div className="field">
            <label>활동 목표 합계</label>
            <input type="text" value={`주 ${totalGoalHours}시간`} readOnly />
          </div>
          <button
            type="button"
            className="btn-primary"
            onClick={handleRecommend}
            disabled={loading || goals.length === 0}
            style={{ alignSelf: "end" }}
          >
            {loading ? "추천 중..." : "AI 배치 추천 받기"}
          </button>
          {allocations.filter((a) => a.dayOfWeek).length > 0 && (
            <button
              type="button"
              className="btn-primary"
              onClick={handleApply}
              disabled={applying}
              style={{ alignSelf: "end", background: "#4fc97a" }}
            >
              {applying ? "적용 중..." : "✓ 이 추천 적용"}
            </button>
          )}
          {appliedAllocations.length > 0 && (
            <button
              type="button"
              className="btn-secondary"
              onClick={handleClearApplied}
              style={{ alignSelf: "end" }}
            >
              적용분 초기화 ({appliedAllocations.length})
            </button>
          )}
        </div>
        <p style={{ fontSize: "0.75rem", color: "var(--muted)", marginTop: 4 }}>
          ※ ChatGPT 연동 전 룰 기반 베이스라인. 요일 분산 + 활동 사이 15분 buffer + 빈 슬롯 매칭. 추천 결과는 "적용" 누를 때까지 휘발.
        </p>
      </div>

      {summary && summary.byGoal && summary.byGoal.length > 0 && (
        <div className="card" style={{ background: "var(--bg)" }}>
          <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 8 }}>
            <h3 style={{ margin: 0 }}>📊 {fmtWeekRange(weekStart)} 달성률</h3>
            <span style={{ fontSize: "0.85rem", color: "var(--muted)" }}>
              완료 {Math.round(summary.totalCompletedMinutes / 60 * 10) / 10}h /
              계획 {Math.round(summary.totalAppliedMinutes / 60 * 10) / 10}h
              ({summary.totalAppliedMinutes > 0
                ? Math.round(100 * summary.totalCompletedMinutes / summary.totalAppliedMinutes)
                : 0}%)
            </span>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 8 }}>
            {summary.byGoal.map((g) => (
              <div
                key={g.goalId}
                style={{
                  padding: 8,
                  borderLeft: `4px solid ${CATEGORY_COLORS[g.category] || "#888"}`,
                  background: "var(--card)",
                  borderRadius: 4,
                }}
              >
                <div style={{ fontSize: "0.78rem", fontWeight: 600 }}>{g.goalTitle}</div>
                <div style={{ fontSize: "0.72rem", color: "var(--muted)", margin: "2px 0" }}>
                  {Math.round(g.completedMinutes / 60 * 10) / 10}h / {Math.round(g.appliedMinutes / 60 * 10) / 10}h
                  · 목표 주 {g.targetHoursPerWeek}h
                </div>
                <div style={{ height: 6, background: "rgba(0,0,0,0.08)", borderRadius: 3, overflow: "hidden" }}>
                  <div style={{
                    width: `${g.completionPercent}%`,
                    height: "100%",
                    background: CATEGORY_COLORS[g.category] || "#888",
                    transition: "width 0.3s",
                  }} />
                </div>
                <div style={{ fontSize: "0.7rem", color: g.completionPercent === 100 ? "#4fc97a" : "var(--muted)", marginTop: 2 }}>
                  {g.completionPercent}% {g.completionPercent === 100 ? "✓ 완료" : ""}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="tm-split" style={{ display: "grid", gridTemplateColumns: "1fr 1.4fr", gap: 16 }}>
        {/* 좌: 자연어 입력 + 활동 리스트 */}
        <div className="card">
          <h3>활동 목표 추가</h3>
          <form onSubmit={handleNaturalLanguageAdd}>
            <div className="field">
              <label>한 줄로 적어주세요</label>
              <input
                type="text"
                value={nlInput}
                onChange={(e) => setNlInput(e.target.value)}
                placeholder="예: 운동 주 3시간 / 알고리즘 공부 4시간 / 독서 2시간"
                style={{ fontSize: "0.95rem" }}
                autoFocus
              />
              <p style={{ fontSize: "0.72rem", color: "var(--muted)", margin: "6px 0 0", lineHeight: 1.4 }}>
                💡 카테고리 키워드(운동/공부/식사/독서·취미/휴식)와 시간(N시간)을 인식합니다.<br />
                향후 ChatGPT 연동 후엔 "월수금 헬스 18시" 같은 복잡한 문장도 자동 파싱.
              </p>
            </div>
            <button type="submit" className="btn-primary" disabled={adding}>
              {adding ? "추가 중..." : "+ 추가"}
            </button>
          </form>

          <h4 style={{ marginTop: 20 }}>등록된 활동 ({goals.length})</h4>
          {goals.length === 0 && <p className="empty">아직 등록한 활동이 없습니다.</p>}
          {goals.map((g) => (
            <div
              key={g.id}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                padding: "8px 0",
                borderBottom: "1px solid var(--border)",
              }}
            >
              <span
                style={{
                  display: "inline-block",
                  width: 10,
                  height: 10,
                  borderRadius: "50%",
                  background: CATEGORY_COLORS[g.category] || "#888",
                }}
              />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600 }}>
                  {g.title}{" "}
                  <span style={{ fontSize: "0.75rem", color: "var(--muted)" }}>
                    · 주 {g.targetHoursPerWeek}시간 · {(CATEGORIES.find((c) => c.value === g.category) || {}).label || g.category}
                  </span>
                  {g.preferredMode === "FIXED" && (
                    <span style={{
                      marginLeft: 6,
                      fontSize: "0.65rem",
                      padding: "1px 6px",
                      background: "#a04fc9",
                      color: "#fff",
                      borderRadius: 8,
                    }}>고정</span>
                  )}
                </div>
              </div>
              <button className="btn-danger-sm" onClick={() => handleDelete(g.id)}>
                삭제
              </button>
            </div>
          ))}
        </div>

        {/* 우: 주간 그리드 + 추천 결과 */}
        <div className="card">
          <h3>주간 빈 시간 + 배치 추천</h3>

          {/* 학사 일정 D-Day 보드 */}
          {examEvents.length > 0 && (
            <div style={{
              padding: 12,
              background: "var(--bg)",
              border: "1px solid var(--border)",
              borderRadius: 6,
              marginBottom: 12,
            }}>
              <div style={{ fontWeight: 600, marginBottom: 8, fontSize: "0.88rem" }}>
                📅 학사 일정 D-Day
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 8 }}>
                {examEvents.slice(0, 8).map((e) => {
                  const isToday = e.daysLeft === 0;
                  const isPast = e.daysLeft < 0;
                  const dDayText = isToday ? "D-DAY" : isPast ? `D+${-e.daysLeft}` : `D-${e.daysLeft}`;
                  return (
                    <div
                      key={e.id}
                      style={{
                        padding: 8,
                        borderLeft: `4px solid ${e.color}`,
                        background: isPast ? "rgba(0,0,0,0.05)" : "var(--card)",
                        borderRadius: 4,
                        opacity: isPast ? 0.6 : 1,
                      }}
                    >
                      <div style={{ fontSize: "0.72rem", color: "var(--muted)", marginBottom: 2 }}>
                        {e.label}
                      </div>
                      <div style={{
                        fontSize: "1.05rem",
                        fontWeight: 700,
                        color: isPast ? "var(--muted)" : e.color,
                      }}>
                        {dDayText}
                      </div>
                      <div style={{ fontSize: "0.72rem", color: "var(--text)" }}>
                        {e.startDate}
                      </div>
                      <div style={{ fontSize: "0.68rem", color: "var(--muted)" }}>
                        {e.title}
                      </div>
                    </div>
                  );
                })}
              </div>
              <p style={{ margin: "8px 0 0", fontSize: "0.7rem", color: "var(--muted)" }}>
                ※ 시험 임박 시 "공부" 카테고리 ActivityGoal 추가하면 빈 시간에 자동 배치됩니다.
              </p>
            </div>
          )}

          <WeekGrid
            blanks={blanks}
            allocations={placedAllocations}
            lectures={lectures}
            applied={allCommitted}
          />

          {appliedAllocations.length > 0 && (
            <>
              <h4 style={{ marginTop: 16 }}>
                ✅ 적용된 배치 — 완료 체크
              </h4>
              {appliedAllocations.map((a) => (
                <div
                  key={a.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    padding: "6px 8px",
                    borderLeft: `3px solid ${CATEGORY_COLORS[a.category] || "#888"}`,
                    margin: "4px 0",
                    fontSize: "0.85rem",
                    background: a.isCompleted ? "rgba(79, 201, 122, 0.08)" : "transparent",
                  }}
                >
                  <input
                    type="checkbox"
                    checked={!!a.isCompleted}
                    onChange={(e) => handleToggleCompleted(a.id, e.target.checked)}
                    style={{ width: 18, height: 18, cursor: "pointer" }}
                  />
                  <div style={{ flex: 1, textDecoration: a.isCompleted ? "line-through" : "none" }}>
                    <strong>{a.goalTitle}</strong> — {DAY_LABELS[a.dayOfWeek]}{" "}
                    {a.startTime.slice(0, 5)}~{a.endTime.slice(0, 5)} · {a.durationMinutes}분
                  </div>
                </div>
              ))}
            </>
          )}

          {allocations.length > 0 && (
            <>
              <h4 style={{ marginTop: 16 }}>
                추천 미리보기 ({placedAllocations.length}건)
              </h4>
              {placedAllocations.map((a, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: "6px 8px",
                    borderLeft: `3px solid ${CATEGORY_COLORS[a.category] || "#888"}`,
                    margin: "4px 0",
                    fontSize: "0.85rem",
                    opacity: 0.85,
                  }}
                >
                  <strong>
                    {a.sourceType === "TASK" ? "📋 " : ""}{a.goalTitle}
                  </strong> — {DAY_LABELS[a.dayOfWeek]}{" "}
                  {a.startTime.slice(0, 5)}~{a.endTime.slice(0, 5)} · {a.durationMinutes}분
                  {a.sourceType === "TASK" && (
                    <span style={{ marginLeft: 4, fontSize: "0.7rem", color: "#f97c4f" }}>(일회성)</span>
                  )}
                  <div style={{ fontSize: "0.72rem", color: "var(--muted)" }}>
                    {a.reason}
                  </div>
                </div>
              ))}
              {unplacedAllocations.length > 0 && (
                <div
                  style={{
                    marginTop: 8,
                    padding: 8,
                    background: "rgba(249, 124, 79, 0.1)",
                    borderRadius: 4,
                    fontSize: "0.78rem",
                  }}
                >
                  ⚠ 일부 활동은 빈 시간 부족으로 못 채웠습니다:
                  <ul style={{ margin: "4px 0 0", paddingLeft: 16 }}>
                    {unplacedAllocations.map((a, idx) => (
                      <li key={idx}>
                        {a.sourceType === "TASK" ? "📋 " : ""}{a.goalTitle}: {a.reason}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function WeekGrid({ blanks, allocations, lectures, applied }) {
  const gridH = HOURS.length * ROW_H;

  return (
    <div style={{ overflowX: "auto", marginTop: 8 }}>
      <div style={{ display: "flex", paddingLeft: 40 }}>
        {DAYS.map((d) => (
          <div
            key={d}
            style={{
              width: COL_W,
              textAlign: "center",
              fontSize: "0.78rem",
              fontWeight: 700,
              color: d === "SAT" ? "#4f9cf9" : d === "SUN" ? "#f94f4f" : "var(--muted)",
            }}
          >
            {DAY_LABELS[d]}
          </div>
        ))}
      </div>
      <div
        style={{
          position: "relative",
          height: gridH,
          minWidth: 40 + DAYS.length * COL_W,
        }}
      >
        {/* hour lines */}
        {HOURS.map((h) => (
          <div
            key={h}
            style={{
              position: "absolute",
              top: (h - 9) * ROW_H,
              left: 0,
              right: 0,
              borderTop: "1px solid var(--border)",
            }}
          >
            <span
              style={{
                width: 38,
                display: "inline-block",
                fontSize: "0.65rem",
                color: "var(--muted)",
                paddingLeft: 2,
              }}
            >
              {h}시
            </span>
          </div>
        ))}

        {/* lectures — 사용자 시간표 강의 (회색 박스, 빈 슬롯·배치와 구분) */}
        {(lectures || []).flatMap((lec, lecIdx) =>
          (lec.schedules || []).map((s, si) => {
            const dayIdx = DAYS.indexOf(s.dayOfWeek);
            if (dayIdx < 0) return null;
            const [sh, sm] = s.startTime.split(":").map(Number);
            const [eh, em] = s.endTime.split(":").map(Number);
            const top = (sh - 9 + sm / 60) * ROW_H;
            const height = Math.max((eh - sh + (em - sm) / 60) * ROW_H, 16);
            if (top + height < 0 || top > gridH) return null;
            return (
              <div
                key={`lec-${lec.id}-${si}`}
                style={{
                  position: "absolute",
                  top,
                  left: 40 + dayIdx * COL_W,
                  width: COL_W - 2,
                  height,
                  background: "rgba(120, 120, 120, 0.75)",
                  borderRadius: 3,
                  padding: "1px 4px",
                  color: "#fff",
                  fontSize: "0.62rem",
                  overflow: "hidden",
                  border: "1px solid rgba(0, 0, 0, 0.2)",
                }}
                title={`${lec.courseName} · ${lec.professor || ""}`}
              >
                {lec.courseName}
              </div>
            );
          }),
        )}

        {/* blank slots — 흐린 회색 */}
        {blanks.map((b, idx) => {
          const dayIdx = DAYS.indexOf(b.dayOfWeek);
          if (dayIdx < 0) return null;
          const [sh, sm] = b.startTime.split(":").map(Number);
          const [eh, em] = b.endTime.split(":").map(Number);
          const top = (sh - 9 + sm / 60) * ROW_H;
          const height = (eh - sh + (em - sm) / 60) * ROW_H;
          if (top + height < 0 || top > gridH) return null;
          return (
            <div
              key={`b-${idx}`}
              style={{
                position: "absolute",
                top,
                left: 40 + dayIdx * COL_W,
                width: COL_W - 2,
                height,
                background: "rgba(120, 200, 120, 0.12)",
                border: "1px dashed rgba(120, 200, 120, 0.4)",
                borderRadius: 3,
              }}
            />
          );
        })}

        {/* applied — 진하게 (사용자가 commit한 시간) */}
        {(applied || []).map((a, idx) => {
          const dayIdx = DAYS.indexOf(a.dayOfWeek);
          if (dayIdx < 0) return null;
          const [sh, sm] = a.startTime.split(":").map(Number);
          const [eh, em] = a.endTime.split(":").map(Number);
          const top = (sh - 9 + sm / 60) * ROW_H;
          const height = Math.max((eh - sh + (em - sm) / 60) * ROW_H, 16);
          return (
            <div
              key={`ap-${idx}`}
              style={{
                position: "absolute",
                top,
                left: 40 + dayIdx * COL_W,
                width: COL_W - 2,
                height,
                background: CATEGORY_COLORS[a.category] || "#888",
                border: "2px solid rgba(0,0,0,0.5)",
                borderRadius: 3,
                padding: "1px 4px",
                color: "#fff",
                fontSize: "0.65rem",
                overflow: "hidden",
                fontWeight: 700,
              }}
              title={a.isFixed
                ? `🔒 ${a.goalTitle} (FIXED, 매주 고정)`
                : `✓ ${a.goalTitle} (적용됨${a.durationMinutes != null ? `, ${a.durationMinutes}분` : ""})`}
            >
              {a.isFixed ? "🔒" : "✓"} {a.goalTitle}
            </div>
          );
        })}

        {/* allocations — 추천 미리보기 (얇은 점선 테두리, 반투명) */}
        {allocations.map((a, idx) => {
          const dayIdx = DAYS.indexOf(a.dayOfWeek);
          if (dayIdx < 0) return null;
          const [sh, sm] = a.startTime.split(":").map(Number);
          const [eh, em] = a.endTime.split(":").map(Number);
          const top = (sh - 9 + sm / 60) * ROW_H;
          const height = Math.max((eh - sh + (em - sm) / 60) * ROW_H, 16);
          return (
            <div
              key={`a-${idx}`}
              style={{
                position: "absolute",
                top,
                left: 40 + dayIdx * COL_W,
                width: COL_W - 2,
                height,
                background: CATEGORY_COLORS[a.category] || "#888",
                opacity: 0.55,
                border: "1px dashed rgba(0,0,0,0.4)",
                borderRadius: 3,
                padding: "1px 4px",
                color: "#fff",
                fontSize: "0.65rem",
                overflow: "hidden",
              }}
              title={`${a.goalTitle} (추천 미리보기 ${a.durationMinutes}분)`}
            >
              {a.goalTitle}
            </div>
          );
        })}
      </div>
      <div
        style={{
          display: "flex",
          gap: 12,
          marginTop: 6,
          fontSize: "0.72rem",
          color: "var(--muted)",
          flexWrap: "wrap",
        }}
      >
        <span>⬜ 강의</span>
        <span>🟩 빈 슬롯</span>
        <span>✓ 적용된 배치</span>
        <span>🔒 FIXED 활동</span>
        <span style={{ opacity: 0.6 }}>┄ 추천 미리보기</span>
        {Object.entries(CATEGORY_COLORS).map(([cat, c]) => (
          <span key={cat}>
            <span
              style={{
                display: "inline-block",
                width: 10,
                height: 10,
                background: c,
                marginRight: 3,
                verticalAlign: "middle",
                borderRadius: 2,
              }}
            />
            {CATEGORIES.find((x) => x.value === cat)?.label}
          </span>
        ))}
      </div>
    </div>
  );
}
