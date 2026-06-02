import { useState, useEffect } from "react";
import { getMe, updateMe, changePassword } from "../api/user";
import {
  listTimetables,
  createTimetable,
  deleteTimetable,
  setPrimaryTimetable,
} from "../api/timetable";
import { listLectures } from "../api/academic";
import { getCurrentSemester, TERM_LABELS } from "../utils/semester";

const DAY_LABELS = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };

export default function ProfilePage({ onUserUpdate }) {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: "", marketingAgree: false });
  const [pwForm, setPwForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [tab, setTab] = useState("profile");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  // ── 내 시간표 ──────────────────────────────────────────────
  const sem = getCurrentSemester();
  const [timetables, setTimetables] = useState([]);
  const [showTtForm, setShowTtForm] = useState(false);
  const [ttForm, setTtForm] = useState({
    year: sem.year,
    termSeason: sem.termSeason,
    title: "",
    setPrimary: true,
  });
  const [ttKeyword, setTtKeyword] = useState("");
  const [ttLectures, setTtLectures] = useState([]);
  const [ttSelectedIds, setTtSelectedIds] = useState([]);

  useEffect(() => {
    loadProfile();
    loadTimetables();
  }, []);

  const loadTimetables = async () => {
    try {
      const res = await listTimetables();
      setTimetables(res.data.data || []);
    } catch {
      /* 시간표 로드 실패는 조용히 무시 (프로필 탭엔 영향 없음) */
    }
  };

  const handleSearchTtLectures = async () => {
    try {
      const res = await listLectures({
        universityId: 1,
        year: ttForm.year,
        termSeason: ttForm.termSeason,
        keyword: ttKeyword || undefined,
      });
      setTtLectures(res.data.data || []);
    } catch {
      setTtLectures([]);
    }
  };

  const toggleTtLecture = (id) => {
    setTtSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id],
    );
  };

  const handleCreateTt = async (e) => {
    e?.preventDefault();
    setError("");
    setMsg("");
    try {
      const res = await createTimetable({
        year: ttForm.year,
        termSeason: ttForm.termSeason,
        title: ttForm.title?.trim() || undefined,
        lectureIds: ttSelectedIds,
      });
      const created = res.data?.data;
      if (ttForm.setPrimary && created?.id) {
        await setPrimaryTimetable(created.id);
      }
      setMsg("시간표가 저장되었습니다.");
      setShowTtForm(false);
      setTtSelectedIds([]);
      setTtLectures([]);
      setTtKeyword("");
      setTtForm((f) => ({ ...f, title: "" }));
      loadTimetables();
    } catch (err) {
      setError(err.response?.data?.message || "시간표 저장 실패");
    }
  };

  const handleSetPrimaryTt = async (id) => {
    setError("");
    setMsg("");
    try {
      await setPrimaryTimetable(id);
      setMsg("대표 시간표로 지정되었습니다.");
      loadTimetables();
    } catch (err) {
      setError(err.response?.data?.message || "대표 지정 실패");
    }
  };

  const handleDeleteTt = async (id) => {
    if (!window.confirm("이 시간표를 삭제할까요?")) return;
    setError("");
    setMsg("");
    try {
      await deleteTimetable(id);
      setMsg("시간표가 삭제되었습니다.");
      loadTimetables();
    } catch (err) {
      setError(err.response?.data?.message || "삭제 실패");
    }
  };

  const loadProfile = async () => {
    try {
      const res = await getMe();
      const data = res.data.data;
      setProfile(data);
      setForm({ name: data.name, marketingAgree: data.marketingAgree });
    } catch {
      setError("프로필을 불러오지 못했습니다.");
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setError("");
    setMsg("");
    try {
      await updateMe(form);
      setMsg("프로필이 수정되었습니다.");
      loadProfile();
      if (onUserUpdate) onUserUpdate();
    } catch (err) {
      setError(err.response?.data?.message || "수정 실패");
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setError("");
    setMsg("");
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }
    try {
      await changePassword({
        currentPassword: pwForm.currentPassword,
        newPassword: pwForm.newPassword,
      });
      setMsg("비밀번호가 변경되었습니다.");
      setPwForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      setError(err.response?.data?.message || "비밀번호 변경 실패");
    }
  };

  if (!profile)
    return (
      <div className="page-container">
        <p>로딩 중...</p>
      </div>
    );

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>내 정보</h2>
        <div className="tab-group">
          <button
            className={`tab ${tab === "profile" ? "active" : ""}`}
            onClick={() => setTab("profile")}
          >
            프로필
          </button>
          <button
            className={`tab ${tab === "timetables" ? "active" : ""}`}
            onClick={() => setTab("timetables")}
          >
            내 시간표
          </button>
          <button
            className={`tab ${tab === "password" ? "active" : ""}`}
            onClick={() => setTab("password")}
          >
            비밀번호
          </button>
        </div>
      </div>

      {msg && <p className="success">{msg}</p>}
      {error && <p className="error">{error}</p>}

      {tab === "profile" && (
        <div className="card" style={{ maxWidth: 480 }}>
          <div className="profile-meta">
            <div className="profile-avatar">{profile.name?.charAt(0)}</div>
            <div>
              <div className="profile-email">{profile.email}</div>
              <div className="profile-badges">
                <span className="badge">{profile.role}</span>
                <span
                  className={`badge status-${profile.status?.toLowerCase()}`}
                >
                  {profile.status}
                </span>
              </div>
            </div>
          </div>
          <form onSubmit={handleUpdateProfile} className="form-grid">
            <div className="field">
              <label>이름</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div className="field-check">
              <label>
                <input
                  type="checkbox"
                  checked={form.marketingAgree}
                  onChange={(e) =>
                    setForm({ ...form, marketingAgree: e.target.checked })
                  }
                />
                마케팅 수신 동의
              </label>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary">
                수정
              </button>
            </div>
          </form>
        </div>
      )}

      {tab === "timetables" && (
        <div className="card" style={{ maxWidth: 720 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <div>
              <h3 style={{ margin: 0 }}>내 학교 시간표</h3>
              <p style={{ margin: "4px 0 0", fontSize: "0.82rem", color: "var(--muted)" }}>
                학기별로 저장하고, '대표'로 지정하면 시간표 메뉴에 표시됩니다. 현재 학기: <strong>{sem.label}</strong>
              </p>
            </div>
            <button className="btn-primary" onClick={() => setShowTtForm((v) => !v)}>
              {showTtForm ? "취소" : "+ 새 시간표"}
            </button>
          </div>

          {showTtForm && (
            <form onSubmit={handleCreateTt} style={{ background: "var(--bg)", borderRadius: 8, padding: 14, marginBottom: 16 }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
                <input
                  type="number"
                  value={ttForm.year}
                  onChange={(e) => setTtForm({ ...ttForm, year: parseInt(e.target.value) || ttForm.year })}
                  style={{ width: 90 }}
                  aria-label="연도"
                />
                <select
                  value={ttForm.termSeason}
                  onChange={(e) => setTtForm({ ...ttForm, termSeason: e.target.value })}
                  style={{ width: 120 }}
                  aria-label="학기"
                >
                  <option value="SPRING">1학기</option>
                  <option value="SUMMER">여름방학</option>
                  <option value="FALL">2학기</option>
                  <option value="WINTER">겨울방학</option>
                </select>
                <input
                  type="text"
                  placeholder="시간표 이름 (선택)"
                  value={ttForm.title}
                  onChange={(e) => setTtForm({ ...ttForm, title: e.target.value })}
                  style={{ flex: 1, minWidth: 140 }}
                />
              </div>
              <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                <input
                  type="text"
                  placeholder="강의명·교수명 검색"
                  value={ttKeyword}
                  onChange={(e) => setTtKeyword(e.target.value)}
                  onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); handleSearchTtLectures(); } }}
                  style={{ flex: 1 }}
                />
                <button type="button" className="btn-secondary" onClick={handleSearchTtLectures}>검색</button>
              </div>
              {ttLectures.length > 0 && (
                <div style={{ maxHeight: 220, overflowY: "auto", border: "1px solid var(--border)", borderRadius: 4, marginBottom: 8 }}>
                  {ttLectures.map((l) => {
                    const picked = ttSelectedIds.includes(l.id);
                    return (
                      <div key={l.id} style={{ padding: "4px 8px", display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--border)" }}>
                        <span style={{ fontSize: "0.82rem" }}>
                          <strong>{l.courseName}</strong> · {l.professor || "-"} · {l.credits}학점
                          {(l.schedules || []).map((s, i) => (
                            <span key={i} style={{ color: "var(--muted)", marginLeft: 6 }}>
                              {DAY_LABELS[s.dayOfWeek]} {s.startTime}~{s.endTime}
                            </span>
                          ))}
                        </span>
                        <button type="button" className={picked ? "btn-secondary-sm" : "btn-primary-sm"} onClick={() => toggleTtLecture(l.id)}>
                          {picked ? "해제" : "추가"}
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
              <label style={{ display: "flex", alignItems: "center", gap: 6, fontSize: "0.85rem", marginBottom: 10 }}>
                <input
                  type="checkbox"
                  checked={ttForm.setPrimary}
                  onChange={(e) => setTtForm({ ...ttForm, setPrimary: e.target.checked })}
                />
                저장하면 이 시간표를 대표(시간표 메뉴에 표시)로 지정
              </label>
              <button type="submit" className="btn-primary">저장 ({ttSelectedIds.length}강의)</button>
            </form>
          )}

          {timetables.length === 0 && <p className="empty">아직 저장된 시간표가 없습니다.</p>}
          {[...timetables].sort((a, b) => b.year - a.year).map((t) => (
            <div key={t.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 0", borderBottom: "1px solid var(--border)" }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, display: "flex", alignItems: "center", gap: 6 }}>
                  {t.title}
                  {t.isPrimary && <span className="primary-badge">대표</span>}
                </div>
                <div style={{ fontSize: "0.78rem", color: "var(--muted)" }}>
                  {t.year} {TERM_LABELS[t.termSeason] || t.termSeason} · {(t.lectures || []).length}강의
                </div>
              </div>
              {!t.isPrimary && (
                <button className="btn-secondary-sm" onClick={() => handleSetPrimaryTt(t.id)}>대표 지정</button>
              )}
              <button className="btn-danger-sm" onClick={() => handleDeleteTt(t.id)}>삭제</button>
            </div>
          ))}
        </div>
      )}

      {tab === "password" && (
        <div className="card" style={{ maxWidth: 480 }}>
          <form onSubmit={handleChangePassword} className="form-grid">
            <div className="field">
              <label>현재 비밀번호</label>
              <input
                type="password"
                value={pwForm.currentPassword}
                onChange={(e) =>
                  setPwForm({ ...pwForm, currentPassword: e.target.value })
                }
                required
              />
            </div>
            <div className="field">
              <label>새 비밀번호</label>
              <input
                type="password"
                value={pwForm.newPassword}
                onChange={(e) =>
                  setPwForm({ ...pwForm, newPassword: e.target.value })
                }
                required
              />
            </div>
            <div className="field">
              <label>새 비밀번호 확인</label>
              <input
                type="password"
                value={pwForm.confirmPassword}
                onChange={(e) =>
                  setPwForm({ ...pwForm, confirmPassword: e.target.value })
                }
                required
              />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary">
                변경
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
