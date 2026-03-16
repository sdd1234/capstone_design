import { NavLink, Outlet, useNavigate } from "react-router-dom";

const NAV_ITEMS = [
  { to: "/timetable", label: "시간표" },
  { to: "/calendar", label: "캘린더" },
  { to: "/ai", label: "AI 추천" },
  { to: "/lectures", label: "강의 검색" },
  { to: "/preference", label: "수강 설정" },
  { to: "/profile", label: "내 정보" },
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
        <div className="sidebar-brand">Campus Fit</div>
        <ul className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) => (isActive ? "active" : "")}
              >
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
                인증 관리
              </NavLink>
            </li>
          )}
        </ul>
        <div className="sidebar-footer">
          <span className="sidebar-user">{user?.name}</span>
          <button className="btn-link" onClick={handleLogout}>
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
