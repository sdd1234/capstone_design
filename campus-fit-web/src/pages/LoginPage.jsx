import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, resetPassword } from "../api/auth";

export default function LoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showReset, setShowReset] = useState(false);
  const [resetForm, setResetForm] = useState({
    email: "",
    newPassword: "",
    confirm: "",
  });
  const [resetMsg, setResetMsg] = useState("");
  const [resetError, setResetError] = useState("");
  const [resetLoading, setResetLoading] = useState(false);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await login(form.email, form.password);
      const { accessToken } = res.data.data;
      localStorage.setItem("accessToken", accessToken);
      await onLogin();
      navigate("/timetable");
    } catch (err) {
      setError(err.response?.data?.message || "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 테스트 로그인
  const handleTestLogin = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await login("admin@campusfit.com", "admin1234!");
      const { accessToken } = res.data.data;
      localStorage.setItem("accessToken", accessToken);
      await onLogin();
      navigate("/timetable");
    } catch (err) {
      setError(err.response?.data?.message || "테스트 로그인 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleResetSubmit = async (e) => {
    e.preventDefault();
    setResetError("");
    setResetMsg("");
    if (resetForm.newPassword !== resetForm.confirm) {
      setResetError("비밀번호가 일치하지 않습니다.");
      return;
    }
    if (resetForm.newPassword.length < 6) {
      setResetError("비밀번호는 6자 이상이어야 합니다.");
      return;
    }
    setResetLoading(true);
    try {
      await resetPassword(resetForm.email, resetForm.newPassword);
      setResetMsg("비밀번호가 재설정되었습니다. 로그인해주세요.");
      setTimeout(() => setShowReset(false), 2000);
    } catch (err) {
      setResetError(err.response?.data?.message || "재설정에 실패했습니다.");
    } finally {
      setResetLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Campus Fit</h1>
        <h2>로그인</h2>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>이메일</label>
            <input
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              placeholder="user@uni.ac.kr"
              required
            />
          </div>
          <div className="field">
            <label>비밀번호</label>
            <input
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              placeholder="비밀번호"
              required
            />
          </div>
          {error && <p className="error">{error}</p>}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "로그인 중..." : "로그인"}
          </button>
        </form>
        <p className="auth-link" style={{ marginTop: 8 }}>
          <button
            type="button"
            onClick={() => {
              setShowReset(true);
              setResetForm({ email: form.email, newPassword: "", confirm: "" });
              setResetMsg("");
              setResetError("");
            }}
            style={{
              background: "none",
              border: "none",
              color: "var(--primary)",
              cursor: "pointer",
              fontSize: "0.9rem",
              fontWeight: 500,
              padding: 0,
            }}
          >
            비밀번호를 잊으셨나요?
          </button>
        </p>
        <button
          type="button"
          onClick={handleTestLogin}
          style={{
            width: "100%",
            padding: "10px 16px",
            background: "#f0f0f0",
            border: "1px solid #ddd",
            borderRadius: 8,
            cursor: "pointer",
            fontSize: "0.9rem",
            fontWeight: 500,
            marginTop: 16,
            marginBottom: 16,
          }}
        >
          🚀 테스트 로그인 (admin@campusfit.com)
        </button>
        <p className="auth-link">
          계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
      </div>

      {showReset && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.4)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
        >
          <div
            style={{
              background: "var(--card)",
              borderRadius: 12,
              padding: 32,
              width: 360,
              boxShadow: "0 8px 32px rgba(0,0,0,0.18)",
            }}
          >
            <h3 style={{ marginBottom: 20, fontSize: "1.1rem" }}>
              비밀번호 재설정
            </h3>
            <form onSubmit={handleResetSubmit}>
              <div className="field">
                <label>이메일</label>
                <input
                  type="email"
                  value={resetForm.email}
                  onChange={(e) =>
                    setResetForm({ ...resetForm, email: e.target.value })
                  }
                  required
                  placeholder="가입한 이메일"
                />
              </div>
              <div className="field">
                <label>새 비밀번호</label>
                <input
                  type="password"
                  value={resetForm.newPassword}
                  onChange={(e) =>
                    setResetForm({ ...resetForm, newPassword: e.target.value })
                  }
                  required
                  placeholder="6자 이상"
                />
              </div>
              <div className="field">
                <label>새 비밀번호 확인</label>
                <input
                  type="password"
                  value={resetForm.confirm}
                  onChange={(e) =>
                    setResetForm({ ...resetForm, confirm: e.target.value })
                  }
                  required
                  placeholder="비밀번호 재입력"
                />
              </div>
              {resetError && <p className="error">{resetError}</p>}
              {resetMsg && <p className="success">{resetMsg}</p>}
              <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={resetLoading}
                  style={{ flex: 1 }}
                >
                  {resetLoading ? "처리 중..." : "재설정"}
                </button>
                <button
                  type="button"
                  onClick={() => setShowReset(false)}
                  className="btn-secondary"
                  style={{ flex: 1 }}
                >
                  취소
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
