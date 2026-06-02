import { useState, useEffect, useRef } from "react";
import {
  listRecommendations,
  createRecommendation,
  deleteRecommendation,
} from "../api/ai";
import { createTimetable, listTimetables, getTimetable, setPrimaryTimetable } from "../api/timetable";
import { getPreference, savePreference } from "../api/preference";
import { getDepts, listLectures } from "../api/academic";
import { getCurrentSemester } from "../utils/semester";

const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금" };
const MINI_DAYS = ["MON", "TUE", "WED", "THU", "FRI"];
const MINI_HOURS = Array.from({ length: 14 }, (_, i) => i + 9);
const MINI_CELL = 22;
const MINI_COL = 42;
const COLORS = [
  "#4f9cf9", "#f97c4f", "#4fc97a", "#c94fb8", "#f9c74f",
  "#a04fc9", "#4fc9c9", "#f94f4f", "#9cf94f", "#4fc9f9",
];

const PERSONA_COLORS = {
  LIGHT_WEEK: "#4fc97a",
  MAJOR_FOCUS: "#f97c4f",
  RELAXED: "#4f9cf9",
};

const MODES = {
  TIME_FREE: { label: "시간 무관", emoji: "⏱️", hint: "학점만 채우면 OK. 시간/요일 신경 안 씀." },
  TIME_SENSITIVE: { label: "시간 민감", emoji: "🕐", hint: "알바·아침수업 등 특정 시간대 회피." },
  DAY_SENSITIVE: { label: "요일 민감", emoji: "📅", hint: "공강일이 필요해요 (월공강/금공강 등)." },
};

function MiniGrid({ lectures }) {
  return (
    <div style={{ overflowX: "auto", margin: "10px 0" }}>
      <div style={{ display: "flex", paddingLeft: 36, marginBottom: 3 }}>
        {["월", "화", "수", "목", "금"].map((d) => (
          <div key={d} style={{ width: MINI_COL, textAlign: "center", fontSize: "0.72rem", fontWeight: 700, color: "var(--muted)" }}>{d}</div>
        ))}
      </div>
      <div style={{ position: "relative", height: `${MINI_HOURS.length * MINI_CELL}px`, minWidth: `${36 + MINI_DAYS.length * MINI_COL}px` }}>
        {MINI_HOURS.map((h) => (
          <div key={h} style={{ position: "absolute", top: `${(h - 9) * MINI_CELL}px`, left: 0, right: 0, borderTop: "1px solid var(--border)" }}>
            <span style={{ width: 34, display: "inline-block", fontSize: "0.6rem", color: "var(--muted)", paddingLeft: 2 }}>{h}시</span>
          </div>
        ))}
        {(lectures || []).map((lec, idx) =>
          (lec.schedules || []).map((s, si) => {
            const dayIdx = MINI_DAYS.indexOf(s.dayOfWeek);
            if (dayIdx < 0) return null;
            const [sh, sm] = s.startTime.split(":").map(Number);
            const [eh, em] = s.endTime.split(":").map(Number);
            const top = (sh - 9 + sm / 60) * MINI_CELL;
            const height = Math.max((eh - sh + (em - sm) / 60) * MINI_CELL, 14);
            if (top < 0 || top > MINI_HOURS.length * MINI_CELL) return null;
            return (
              <div key={`${lec.id}-${si}`} style={{ position: "absolute", top: `${top}px`, left: `${36 + dayIdx * MINI_COL}px`, width: `${MINI_COL - 2}px`, height: `${height}px`, backgroundColor: COLORS[idx % COLORS.length], borderRadius: 3, overflow: "hidden", padding: "1px 3px" }}>
                <div style={{ fontSize: "0.58rem", color: "#fff", lineHeight: 1.2, overflow: "hidden" }}>{lec.courseName}</div>
              </div>
            );
          }),
        )}
      </div>
    </div>
  );
}

const makeEmptyForm = () => ({
  year: getCurrentSemester().year,
  termSeason: getCurrentSemester().termSeason,
  dept: "",
  grade: null,
  targetMajorCredits: 9,
  targetGeneralCredits: 6,
  targetRemoteCredits: 0,
  // TIME_SENSITIVE
  excludeMorning: false,
  avoidRanges: [], // [{ dayOfWeek, startTime, endTime }]
  // DAY_SENSITIVE
  freeDays: [], // ['MON', 'FRI']
});

const emptyRefine = {
  extraAvoidRanges: [], // 추가 AVOID
  preferredLectureIds: [], // 듣고 싶은 강의 id 목록
};

export default function AiRecommendationPage() {
  const [recommendations, setRecommendations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [step, setStep] = useState("select"); // select | form | result
  const [mode, setMode] = useState(null);
  const [form, setForm] = useState(makeEmptyForm);
  const [refine, setRefine] = useState(emptyRefine);
  const [refineOpen, setRefineOpen] = useState(false);
  const [depts, setDepts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [saveMsg, setSaveMsg] = useState("");
  const [reasonsOpen, setReasonsOpen] = useState({});
  const [courseSearch, setCourseSearch] = useState("");
  const [courseHits, setCourseHits] = useState([]);
  const searchTimer = useRef(null);

  useEffect(() => {
    loadList();
  }, []);

  useEffect(() => {
    getDepts(form.year, form.termSeason)
      .then((res) => setDepts(res.data.data || []))
      .catch(() => setDepts([]));
  }, [form.year, form.termSeason]);

  const loadList = async () => {
    try {
      const res = await listRecommendations();
      const list = res.data.data || [];
      setRecommendations(list);
    } catch {
      setRecommendations([]);
    }
  };

  const resetAll = () => {
    setMode(null);
    setForm(makeEmptyForm());
    setRefine(emptyRefine);
    setRefineOpen(false);
    setSelected(null);
    setStep("select");
    setError("");
  };

  // 모드 선택 시 호출: 이전 preference가 있으면 학과·학년·학점 기본값으로 자동 채움
  const selectMode = async (key) => {
    setMode(key);
    setStep("form");
    setError("");
    try {
      const res = await getPreference(form.year, form.termSeason);
      const data = res.data?.data;
      if (data) {
        setForm((f) => ({
          ...f,
          dept: data.options?.dept || f.dept,
          grade: data.options?.grade ?? f.grade,
          targetMajorCredits: data.creditPolicy?.targetMajorCredits ?? f.targetMajorCredits,
          targetGeneralCredits: data.creditPolicy?.targetGeneralCredits ?? f.targetGeneralCredits,
        }));
      }
    } catch {
      // 이전 preference 없으면 기본값 유지
    }
  };

  const collectOccupiedLectureIds = async (year, termSeason) => {
    try {
      const ttRes = await listTimetables();
      const mine = (ttRes.data.data || []).filter((t) => t.year === year && t.termSeason === termSeason);
      const ids = new Set();
      for (const t of mine) {
        if (Array.isArray(t.lectures)) {
          t.lectures.forEach((l) => ids.add(l.id));
        } else {
          try {
            const detail = await getTimetable(t.id);
            (detail.data.data?.lectures || []).forEach((l) => ids.add(l.id));
          } catch { /* ignore */ }
        }
      }
      return Array.from(ids);
    } catch {
      return [];
    }
  };

  // 모드 + form + refine → 백엔드 preference 구조로 변환
  const buildPreferencePayload = (extraTimeRanges = [], extraDesiredCourseIds = []) => {
    const timeRanges = [];
    if (mode === "TIME_SENSITIVE") {
      form.avoidRanges.forEach((r) => {
        if (r.startTime && r.endTime) {
          timeRanges.push({ type: "AVOID", dayOfWeek: r.dayOfWeek || null, startTime: r.startTime, endTime: r.endTime });
        }
      });
    }
    if (mode === "DAY_SENSITIVE") {
      form.freeDays.forEach((day) => {
        timeRanges.push({ type: "AVOID", dayOfWeek: day, startTime: "09:00", endTime: "22:00" });
      });
    }
    extraTimeRanges.forEach((r) => {
      if (r.startTime && r.endTime) {
        timeRanges.push({ type: "AVOID", dayOfWeek: r.dayOfWeek || null, startTime: r.startTime, endTime: r.endTime });
      }
    });

    const targetCredits = (form.targetMajorCredits || 0)
        + (form.targetGeneralCredits || 0)
        + (form.targetRemoteCredits || 0);

    return {
      year: form.year,
      termSeason: form.termSeason,
      creditPolicy: {
        minCredits: Math.max(12, targetCredits - 3),
        maxCredits: Math.min(24, targetCredits + 3),
        targetCredits: targetCredits || 18,
        targetMajorCredits: form.targetMajorCredits || 0,
        targetGeneralCredits: form.targetGeneralCredits || 0,
        targetRemoteCredits: form.targetRemoteCredits || 0,
      },
      options: {
        excludeMorning: mode === "TIME_SENSITIVE" ? !!form.excludeMorning : false,
        maxDaysPerWeek: 5,
        allowGapsMinutes: 0,
        dept: form.dept || null,
        preferMajorOnly: false,
        grade: form.grade || null,
      },
      timeRanges,
      desiredCourses: extraDesiredCourseIds.map((courseId) => ({ courseId, priority: 1 })),
    };
  };

  const runRecommendation = async (extraTimeRanges = [], extraPreferredLectureIds = [], extraDesiredCourseIds = []) => {
    setLoading(true);
    setError("");
    try {
      const payload = buildPreferencePayload(extraTimeRanges, extraDesiredCourseIds);
      await savePreference(payload);

      const occupiedLectureIds = await collectOccupiedLectureIds(form.year, form.termSeason);
      const res = await createRecommendation({
        year: form.year,
        termSeason: form.termSeason,
        occupiedLectureIds,
        preferredLectureIds: extraPreferredLectureIds,
      });
      const rec = res.data.data;
      setRecommendations((prev) => [rec, ...prev]);
      setSelected(rec);
      setStep("result");
      setRefineOpen(false);
    } catch (err) {
      setError(err.response?.data?.message || "추천 생성 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = (e) => {
    e?.preventDefault();
    if (!form.dept) {
      setError("학과를 선택해주세요.");
      return;
    }
    // 폼에서 미리 고른 '듣고 싶은 강의'를 첫 추천부터 반영
    runRecommendation([], refine.preferredLectureIds, []);
  };

  const togglePreferred = (id) => {
    setRefine((prev) => ({
      ...prev,
      preferredLectureIds: prev.preferredLectureIds.includes(id)
        ? prev.preferredLectureIds.filter((x) => x !== id)
        : [...prev.preferredLectureIds, id],
    }));
  };

  const handleRefineSubmit = (e) => {
    e?.preventDefault();
    runRecommendation(refine.extraAvoidRanges, refine.preferredLectureIds, []);
  };

  const searchCourses = async (keyword) => {
    if (!keyword || keyword.trim().length < 2) {
      setCourseHits([]);
      return;
    }
    try {
      const res = await listLectures({ universityId: 1, year: form.year, termSeason: form.termSeason, keyword });
      setCourseHits((res.data.data || []).slice(0, 20));
    } catch {
      setCourseHits([]);
    }
  };

  // 300ms 디바운스 — 타이핑 중 매 키마다 API 호출하지 않도록
  const handleCourseSearchChange = (e) => {
    const v = e.target.value;
    setCourseSearch(v);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => searchCourses(v), 300);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await deleteRecommendation(id);
      const next = recommendations.filter((r) => r.id !== id);
      setRecommendations(next);
      if (selected?.id === id) {
        setSelected(next[0] || null);
        if (!next[0]) setStep("select");
      }
    } catch {
      setError("삭제 실패");
    }
  };

  const handleSaveAsUserTimetable = async (candidate, rec) => {
    setSaveMsg("");
    try {
      const res = await createTimetable({
        year: rec.year,
        termSeason: rec.termSeason,
        title: `AI 추천 ${rec.year} ${rec.termSeason} #${candidate.rank}`,
        lectureIds: candidate.lectures.map((l) => l.id),
        sourceRecommendationId: rec.id,
      });
      // 저장과 동시에 해당 학기 '대표(내 학교)' 시간표로 바로 적용
      const created = res.data?.data;
      if (created?.id) {
        await setPrimaryTimetable(created.id);
      }
      setSaveMsg("대표 시간표로 적용되었습니다! 시간표 메뉴에서 확인하세요.");
      setTimeout(() => setSaveMsg(""), 4000);
    } catch (err) {
      setError(err.response?.data?.message || "저장 실패");
    }
  };

  // ─────────── 렌더 ───────────
  return (
    <div className="page-container">
      <div className="page-header">
        <h2>AI 시간표 추천</h2>
        {step !== "select" && (
          <button className="btn-secondary" onClick={resetAll}>← 처음부터</button>
        )}
      </div>

      {error && <p className="error">{error}</p>}
      {saveMsg && <p className="success">{saveMsg}</p>}

      {/* STEP 1: 부류 선택 */}
      {step === "select" && (
        <div className="card">
          <h3>어떤 시간표가 필요해요?</h3>
          <p style={{ fontSize: "0.85rem", color: "var(--muted)", marginBottom: 16 }}>
            한 가지 선택하면 그에 맞는 정보만 입력하면 됩니다.
          </p>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 12 }}>
            {Object.entries(MODES).map(([key, m]) => (
              <button
                key={key}
                type="button"
                onClick={() => selectMode(key)}
                style={{
                  background: "var(--card)",
                  border: "1px solid var(--border)",
                  borderRadius: 8,
                  padding: 16,
                  textAlign: "left",
                  cursor: "pointer",
                }}
              >
                <div style={{ fontSize: "1.6rem", marginBottom: 6 }}>{m.emoji}</div>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>{m.label}</div>
                <div style={{ fontSize: "0.78rem", color: "var(--muted)" }}>{m.hint}</div>
              </button>
            ))}
          </div>

          {/* 메인에서 미리 강의 검색·둘러보기 */}
          <div style={{ marginTop: 20, padding: 14, background: "var(--bg)", borderRadius: 8 }}>
            <h4 style={{ margin: "0 0 4px" }}>🔍 강의 미리 검색하기</h4>
            <p style={{ fontSize: "0.78rem", color: "var(--muted)", margin: "0 0 8px" }}>
              어떤 강의가 있는지 먼저 둘러보세요. 듣고 싶은 강의를 골라두면 위에서 부류를 선택해 추천받을 때 우선 반영됩니다.
            </p>
            <div style={{ display: "flex", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
              <input
                type="number"
                value={form.year}
                onChange={(e) => setForm({ ...form, year: parseInt(e.target.value) || form.year })}
                style={{ width: 90 }}
                aria-label="연도"
              />
              <select value={form.termSeason} onChange={(e) => setForm({ ...form, termSeason: e.target.value })} style={{ width: 110 }} aria-label="학기">
                <option value="SPRING">1학기</option>
                <option value="SUMMER">여름학기</option>
                <option value="FALL">2학기</option>
                <option value="WINTER">겨울학기</option>
              </select>
              <input
                type="text"
                value={courseSearch}
                onChange={handleCourseSearchChange}
                placeholder="과목명 또는 교수명 (2자 이상)"
                style={{ flex: 1, minWidth: 180 }}
              />
            </div>
            {courseHits.length > 0 && (
              <div style={{ maxHeight: 220, overflowY: "auto", border: "1px solid var(--border)", borderRadius: 4 }}>
                {courseHits.map((l) => {
                  const picked = refine.preferredLectureIds.includes(l.id);
                  return (
                    <div key={l.id} style={{ padding: "4px 8px", display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--border)" }}>
                      <span style={{ fontSize: "0.82rem" }}>
                        <strong>{l.courseName}</strong> · {l.professor} · {l.credits}학점
                        {l.category && (
                          <span style={{ fontSize: "0.7rem", color: "var(--muted)", marginLeft: 6 }}>[{l.category}]</span>
                        )}
                      </span>
                      <button
                        type="button"
                        className={picked ? "btn-secondary-sm" : "btn-primary-sm"}
                        onClick={() => togglePreferred(l.id)}
                      >
                        {picked ? "해제" : "추가"}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
            {refine.preferredLectureIds.length > 0 && (
              <div style={{ fontSize: "0.78rem", color: "var(--muted)", marginTop: 6 }}>
                듣고 싶은 강의 {refine.preferredLectureIds.length}개 선택됨 — 부류를 선택하면 추천에 반영됩니다.
              </div>
            )}
          </div>

        </div>
      )}

      {/* STEP 2: 폼 입력 */}
      {step === "form" && mode && (
        <div className="card">
          <h3>{MODES[mode].emoji} {MODES[mode].label} — 정보 입력</h3>

          <form onSubmit={handleGenerate}>
            <div className="form-row">
              <div className="field">
                <label>연도</label>
                <input type="number" value={form.year} onChange={(e) => setForm({ ...form, year: parseInt(e.target.value) || form.year })} />
              </div>
              <div className="field">
                <label>학기</label>
                <select value={form.termSeason} onChange={(e) => setForm({ ...form, termSeason: e.target.value })}>
                  <option value="SPRING">1학기</option>
                  <option value="SUMMER">여름학기</option>
                  <option value="FALL">2학기</option>
                  <option value="WINTER">겨울학기</option>
                </select>
              </div>
              <div className="field">
                <label>학과</label>
                <select value={form.dept} onChange={(e) => setForm({ ...form, dept: e.target.value })} required>
                  <option value="">선택하세요</option>
                  {depts.map((d) => <option key={d} value={d}>{d}</option>)}
                </select>
              </div>
              <div className="field">
                <label>학년</label>
                <select
                  value={form.grade ?? ""}
                  onChange={(e) => setForm({ ...form, grade: e.target.value ? parseInt(e.target.value) : null })}
                >
                  <option value="">전학년</option>
                  {[1, 2, 3, 4].map((g) => <option key={g} value={g}>{g}학년</option>)}
                </select>
              </div>
            </div>

            {form.grade === 1 && (
              <div style={{
                padding: 12,
                background: "rgba(249, 199, 79, 0.15)",
                border: "1px solid rgba(249, 199, 79, 0.5)",
                borderRadius: 6,
                marginBottom: 12,
                fontSize: "0.82rem",
                lineHeight: 1.5,
              }}>
                💡 <strong>1학년 안내</strong> — 학교에서 자동 배정한 필수과목(채플 · 대학생활과 진로설계 · 커뮤니티잉글리쉬 · 기독교의 이해 · 교양세미나 등)을
                먼저 <a href="/timetable" style={{ color: "var(--primary)", fontWeight: 600 }}>시간표 페이지</a>에서 등록해주세요.
                등록된 강의 시간대는 자동으로 회피해 추가 강의(전공·교양)를 추천합니다.
              </div>
            )}

            <div className="form-row">
              <div className="field">
                <label>전공 학점 목표</label>
                <input type="number" value={form.targetMajorCredits} min={0} max={21}
                  onChange={(e) => setForm({ ...form, targetMajorCredits: parseInt(e.target.value) || 0 })} />
              </div>
              <div className="field">
                <label>교양 학점 목표</label>
                <input type="number" value={form.targetGeneralCredits} min={0} max={21}
                  onChange={(e) => setForm({ ...form, targetGeneralCredits: parseInt(e.target.value) || 0 })} />
              </div>
              <div className="field">
                <label>
                  원격 학점{" "}
                  <span style={{ fontSize: "0.7rem", color: "var(--muted)" }}>(선택)</span>
                </label>
                <input type="number" value={form.targetRemoteCredits} min={0} max={15}
                  onChange={(e) => setForm({ ...form, targetRemoteCredits: parseInt(e.target.value) || 0 })} />
              </div>
              <div className="field">
                <label>합계</label>
                <input type="text"
                  value={`${(form.targetMajorCredits || 0) + (form.targetGeneralCredits || 0) + (form.targetRemoteCredits || 0)} 학점`}
                  readOnly />
              </div>
            </div>
            <p style={{ fontSize: "0.7rem", color: "var(--muted)", margin: "-8px 0 12px 4px" }}>
              ※ <strong>교양 학점</strong>은 오프라인 교양만. <strong>원격 학점</strong>은 별도로 입력해야 원격 강의가 추천됩니다 (0이면 미추천).
            </p>

            {/* 부류별 추가 필드 */}
            {mode === "TIME_SENSITIVE" && (
              <div style={{ marginTop: 16, padding: 12, background: "var(--bg)", borderRadius: 6 }}>
                <h4 style={{ marginBottom: 8 }}>🕐 시간 제약</h4>
                <label style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 10 }}>
                  <input type="checkbox" checked={form.excludeMorning}
                    onChange={(e) => setForm({ ...form, excludeMorning: e.target.checked })} />
                  10시 이전 강의 회피
                </label>
                <AvoidRangeEditor
                  ranges={form.avoidRanges}
                  onChange={(next) => setForm({ ...form, avoidRanges: next })}
                  label="알바·일정 시간대 (회피)"
                />
              </div>
            )}

            {mode === "DAY_SENSITIVE" && (
              <div style={{ marginTop: 16, padding: 12, background: "var(--bg)", borderRadius: 6 }}>
                <h4 style={{ marginBottom: 8 }}>📅 공강 원하는 요일</h4>
                <div style={{ display: "flex", gap: 8 }}>
                  {MINI_DAYS.map((d) => (
                    <label key={d} style={{ display: "flex", alignItems: "center", gap: 4 }}>
                      <input
                        type="checkbox"
                        checked={form.freeDays.includes(d)}
                        onChange={(e) => {
                          const next = e.target.checked
                            ? [...form.freeDays, d]
                            : form.freeDays.filter((x) => x !== d);
                          setForm({ ...form, freeDays: next });
                        }}
                      />
                      {DAY_LABELS[d]}
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* 강의 검색 흡수: 듣고 싶은 강의 먼저 고르기 */}
            <div style={{ marginTop: 16, padding: 12, background: "var(--bg)", borderRadius: 6 }}>
              <h4 style={{ marginBottom: 4 }}>
                🔍 듣고 싶은 강의 먼저 고르기{" "}
                <span style={{ fontWeight: 400, fontSize: "0.75rem", color: "var(--muted)" }}>(선택)</span>
              </h4>
              <p style={{ fontSize: "0.75rem", color: "var(--muted)", margin: "0 0 8px" }}>
                꼭 듣고 싶은 과목·교수를 검색해 추가하면 추천에 우선 반영됩니다. "뭐 들을까" 고민될 때 둘러보세요.
              </p>
              <input
                type="text"
                value={courseSearch}
                onChange={handleCourseSearchChange}
                placeholder="과목명 또는 교수명 (2자 이상)"
              />
              {courseHits.length > 0 && (
                <div style={{ maxHeight: 200, overflowY: "auto", border: "1px solid var(--border)", marginTop: 6, borderRadius: 4 }}>
                  {courseHits.map((l) => {
                    const picked = refine.preferredLectureIds.includes(l.id);
                    return (
                      <div key={l.id} style={{ padding: "4px 8px", display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--border)" }}>
                        <span style={{ fontSize: "0.82rem" }}>
                          <strong>{l.courseName}</strong> · {l.professor} · {l.credits}학점
                          {l.category && (
                            <span style={{ fontSize: "0.7rem", color: "var(--muted)", marginLeft: 6 }}>[{l.category}]</span>
                          )}
                        </span>
                        <button
                          type="button"
                          className={picked ? "btn-secondary-sm" : "btn-primary-sm"}
                          onClick={() => togglePreferred(l.id)}
                        >
                          {picked ? "해제" : "추가"}
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
              {refine.preferredLectureIds.length > 0 && (
                <div style={{ fontSize: "0.78rem", color: "var(--muted)", marginTop: 6 }}>
                  듣고 싶은 강의 {refine.preferredLectureIds.length}개 선택됨
                </div>
              )}
            </div>

            <div style={{ marginTop: 16 }}>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? "추천 생성 중..." : "AI 추천 받기"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* STEP 3: 결과 */}
      {step === "result" && selected && (() => {
        // 본인 학과 전공 학점 검증 — 모든 페르소나의 최대치 확인
        const target = form.targetMajorCredits || 0;
        const maxOwnDeptMajor = Math.max(0, ...(selected.candidates || []).map((cand) =>
          (cand.lectures || [])
            .filter((l) => (l.category || "").includes("전공") && l.dept === form.dept)
            .reduce((s, l) => s + (l.credits || 0), 0),
        ));
        const shortBy = target - maxOwnDeptMajor;
        const showShortageWarning = target > 0 && !!form.dept && shortBy > 0;

        return (
        <>
          {showShortageWarning && (
            <div className="card" style={{
              background: "rgba(249, 124, 79, 0.12)",
              border: "1px solid rgba(249, 124, 79, 0.5)",
            }}>
              <h3 style={{ margin: 0, fontSize: "1rem" }}>⚠ 전공 목표 미달</h3>
              <p style={{ margin: "6px 0", fontSize: "0.88rem" }}>
                <strong>{form.dept} {form.grade ? `${form.grade}학년 ` : ""}전공</strong>으로
                목표 <strong>{target}학점</strong>을 채울 수 없습니다.
                가용 최대 <strong>{maxOwnDeptMajor}학점</strong> ·
                미달 <strong>{shortBy}학점</strong>은 교양으로 대체되었습니다.
              </p>
              <p style={{ margin: "6px 0", fontSize: "0.82rem", color: "var(--muted)" }}>
                전공 목표를 낮추거나, 학년·학과 선택을 바꿔서 다시 시도해보세요.
              </p>
              <button className="btn-primary-sm" onClick={() => setStep("form")}>
                ← 다시 설정하기
              </button>
            </div>
          )}
          <div className="card">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h3>{selected.year} {selected.termSeason} 추천 결과</h3>
              <div>
                <button className="btn-secondary" onClick={() => setRefineOpen((v) => !v)}>
                  🔧 {refineOpen ? "닫기" : "더 세밀하게 다시 받기"}
                </button>
              </div>
            </div>

            {mode && (
              <p style={{ fontSize: "0.78rem", color: "var(--muted)", marginTop: 4, marginBottom: 0 }}>
                조건: {MODES[mode].label}
                {form.dept && ` · ${form.dept}`}
                {` · ${form.grade ? `${form.grade}학년` : "전학년"}`}
                {` · 전공 ${form.targetMajorCredits} + 교양 ${form.targetGeneralCredits}학점`}
                {form.excludeMorning && " · 10시 전 회피"}
                {form.freeDays.length > 0 && ` · 공강 ${form.freeDays.map((d) => DAY_LABELS[d]).join("/")}`}
                {form.avoidRanges.length > 0 && ` · 회피 시간대 ${form.avoidRanges.length}개`}
                {refine.extraAvoidRanges.length > 0 && ` · 추가 회피 ${refine.extraAvoidRanges.length}개`}
                {refine.preferredLectureIds.length > 0 && ` · 선호 강의 ${refine.preferredLectureIds.length}개`}
              </p>
            )}

            {refineOpen && (
              <form onSubmit={handleRefineSubmit} style={{ marginTop: 12, padding: 12, background: "var(--bg)", borderRadius: 6 }}>
                <AvoidRangeEditor
                  ranges={refine.extraAvoidRanges}
                  onChange={(next) => setRefine({ ...refine, extraAvoidRanges: next })}
                  label="추가 회피 시간대"
                />
                <div style={{ marginTop: 10 }}>
                  <label>듣고 싶은 과목·교수 검색</label>
                  <input
                    type="text"
                    value={courseSearch}
                    onChange={handleCourseSearchChange}
                    placeholder="과목명 또는 교수명 (2자 이상)"
                  />
                  {courseHits.length > 0 && (
                    <div style={{ maxHeight: 180, overflowY: "auto", border: "1px solid var(--border)", marginTop: 4, borderRadius: 4 }}>
                      {courseHits.map((l) => {
                        const picked = refine.preferredLectureIds.includes(l.id);
                        return (
                          <div key={l.id} style={{ padding: "4px 8px", display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--border)" }}>
                            <span style={{ fontSize: "0.82rem" }}>
                              <strong>{l.courseName}</strong> · {l.professor} · {l.credits}학점
                            </span>
                            <button
                              type="button"
                              className={picked ? "btn-secondary-sm" : "btn-primary-sm"}
                              onClick={() => togglePreferred(l.id)}
                            >
                              {picked ? "해제" : "추가"}
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  )}
                  {refine.preferredLectureIds.length > 0 && (
                    <div style={{ fontSize: "0.78rem", color: "var(--muted)", marginTop: 4 }}>
                      선호 강의 {refine.preferredLectureIds.length}개 선택됨
                    </div>
                  )}
                </div>
                <div style={{ marginTop: 10 }}>
                  <button type="submit" className="btn-primary" disabled={loading}>
                    {loading ? "다시 추천 중..." : "이 조건으로 다시 추천"}
                  </button>
                </div>
              </form>
            )}
          </div>

          {(selected.candidates || []).map((cand) => {
            const personaColor = PERSONA_COLORS[cand.persona] || "var(--muted)";
            const isOpen = !!reasonsOpen[cand.id];
            return (
              <div key={cand.id} className="candidate-card">
                <div className="candidate-header">
                  <span className="rank-badge">후보 {cand.rank}</span>
                  {cand.personaLabel && (
                    <span style={{ background: personaColor, color: "#fff", padding: "2px 8px", borderRadius: 4, fontSize: "0.75rem", fontWeight: 600 }}>
                      {cand.personaLabel}
                    </span>
                  )}
                  <span className="credits-badge">{cand.totalCredits}학점</span>
                  {typeof cand.score === "number" && (
                    <span style={{ fontSize: "0.75rem", color: "var(--muted)" }} title="시간표 효용 점수">
                      점수 {cand.score.toFixed(2)}
                    </span>
                  )}
                  <button className="btn-secondary-sm" onClick={() => handleSaveAsUserTimetable(cand, selected)}>시간표로 저장</button>
                </div>

                {cand.reasons && cand.reasons.length > 0 && (
                  <div style={{ margin: "6px 0" }}>
                    <button
                      type="button"
                      onClick={() => setReasonsOpen((prev) => ({ ...prev, [cand.id]: !prev[cand.id] }))}
                      style={{ background: "transparent", border: "1px solid var(--border)", borderRadius: 4, padding: "2px 8px", fontSize: "0.75rem", cursor: "pointer", color: "var(--text)" }}
                    >
                      {isOpen ? "▾" : "▸"} 왜 이 시간표인가요? ({cand.reasons.length})
                    </button>
                    {isOpen && (
                      <ul style={{ margin: "6px 0 0", paddingLeft: 20, fontSize: "0.78rem", color: "var(--text)" }}>
                        {cand.reasons.map((r, idx) => <li key={idx}>{r}</li>)}
                      </ul>
                    )}
                  </div>
                )}

                <MiniGrid lectures={cand.lectures} />
                <LectureGroupList lectures={cand.lectures || []} />
              </div>
            );
          })}
        </>
        );
      })()}
    </div>
  );
}

function LectureGroupList({ lectures }) {
  // category 기준 그룹화: 전공(전공 포함) / 교양(교양 포함) / 기타
  const groups = { 전공: [], 교양: [], 기타: [] };
  lectures.forEach((l) => {
    const cat = l.category || "";
    if (cat.includes("전공")) groups["전공"].push(l);
    else if (cat.includes("교양")) groups["교양"].push(l);
    else groups["기타"].push(l);
  });

  const renderGroup = (name, list, color) => {
    if (list.length === 0) return null;
    const total = list.reduce((s, l) => s + (l.credits || 0), 0);
    return (
      <div style={{ marginTop: 8 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 4 }}>
          <span
            style={{
              fontSize: "0.72rem",
              fontWeight: 700,
              color: "#fff",
              background: color,
              padding: "1px 8px",
              borderRadius: 10,
            }}
          >
            {name}
          </span>
          <span style={{ fontSize: "0.72rem", color: "var(--muted)" }}>
            {list.length}과목 · {total}학점
          </span>
        </div>
        <div style={{ paddingLeft: 8 }}>
          {list.map((l, idx) => (
            <div key={l.id} style={{ fontSize: "0.82rem", color: "var(--text)" }}>
              <span style={{ color: "var(--muted)", marginRight: 4 }}>{idx + 1}.</span>
              {l.courseName}{" "}
              <strong>{l.credits}학점</strong>
              {l.professor ? <span style={{ color: "var(--muted)" }}> · {l.professor}</span> : null}
              {l.category && (
                <span style={{ fontSize: "0.7rem", color: "var(--muted)", marginLeft: 6 }}>
                  [{l.category}]
                </span>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div style={{ marginTop: 4 }}>
      {renderGroup("전공", groups["전공"], "#f97c4f")}
      {renderGroup("교양", groups["교양"], "#4f9cf9")}
      {renderGroup("기타", groups["기타"], "#888")}
    </div>
  );
}

function AvoidRangeEditor({ ranges, onChange, label }) {
  const add = () => onChange([...ranges, { dayOfWeek: "MON", startTime: "12:00", endTime: "14:00" }]);
  const update = (idx, patch) => onChange(ranges.map((r, i) => i === idx ? { ...r, ...patch } : r));
  const remove = (idx) => onChange(ranges.filter((_, i) => i !== idx));

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
        <label style={{ margin: 0 }}>{label}</label>
        <button type="button" className="btn-secondary-sm" onClick={add}>+ 추가</button>
      </div>
      {ranges.length === 0 && (
        <p style={{ fontSize: "0.75rem", color: "var(--muted)", margin: 0 }}>없음</p>
      )}
      {ranges.map((r, idx) => (
        <div key={idx} style={{ display: "flex", gap: 6, alignItems: "center", marginBottom: 4 }}>
          <select value={r.dayOfWeek} onChange={(e) => update(idx, { dayOfWeek: e.target.value })}>
            {["MON", "TUE", "WED", "THU", "FRI"].map((d) => <option key={d} value={d}>{DAY_LABELS[d]}</option>)}
          </select>
          <input type="time" value={r.startTime} onChange={(e) => update(idx, { startTime: e.target.value })} />
          <span>~</span>
          <input type="time" value={r.endTime} onChange={(e) => update(idx, { endTime: e.target.value })} />
          <button type="button" className="btn-danger-sm" onClick={() => remove(idx)}>✕</button>
        </div>
      ))}
    </div>
  );
}
