import { useState, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { getMe } from "./api/user";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import TimetablePage from "./pages/TimetablePage";
import CalendarPage from "./pages/CalendarPage";
import AiRecommendationPage from "./pages/AiRecommendationPage";
import LecturePage from "./pages/LecturePage";
import ProfilePage from "./pages/ProfilePage";
import PreferencePage from "./pages/PreferencePage";
import AdminPage from "./pages/AdminPage";

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadUser = async () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setLoading(false);
      return;
    }
    try {
      const res = await getMe();
      setUser(res.data.data);
    } catch {
      localStorage.removeItem("accessToken");
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUser();
  }, []);

  if (loading) return <div className="loading">로딩 중...</div>;

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            user ? (
              <Navigate to="/timetable" replace />
            ) : (
              <LoginPage onLogin={loadUser} />
            )
          }
        />
        <Route path="/signup" element={<SignupPage />} />

        <Route
          element={
            <ProtectedRoute user={user}>
              <Layout user={user} onLogout={() => setUser(null)} />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Navigate to="/timetable" replace />} />
          <Route path="/timetable" element={<TimetablePage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/ai" element={<AiRecommendationPage />} />
          <Route path="/lectures" element={<LecturePage />} />
          <Route
            path="/profile"
            element={<ProfilePage onUserUpdate={loadUser} />}
          />
          <Route path="/preference" element={<PreferencePage />} />
          {user?.role === "ADMIN" && (
            <Route path="/admin" element={<AdminPage />} />
          )}
        </Route>

        <Route
          path="*"
          element={<Navigate to={user ? "/timetable" : "/login"} replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
