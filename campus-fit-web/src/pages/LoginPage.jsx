import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { GraduationCap, ArrowRight, Zap } from "lucide-react";

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
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            marginBottom: 4,
          }}
        >
          <GraduationCap size={28} color="#6366f1" />
          <h1>Campus Fit</h1>
        </div>
        <h2>AI 기반 시간표 추천 플랫폼</h2>
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
            {!loading && <ArrowRight size={16} />}
          </button>
        </form>
        <p className="auth-link">
          계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
        <button
          type="button"
          onClick={handleTestLogin}
          style={{
            width: "100%",
            padding: "10px 16px",
            background: "linear-gradient(135deg, #f0f9ff, #eef2ff)",
            border: "1.5px solid #c7d2fe",
            borderRadius: 10,
            cursor: "pointer",
            fontSize: "0.88rem",
            fontWeight: 600,
            marginTop: 16,
            marginBottom: 2,
            color: "#4f46e5",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 6,
            transition: "all 0.2s",
          }}
        >
          <Zap size={15} />
          데모 로그인
        </button>
      </div>
    </div>
  );
}
