import { createBrowserRouter } from "react-router";
import { RootLayout } from "./components/RootLayout";
import { MainPage } from "./pages/MainPage";
import { TradePage } from "./pages/TradePage";
import { LoginPage } from "./pages/LoginPage";
import { ProfilePage } from "./pages/ProfilePage";
import { DeckBuilderPage } from "./pages/DeckBuilderPage";
import { NotFound } from "./pages/NotFound";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: RootLayout,
    children: [
      { index: true, Component: MainPage },
      { path: "trade", Component: TradePage },
      { path: "login", Component: LoginPage },
      { path: "profile", Component: ProfilePage },
      { path: "deck", Component: DeckBuilderPage },
      { path: "*", Component: NotFound },
    ],
  },
]);
