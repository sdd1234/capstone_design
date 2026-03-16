import { useState, useEffect } from "react";
import {
  listRecommendations,
  createRecommendation,
  deleteRecommendation,
} from "../api/ai";
import { createTimetable } from "../api/timetable";

const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금" };
const MINI_DAYS = ["MON", "TUE", "WED", "THU", "FRI"];
const MINI_HOURS = Array.from({ length: 10 }, (_, i) => i + 9);
const MINI_CELL = 22;
const MINI_COL = 42;
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

function MiniGrid({ lectures }) {
  return (
    <div style={{ overflowX: "auto", margin: "10px 0" }}>
      <div
        style={{
          display: "flex",
          paddingLeft: 36,
          marginBottom: 3,
        }}
      >
        {["월", "화", "수", "목", "금"].map((d) => (
          <div
            key={d}
            style={{
              width: MINI_COL,
              textAlign: "center",
              fontSize: "0.72rem",
              fontWeight: 700,
              color: "var(--muted)",
            }}
          >
            {d}
          </div>
        ))}
      </div>
      <div
        style={{
          position: "relative",
          height: `${MINI_HOURS.length * MINI_CELL}px`,
          minWidth: `${36 + MINI_DAYS.length * MINI_COL}px`,
        }}
      >
        {MINI_HOURS.map((h) => (
          <div
            key={h}
            style={{
              position: "absolute",
              top: `${(h - 9) * MINI_CELL}px`,
              left: 0,
              right: 0,
              borderTop: "1px solid var(--border)",
            }}
          >
            <span
              style={{
                width: 34,
                display: "inline-block",
                fontSize: "0.6rem",
                color: "var(--muted)",
                paddingLeft: 2,
              }}
            >
              {h}시
            </span>
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
            if (top < 0 || top >= MINI_HOURS.length * MINI_CELL) return null;
            return (
              <div
                key={`${lec.id}-${si}`}
                style={{
                  position: "absolute",
                  top: `${top}px`,
                  left: `${36 + dayIdx * MINI_COL}px`,
                  width: `${MINI_COL - 2}px`,
                  height: `${height}px`,
                  backgroundColor: COLORS[idx % COLORS.length],
                  borderRadius: 3,
                  overflow: "hidden",
                  padding: "1px 3px",
                }}
              >
                <div
                  style={{
                    fontSize: "0.58rem",
                    color: "#fff",
                    lineHeight: 1.2,
                    overflow: "hidden",
                  }}
                >
                  {lec.courseName}
                </div>
              </div>
            );
          }),
        )}
      </div>
    </div>
  );
}

export default function AiRecommendationPage() {
  const [recommendations, setRecommendations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState({ year: 2026, termSeason: "SPRING" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [saveMsg, setSaveMsg] = useState("");

  useEffect(() => {
    loadList();
  }, []);

  const loadList = async () => {
    try {
      const res = await listRecommendations();
      const list = res.data.data || [];
      setRecommendations(list);
      if (list.length > 0 && !selected) setSelected(list[0]);
    } catch {
      setRecommendations([]);
    }
  };

  const handleGenerate = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await createRecommendation(form);
      const rec = res.data.data;
      setRecommendations((prev) => [rec, ...prev]);
      setSelected(rec);
    } catch (err) {
      setError(err.response?.data?.message || "AI 추천 생성 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await deleteRecommendation(id);
      const next = recommendations.filter((r) => r.id !== id);
      setRecommendations(next);
      setSelected(next.length > 0 ? next[0] : null);
    } catch {
      setError("삭제 실패");
    }
  };

  const handleSaveAsUserTimetable = async (candidate, rec) => {
    setSaveMsg("");
    try {
      await createTimetable({
        year: rec.year,
        termSeason: rec.termSeason,
        title: `AI 추천 ${rec.year} ${rec.termSeason} #${candidate.rank}`,
        lectureIds: candidate.lectures.map((l) => l.id),
        sourceRecommendationId: rec.id,
      });
      setSaveMsg("시간표로 저장되었습니다!");
      setTimeout(() => setSaveMsg(""), 3000);
    } catch (err) {
      setError(err.response?.data?.message || "저장 실패");
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>AI 시간표 추천</h2>
      </div>

      {error && <p className="error">{error}</p>}
      {saveMsg && <p className="success">{saveMsg}</p>}

      <div className="card">
        <h3>새 추천 생성</h3>
        <form onSubmit={handleGenerate} className="form-row">
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
              onChange={(e) => setForm({ ...form, termSeason: e.target.value })}
            >
              <option value="SPRING">1학기</option>
              <option value="SUMMER">여름학기</option>
              <option value="FALL">2학기</option>
              <option value="WINTER">겨울학기</option>
            </select>
          </div>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "생성 중..." : "AI 추천 받기"}
          </button>
        </form>
      </div>

      <div className="timetable-layout">
        {/* Recommendation list */}
        <div className="timetable-list">
          {recommendations.map((rec) => (
            <div
              key={rec.id}
              className={`timetable-item ${selected?.id === rec.id ? "active" : ""}`}
              onClick={() => setSelected(rec)}
            >
              <div className="tt-title">
                {rec.year} {rec.termSeason}
              </div>
              <div className="tt-meta">
                {rec.createdAt?.slice(0, 10)} · {rec.candidates?.length}개 후보
              </div>
              <button
                className="btn-danger-sm"
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(rec.id);
                }}
              >
                삭제
              </button>
            </div>
          ))}
          {recommendations.length === 0 && (
            <p className="empty">추천 결과가 없습니다.</p>
          )}
        </div>

        {/* Detail */}
        <div className="timetable-view">
          {selected ? (
            <>
              <h3>
                {selected.year} {selected.termSeason} 추천 결과
              </h3>
              {(selected.candidates || []).map((cand) => (
                <div key={cand.id} className="candidate-card">
                  <div className="candidate-header">
                    <span className="rank-badge">후보 {cand.rank}</span>
                    <span className="credits-badge">
                      {cand.totalCredits}학점
                    </span>
                    <button
                      className="btn-secondary-sm"
                      onClick={() => handleSaveAsUserTimetable(cand, selected)}
                    >
                      시간표로 저장
                    </button>
                  </div>
                  <MiniGrid lectures={cand.lectures} />
                  <div
                    style={{
                      marginTop: 8,
                      display: "flex",
                      flexWrap: "wrap",
                      gap: "4px 12px",
                    }}
                  >
                    {(cand.lectures || []).map((l) => (
                      <span
                        key={l.id}
                        style={{ fontSize: "0.8rem", color: "var(--muted)" }}
                      >
                        {l.courseName}{" "}
                        <strong style={{ color: "var(--text)" }}>
                          {l.credits}학점
                        </strong>
                        {l.professor ? ` · ${l.professor}` : ""}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </>
          ) : (
            <p className="empty">추천 결과를 선택하거나 새로 생성하세요.</p>
          )}
        </div>
      </div>
    </div>
  );
}
