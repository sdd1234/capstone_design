import { useState, useEffect } from "react";
import { getMe, updateMe, changePassword } from "../api/user";

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

  useEffect(() => {
    loadProfile();
  }, []);

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
