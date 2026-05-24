import { Outlet } from "react-router";
import { Header } from "../components/Header";

export function RootLayout() {
  return (
    <div className="min-h-screen">
      <div className="hanzi-texture" />
      <Header />
      <Outlet />
    </div>
  );
}
