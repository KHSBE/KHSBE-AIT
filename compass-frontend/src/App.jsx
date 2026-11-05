import { useEffect, useState } from "react";
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Link,
  NavLink,
  useNavigate,
  Navigate,
} from "react-router-dom";
import { CalendarDays, Navigation2, LogOut, ShieldCheck } from "lucide-react";
import Home from "./pages/Home";
import Planner from "./pages/Planner";
import Login from "./pages/Login";

import OAuth2Redirect from "./pages/OAuth2Redirect";
import MyPage from "./pages/MyPage";
import PlanDetail from "./pages/PlanDetail";
import Admin from "./pages/Admin";
import {
  buildApiUrl,
  fetchWithAuth,
  getAccessTokenRole,
  logout,
} from "./utils/api";

function ProtectedRoute({ children }) {
  const token = localStorage.getItem("accessToken");

  if (!token) {
    alert("로그인이 필요한 서비스입니다.");
    return <Navigate to="/login" replace />;
  }

  return children;
}

function Navigation() {
  const navigate = useNavigate();
  const isLoggedIn = !!localStorage.getItem("accessToken");
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    const checkAdmin = async () => {
      if (!isLoggedIn) {
        setIsAdmin(false);
        return;
      }

      if (getAccessTokenRole() !== "ADMIN") {
        setIsAdmin(false);
        return;
      }

      try {
        const response = await fetchWithAuth(buildApiUrl("/api/admin/me"));
        if (!response.ok) {
          setIsAdmin(false);
          return;
        }

        const data = await response.json();
        setIsAdmin(!!data?.data?.admin);
      } catch {
        setIsAdmin(false);
      }
    };

    checkAdmin();
  }, [isLoggedIn]);

  const handleLogout = async () => {
    try {
      await logout();
      navigate("/");
    } catch (e) {
      alert(e.message);
    }
  };
  return (
    <nav className="app-navigation" aria-label="주 메뉴">
      <div className="navigation-inner">
        <Link to="/" className="brand" aria-label="Compass 홈">
          <span className="brand-symbol">
            <Navigation2 size={24} strokeWidth={2} />
          </span>
          Compass
        </Link>
        <div className="navigation-links">
          <NavLink to="/planner" className="nav-link" aria-label="여행 만들기">
            <Navigation2 size={18} />
            <span className="nav-label">여행 만들기</span>
          </NavLink>
          {isLoggedIn && (
            <NavLink to="/mypage" className="nav-link" aria-label="내 여행">
              <CalendarDays size={18} />
              <span className="nav-label">내 여행</span>
            </NavLink>
          )}
          {isLoggedIn && isAdmin && (
            <NavLink
              to="/admin"
              className="nav-link nav-admin"
              aria-label="관리자"
            >
              <ShieldCheck size={18} />
              <span className="nav-label">관리자</span>
            </NavLink>
          )}
          {isLoggedIn ? (
            <button
              type="button"
              onClick={handleLogout}
              className="nav-link nav-logout"
              aria-label="로그아웃"
            >
              <LogOut size={15} />
              <span className="nav-label">로그아웃</span>
            </button>
          ) : (
            <Link to="/login" className="nav-link nav-login">
              로그인
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}
function App() {
  return (
    <Router>
      <div className="page-shell text-[#17263a]">
        <Navigation />
        <main className="relative z-10">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Navigate to="/login" replace />} />
            <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
            <Route
              path="/planner"
              element={
                <ProtectedRoute>
                  <Planner />
                </ProtectedRoute>
              }
            />
            <Route
              path="/mypage"
              element={
                <ProtectedRoute>
                  <MyPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/plans/:id"
              element={
                <ProtectedRoute>
                  <PlanDetail />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  <Admin />
                </ProtectedRoute>
              }
            />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
