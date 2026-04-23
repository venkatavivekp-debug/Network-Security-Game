import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/layout/AppLayout";
import { RequireAuth } from "./state/auth/RequireAuth";
import { LoginPage } from "./pages/LoginPage";
import { CommandCenterPage } from "./pages/CommandCenterPage";
import { MessagingPage } from "./pages/MessagingPage";
import { EvaluationPage } from "./pages/EvaluationPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/login" element={<LoginPage />} />

        <Route
          path="/simulation"
          element={
            <RequireAuth>
              <CommandCenterPage />
            </RequireAuth>
          }
        />
        <Route
          path="/messaging"
          element={
            <RequireAuth>
              <MessagingPage />
            </RequireAuth>
          }
        />
        <Route
          path="/evaluation"
          element={
            <RequireAuth>
              <EvaluationPage />
            </RequireAuth>
          }
        />

        <Route path="/" element={<Navigate to="/simulation" replace />} />
        <Route path="*" element={<Navigate to="/simulation" replace />} />
      </Route>
    </Routes>
  );
}
