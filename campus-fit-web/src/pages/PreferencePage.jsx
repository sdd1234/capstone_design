import { useState, useEffect } from "react";
import { getPreference, savePreference } from "../api/preference";
import { getDepts, listLectures } from "../api/academic";

const DAYS = ["MON", "TUE", "WED", "THU", "FRI"];
const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금" };

const emptyPref = {
  year: 2026,
  termSeason: "SPRING",
  creditPolicy: {
    minCredits: 12,
    maxCredits: 21,
    targetCredits: 18,
    targetMajorCredits: 9,
    targetGeneralCredits: 6,
    targetRemoteCredits: 0,
  },
  options: {
    excludeMorning: false,
    maxDaysPerWeek: 5,
    allowGapsMinutes: 0,
    dept: "",
    preferMajorOnly: true,
    grade: null,
  },
  timeRanges: [],
  desiredCourses: [],
};

export default function PreferencePage() {
  const [pref, setPref] = useState(emptyPref);
  const [loaded, setLoaded] = useState(false);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [depts, setDepts] = useState([]);
  const [courseSearches, setCourseSearches] = useState({});

  // 학과 목록: loaded 여부와 무관하게 year/termSeason 변경 시 자동 로드
  useEffect(() => {
    getDepts(pref.year, pref.termSeason)
      .then((res) => setDepts(res.data.data || []))
      .catch(() => setDepts([]));
  }, [pref.year, pref.termSeason]);

  const handleLoad = async () => {
    setError("");
    setMsg("");
    setCourseSearches({});
    // 학과 목록 재조회
    getDepts(pref.year, pref.termSeason)
      .then((res) => setDepts(res.data.data || []))
      .catch(() => {});
    try {
      const res = await getPreference(pref.year, pref.termSeason);
      const data = res.data.data;
      setPref({
        year: data.year,
        termSeason: data.termSeason,
        creditPolicy: { ...emptyPref.creditPolicy, ...data.creditPolicy },
        options: {
          ...emptyPref.options,
          ...data.options,
          preferMajorOnly: true,
        },
        timeRanges: data.timeRanges || [],
        desiredCourses: data.desiredCourses || [],
      });
      setLoaded(true);
    } catch (err) {
      if (err.response?.status === 404) {
        setMsg("저장된 설정이 없습니다. 새로 작성 후 저장하세요.");
        setLoaded(true);
      } else {
        setError("불러오기 실패");
      }
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setError("");
    setMsg("");
    try {
      await savePreference(pref);
      setMsg("설정이 저장되었습니다.");
    } catch (err) {
      setError(err.response?.data?.message || "저장 실패");
    }
  };

  const addTimeRange = () => {
    setPref({
      ...pref,
      timeRanges: [
        ...pref.timeRanges,
        {
          type: "AVOID",
          dayOfWeek: "MON",
          startTime: "09:00",
          endTime: "12:00",
        },
      ],
    });
  };

  const removeTimeRange = (idx) => {
    setPref({
      ...pref,
      timeRanges: pref.timeRanges.filter((_, i) => i !== idx),
    });
  };

  const updateTimeRange = (idx, key, value) => {
    const updated = pref.timeRanges.map((r, i) =>
      i === idx ? { ...r, [key]: value } : r,
    );
    setPref({ ...pref, timeRanges: updated });
  };

  const addDesiredCourse = () => {
    setPref({
      ...pref,
      desiredCourses: [
        ...pref.desiredCourses,
        { courseId: null, rawText: "", priority: 1 },
      ],
    });
  };

  const removeDesiredCourse = (idx) => {
    setPref({
      ...pref,
      desiredCourses: pref.desiredCourses.filter((_, i) => i !== idx),
    });
    setCourseSearches({});
  };

  const updateDesiredCourse = (idx, key, value) => {
    const updated = pref.desiredCourses.map((d, i) =>
      i === idx ? { ...d, [key]: value } : d,
    );
    setPref({ ...pref, desiredCourses: updated });
  };

  const handleCourseSearch = async (idx, query) => {
    setCourseSearches((prev) => ({
      ...prev,
      [idx]: { ...(prev[idx] || {}), query, open: false },
    }));
    if (!query || query.length < 1) return;
    try {
      const res = await listLectures({
        universityId: 1,
        year: pref.year,
        termSeason: pref.termSeason,
        keyword: query,
      });
      const results = res.data.data || [];
      setCourseSearches((prev) => ({
        ...prev,
        [idx]: { query, results, open: results.length > 0 },
      }));
    } catch {
      setCourseSearches((prev) => ({
        ...prev,
        [idx]: { query, results: [], open: false },
      }));
    }
  };

  const selectCourse = (idx, lecture) => {
    const updated = pref.desiredCourses.map((d, i) =>
      i === idx
        ? { ...d, courseId: lecture.courseId, rawText: lecture.courseName }
        : d,
    );
    setPref({ ...pref, desiredCourses: updated });
    setCourseSearches((prev) => ({
      ...prev,
      [idx]: { query: lecture.courseName, results: [], open: false },
    }));
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>수강 설정</h2>
      </div>

      {msg && <p className="success">{msg}</p>}
      {error && <p className="error">{error}</p>}

      <div className="card">
        <div className="search-row">
          <select
            value={pref.year}
            onChange={(e) =>
              setPref({ ...pref, year: parseInt(e.target.value) })
            }
          >
            <option value={2026}>2026</option>
            <option value={2025}>2025</option>
          </select>
          <select
            value={pref.termSeason}
            onChange={(e) => setPref({ ...pref, termSeason: e.target.value })}
          >
            <option value="SPRING">1학기</option>
            <option value="SUMMER">여름학기</option>
            <option value="FALL">2학기</option>
            <option value="WINTER">겨울학기</option>
          </select>
          <button className="btn-secondary" onClick={handleLoad}>
            불러오기
          </button>
        </div>
      </div>

      {loaded && (
        <form onSubmit={handleSave}>
          {/* Credit Policy */}
          <div className="card">
            <h3>학점 설정</h3>
            <div className="form-row">
              <div className="field">
                <label>목표 학점</label>
                <input
                  type="number"
                  value={pref.creditPolicy.targetCredits}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      creditPolicy: {
                        ...pref.creditPolicy,
                        targetCredits: parseInt(e.target.value),
                      },
                    })
                  }
                />
              </div>
              <div className="field">
                <label>최소 학점</label>
                <input
                  type="number"
                  value={pref.creditPolicy.minCredits}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      creditPolicy: {
                        ...pref.creditPolicy,
                        minCredits: parseInt(e.target.value),
                      },
                    })
                  }
                />
              </div>
              <div className="field">
                <label>최대 학점</label>
                <input
                  type="number"
                  value={pref.creditPolicy.maxCredits}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      creditPolicy: {
                        ...pref.creditPolicy,
                        maxCredits: parseInt(e.target.value),
                      },
                    })
                  }
                />
              </div>
            </div>
            <div className="form-row" style={{ marginTop: "0.75rem" }}>
              <div className="field">
                <label>전공 목표 학점</label>
                <input
                  type="number"
                  min={0}
                  value={pref.creditPolicy.targetMajorCredits ?? 0}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      creditPolicy: {
                        ...pref.creditPolicy,
                        targetMajorCredits: parseInt(e.target.value),
                      },
                    })
                  }
                />
              </div>
              <div className="field">
                <label>교양 목표 학점</label>
                <input
                  type="number"
                  min={0}
                  value={pref.creditPolicy.targetGeneralCredits ?? 0}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      creditPolicy: {
                        ...pref.creditPolicy,
                        targetGeneralCredits: parseInt(e.target.value),
                      },
                    })
                  }
                />
              </div>
              <div className="field">
                <label>원격 목표 학점</label>
                <input
                  type="number"
                  min={0}
                  value={pref.creditPolicy.targetRemoteCredits ?? 0}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      creditPolicy: {
                        ...pref.creditPolicy,
                        targetRemoteCredits: parseInt(e.target.value),
                      },
                    })
                  }
                />
              </div>
            </div>
          </div>

          {/* Options */}
          <div className="card">
            <h3>기타 설정</h3>
            <div className="form-row">
              <div className="field">
                <label>학과</label>
                <select
                  value={pref.options.dept || ""}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      options: { ...pref.options, dept: e.target.value },
                    })
                  }
                >
                  <option value="">전체 (선택 안 함)</option>
                  {depts.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>학년</label>
                <select
                  value={pref.options.grade ?? ""}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      options: {
                        ...pref.options,
                        grade:
                          e.target.value === ""
                            ? null
                            : parseInt(e.target.value),
                      },
                    })
                  }
                >
                  <option value="">전학년</option>
                  {[1, 2, 3, 4].map((g) => (
                    <option key={g} value={g}>
                      {g}학년
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>학기</label>
                <select
                  value={pref.termSeason}
                  onChange={(e) =>
                    setPref({ ...pref, termSeason: e.target.value })
                  }
                >
                  <option value="SPRING">1학기</option>
                  <option value="FALL">2학기</option>
                  <option value="SUMMER">여름학기</option>
                  <option value="WINTER">겨울학기</option>
                </select>
              </div>
              <div className="field">
                <label>최대 등교일수</label>
                <select
                  value={pref.options.maxDaysPerWeek}
                  onChange={(e) =>
                    setPref({
                      ...pref,
                      options: {
                        ...pref.options,
                        maxDaysPerWeek: parseInt(e.target.value),
                      },
                    })
                  }
                >
                  {[1, 2, 3, 4, 5].map((n) => (
                    <option key={n} value={n}>
                      {n}일
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="form-row" style={{ marginTop: "0.5rem" }}>
              <div className="field-check">
                <label>
                  <input
                    type="checkbox"
                    checked={pref.options.excludeMorning}
                    onChange={(e) =>
                      setPref({
                        ...pref,
                        options: {
                          ...pref.options,
                          excludeMorning: e.target.checked,
                        },
                      })
                    }
                  />
                  9시 수업 제외
                </label>
              </div>
            </div>
          </div>

          {/* Time Ranges */}
          <div className="card">
            <div className="section-header">
              <h3>기피·선호 시간대</h3>
              <button
                type="button"
                className="btn-secondary"
                onClick={addTimeRange}
              >
                + 추가
              </button>
            </div>
            {pref.timeRanges.map((r, idx) => (
              <div key={idx} className="time-range-row">
                <select
                  value={r.type}
                  onChange={(e) => updateTimeRange(idx, "type", e.target.value)}
                >
                  <option value="AVOID">기피</option>
                  <option value="PREFER">선호</option>
                </select>
                <select
                  value={r.dayOfWeek || ""}
                  onChange={(e) =>
                    updateTimeRange(idx, "dayOfWeek", e.target.value || null)
                  }
                >
                  <option value="">모든 요일</option>
                  {DAYS.map((d) => (
                    <option key={d} value={d}>
                      {DAY_LABELS[d]}
                    </option>
                  ))}
                </select>
                <input
                  type="time"
                  value={r.startTime}
                  onChange={(e) =>
                    updateTimeRange(idx, "startTime", e.target.value)
                  }
                />
                <span>~</span>
                <input
                  type="time"
                  value={r.endTime}
                  onChange={(e) =>
                    updateTimeRange(idx, "endTime", e.target.value)
                  }
                />
                <button
                  type="button"
                  className="btn-danger-sm"
                  onClick={() => removeTimeRange(idx)}
                >
                  삭제
                </button>
              </div>
            ))}
            {pref.timeRanges.length === 0 && (
              <p className="empty">설정된 시간대가 없습니다.</p>
            )}
          </div>

          {/* Desired Courses */}
          <div className="card">
            <div className="section-header">
              <h3>희망 수강 과목</h3>
              <button
                type="button"
                className="btn-secondary"
                onClick={addDesiredCourse}
              >
                + 추가
              </button>
            </div>
            {pref.desiredCourses.map((d, idx) => {
              const cs = courseSearches[idx] || {};
              const displayVal =
                cs.query !== undefined ? cs.query : d.rawText || "";
              return (
                <div
                  key={idx}
                  className="desired-course-row"
                  style={{ alignItems: "flex-start" }}
                >
                  <div style={{ flex: 1, position: "relative" }}>
                    <input
                      placeholder="과목명 검색 (예: 자료구조)"
                      value={displayVal}
                      onChange={(e) => handleCourseSearch(idx, e.target.value)}
                      onBlur={() =>
                        setTimeout(
                          () =>
                            setCourseSearches((prev) => ({
                              ...prev,
                              [idx]: { ...(prev[idx] || {}), open: false },
                            })),
                          150,
                        )
                      }
                      style={{ width: "100%" }}
                    />
                    {cs.open && cs.results && cs.results.length > 0 && (
                      <div
                        style={{
                          position: "absolute",
                          top: "100%",
                          left: 0,
                          right: 0,
                          zIndex: 1000,
                          background: "#fff",
                          border: "1px solid #ddd",
                          borderRadius: "4px",
                          maxHeight: "200px",
                          overflowY: "auto",
                          boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                        }}
                      >
                        {cs.results.slice(0, 20).map((l) => (
                          <div
                            key={l.id}
                            style={{
                              padding: "0.5rem 0.75rem",
                              cursor: "pointer",
                              borderBottom: "1px solid #f0f0f0",
                            }}
                            onMouseDown={(e) => {
                              e.preventDefault();
                              selectCourse(idx, l);
                            }}
                          >
                            <div style={{ fontWeight: 500 }}>
                              {l.courseName}
                            </div>
                            <div style={{ fontSize: "0.8rem", color: "#666" }}>
                              {l.dept} · {l.category} · {l.credits}학점
                              {l.professor ? ` · ${l.professor}` : ""}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    {d.courseId && (
                      <div
                        style={{
                          fontSize: "0.75rem",
                          color: "#4caf50",
                          marginTop: "2px",
                        }}
                      >
                        ✓ {d.rawText} 선택됨
                      </div>
                    )}
                  </div>
                  <select
                    value={d.priority || 1}
                    onChange={(e) =>
                      updateDesiredCourse(
                        idx,
                        "priority",
                        parseInt(e.target.value),
                      )
                    }
                  >
                    {[1, 2, 3].map((p) => (
                      <option key={p} value={p}>
                        우선순위 {p}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="btn-danger-sm"
                    onClick={() => removeDesiredCourse(idx)}
                  >
                    삭제
                  </button>
                </div>
              );
            })}
            {pref.desiredCourses.length === 0 && (
              <p className="empty">희망 과목이 없습니다.</p>
            )}
          </div>

          <div className="form-actions">
            <button type="submit" className="btn-primary">
              설정 저장
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
