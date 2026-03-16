import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signup } from "../api/auth";

export default function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: "",
    password: "",
    name: "",
    serviceAgree: false,
    privacyAgree: false,
    marketingAgree: false,
  });
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const formData = new FormData();
      formData.append("email", form.email);
      formData.append("password", form.password);
      formData.append("name", form.name);
      formData.append("serviceAgree", form.serviceAgree);
      formData.append("privacyAgree", form.privacyAgree);
      formData.append("marketingAgree", form.marketingAgree);
      if (file) formData.append("verificationFile", file);

      await signup(formData);
      setSuccess("가입 완료! 관리자 인증 후 로그인 가능합니다.");
      setTimeout(() => navigate("/login"), 2000);
    } catch (err) {
      setError(err.response?.data?.message || "회원가입에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Campus Fit</h1>
        <h2>회원가입</h2>
        {success && <p className="success">{success}</p>}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>이메일</label>
            <input
              name="email"
              type="email"
              value={form.email}
              onChange={handleChange}
              required
            />
          </div>
          <div className="field">
            <label>이름</label>
            <input
              name="name"
              type="text"
              value={form.name}
              onChange={handleChange}
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
              required
            />
          </div>
          <div className="field">
            <label>재학생 인증 파일 (선택)</label>
            <input
              type="file"
              accept="image/*,.pdf"
              onChange={(e) => setFile(e.target.files[0])}
            />
          </div>
          <div className="field-check">
            <label>
              <input
                name="serviceAgree"
                type="checkbox"
                checked={form.serviceAgree}
                onChange={handleChange}
              />
              서비스 이용약관 동의 (필수)
            </label>
          </div>
          <div className="field-check">
            <label>
              <input
                name="privacyAgree"
                type="checkbox"
                checked={form.privacyAgree}
                onChange={handleChange}
              />
              개인정보 처리방침 동의 (필수)
            </label>
          </div>
          <div className="field-check">
            <label>
              <input
                name="marketingAgree"
                type="checkbox"
                checked={form.marketingAgree}
                onChange={handleChange}
              />
              마케팅 수신 동의 (선택)
            </label>
          </div>
          {error && <p className="error">{error}</p>}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "처리 중..." : "회원가입"}
          </button>
        </form>
        <p className="auth-link">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </div>
    </div>
  );
}
