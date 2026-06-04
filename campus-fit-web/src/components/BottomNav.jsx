/* ─────────────────────────────────────────────────────────────
   하단 탭바 (디자인 시안 BottomNav 이식 — react-router 연결)
   ───────────────────────────────────────────────────────────── */
import { useLocation, useNavigate } from "react-router-dom";

const ICONS = {
  timetable: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="3" />
      <path d="M3 9h18M9 9v12" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="4.5" width="18" height="16.5" rx="3" />
      <path d="M3 9h18M8 2.5v4M16 2.5v4" />
    </>
  ),
  sparkles: (
    <>
      <path d="M12 3l1.8 4.9L18.7 9.7l-4.9 1.8L12 16.4l-1.8-4.9L5.3 9.7l4.9-1.8L12 3z" />
      <path d="M19 14.5l.7 1.9 1.9.7-1.9.7-.7 1.9-.7-1.9-1.9-.7 1.9-.7.7-1.9z" />
    </>
  ),
  clockcheck: (
    <>
      <circle cx="11" cy="11" r="8" />
      <path d="M11 7v4l2.5 1.5" />
      <path d="M15.5 18.5l1.8 1.8 3.2-3.6" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4.5 20a7.5 7.5 0 0115 0" />
    </>
  ),
};

const TABS = [
  { to: "/timetable", label: "시간표", icon: "timetable" },
  { to: "/calendar", label: "캘린더", icon: "calendar" },
  { to: "/ai", label: "AI 추천", icon: "sparkles" },
  { to: "/time-management", label: "시간관리", icon: "clockcheck" },
  { to: "/profile", label: "내 정보", icon: "user" },
];

function TabIcon({ name, on }) {
  return (
    <svg
      width="23"
      height="23"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={on ? 2.5 : 2}
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ display: "block" }}
    >
      {ICONS[name]}
    </svg>
  );
}

export default function BottomNav() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <nav
      style={{
        flexShrink: 0,
        display: "flex",
        justifyContent: "space-around",
        alignItems: "stretch",
        paddingTop: 8,
        paddingBottom: 26,
        background: "var(--surface)",
        borderTop: "1px solid var(--hairline)",
        position: "relative",
        zIndex: 6,
      }}
    >
      {TABS.map((t) => {
        const on =
          location.pathname === t.to ||
          location.pathname.startsWith(t.to + "/");
        return (
          <button
            key={t.to}
            onClick={() => navigate(t.to)}
            style={{
              flex: 1,
              border: "none",
              background: "none",
              cursor: "pointer",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 4,
              color: on ? "var(--primary)" : "var(--text-3)",
              padding: "2px 0",
              transition: "color .15s",
            }}
          >
            <TabIcon name={t.icon} on={on} />
            <span style={{ fontSize: 10.5, fontWeight: on ? 700 : 600, letterSpacing: "-.01em" }}>
              {t.label}
            </span>
          </button>
        );
      })}
    </nav>
  );
}
