import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../api/auth";

export default function LoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await login(form.email, form.password);
      const { accessToken, refreshToken } = res.data.data;
      localStorage.setItem("accessToken", accessToken);
      if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
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
      const { accessToken, refreshToken } = res.data.data;
      localStorage.setItem("accessToken", accessToken);
      if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
      await onLogin();
      navigate("/timetable");
    } catch (err) {
      setError(err.response?.data?.message || "테스트 로그인 실패");
    } finally {
      setLoading(false);
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
    </div>
  );
}
