import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  CalendarDays,
  Clock,
  GraduationCap,
  LayoutGrid,
  Sparkles,
  User,
  ShieldCheck,
  LogOut,
} from "lucide-react";

// 강의 검색·수강 설정은 'AI 추천' 안으로 흡수됨 (메뉴 7→5 단순화)
const NAV_ITEMS = [
  { to: "/timetable", label: "시간표", icon: LayoutGrid },
  { to: "/calendar", label: "캘린더", icon: CalendarDays },
  { to: "/ai", label: "AI 추천", icon: Sparkles },
  { to: "/time-management", label: "시간 관리", icon: Clock },
  { to: "/profile", label: "내 정보", icon: User },
];

export default function Layout({ user, onLogout }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("accessToken");
    onLogout();
    navigate("/login");
  };

  return (
    <div className="layout">
      <nav className="sidebar">
        <div className="sidebar-brand">
          <GraduationCap size={22} />
          Campus Fit
        </div>
        <ul className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) => (isActive ? "active" : "")}
              >
                <item.icon size={18} />
                {item.label}
              </NavLink>
            </li>
          ))}
          {user?.role === "ADMIN" && (
            <li>
              <NavLink
                to="/admin"
                className={({ isActive }) => (isActive ? "active" : "")}
              >
                <ShieldCheck size={18} />
                관리자
              </NavLink>
            </li>
          )}
        </ul>
        <div className="sidebar-footer">
          <span className="sidebar-user">{user?.name}</span>
          <button className="btn-link" onClick={handleLogout}>
            <LogOut size={14} style={{ marginRight: 4, verticalAlign: "middle" }} />
            로그아웃
          </button>
        </div>
      </nav>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
