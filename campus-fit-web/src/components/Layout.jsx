import { useEffect, useRef, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import IOSDevice from "./IOSDevice";
import BottomNav from "./BottomNav";

const PHONE_W = 402;
const PHONE_H = 874;

export default function Layout({ user, onLogout }) {
  const navigate = useNavigate();
  const [scale, setScale] = useState(1);
  const scrollRef = useRef(null);

  // 뷰포트에 맞춰 폰을 축소 (시안 Mount 로직)
  useEffect(() => {
    const fit = () => {
      const s = Math.min(
        1,
        (window.innerWidth - 24) / PHONE_W,
        (window.innerHeight - 24) / PHONE_H,
      );
      setScale(s > 0 ? s : 1);
    };
    fit();
    window.addEventListener("resize", fit);
    return () => window.removeEventListener("resize", fit);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("accessToken");
    onLogout();
    navigate("/login");
  };

  return (
    <div className="phone-stage">
      <div style={{ transform: `scale(${scale})`, transformOrigin: "center center" }}>
        <IOSDevice width={PHONE_W} height={PHONE_H}>
          <div style={{ height: "100%", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
            {/* 단일 스크롤러 — 상태바 여백(상단) 확보, 페이지는 일반 흐름으로 렌더 */}
            <div ref={scrollRef} className="phone-scroll cf-scroll" style={{ flex: 1, minHeight: 0, overflowY: "auto", overflowX: "hidden" }}>
              <Outlet context={{ user, onLogout: handleLogout }} />
            </div>
            <BottomNav />
          </div>
        </IOSDevice>
      </div>
    </div>
  );
}
