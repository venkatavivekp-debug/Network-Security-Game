import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../state/auth/AuthProvider";
import "../../styles/commandCenter.css";

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="cc-app">
      <header className="cc-topbar">
        <div className="cc-topbar__brand">
          <div className="cc-mark" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <div>
            <div className="cc-brand-title">Network Security Game</div>
            <div className="cc-brand-sub">Cyber Defense Simulation Platform</div>
          </div>
        </div>

        <nav className="cc-topbar__nav">
          <NavLink to="/simulation" className={({ isActive }) => (isActive ? "cc-navlink is-active" : "cc-navlink")}>
            Simulation
          </NavLink>
          <NavLink to="/messaging" className={({ isActive }) => (isActive ? "cc-navlink is-active" : "cc-navlink")}>
            Messaging
          </NavLink>
          <NavLink to="/evaluation" className={({ isActive }) => (isActive ? "cc-navlink is-active" : "cc-navlink")}>
            Evaluation
          </NavLink>
        </nav>

        <div className="cc-topbar__user">
          {user ? (
            <>
              <span className="cc-userchip">
                {user.username} · {user.role}
              </span>
              <button className="cc-btn cc-btn--ghost" type="button" onClick={() => void logout()}>
                Logout
              </button>
            </>
          ) : (
            <span className="cc-userchip">Not signed in</span>
          )}
        </div>
      </header>

      <main className="cc-main">
        <Outlet />
      </main>
    </div>
  );
}

