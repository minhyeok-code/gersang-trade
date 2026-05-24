import { Outlet } from "react-router";
import { Header } from "./Header";
import "../../styles/geosang.css";

export function RootLayout() {
  return (
    <div className="min-h-screen">
      <div className="fixed inset-0 pointer-events-none z-0 bg-[repeating-linear-gradient(0deg,transparent,transparent_32px,rgba(139,107,74,0.02)_32px,rgba(139,107,74,0.02)_33px)]"></div>
      <div className="relative z-[1]">
        <Header />
        <Outlet />
      </div>
    </div>
  );
}
