import { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate, Navigate } from 'react-router-dom';
import { LayoutDashboard, UserRound, LogOut, ShieldCheck } from 'lucide-react';
import Home from './pages/Home';
import Planner from './pages/Planner';
import Login from './pages/Login';
import Signup from './pages/Signup';
import OAuth2Redirect from './pages/OAuth2Redirect';
import MyPage from './pages/MyPage';
import PlanDetail from './pages/PlanDetail';
import Admin from './pages/Admin';
import { buildApiUrl, fetchWithAuth } from './utils/api';

function ProtectedRoute({ children }) {
  const token = localStorage.getItem('accessToken');

  if (!token) {
    alert('로그인이 필요한 서비스입니다.');
    return <Navigate to="/login" replace />;
  }

  return children;
}

function Navigation() {
  const navigate = useNavigate();
  const isLoggedIn = !!localStorage.getItem('accessToken');
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    const checkAdmin = async () => {
      if (!isLoggedIn) {
        setIsAdmin(false);
        return;
      }

      try {
        const response = await fetchWithAuth(buildApiUrl('/api/admin/me'));
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

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    alert('로그아웃되었습니다.');
    navigate('/');
  };

  return (
    <nav className="sticky top-0 z-50 border-b border-[#c9e3fb]/90 bg-[#f6fbff]/90 backdrop-blur-xl">
      <div className="mx-auto flex h-[76px] w-full max-w-7xl items-center justify-between px-4 md:px-8">
        <Link to="/" className="group flex items-center gap-2">
          <span className="h-2.5 w-2.5 rounded-full bg-[#1f8ef1] shadow-[0_0_0_6px_rgba(31,142,241,0.2)] transition group-hover:scale-110" />
          <span className="serif-display text-[30px] leading-none tracking-tight text-[#17263a] md:text-3xl">Compass</span>
        </Link>

        <div className="flex items-center gap-1 text-sm font-semibold md:gap-2">
          <Link
            to="/planner"
            className="hidden rounded-full px-4 py-2 text-[#4b6177] transition hover:bg-[#e2f1ff] hover:text-[#1f8ef1] sm:inline-flex"
          >
            AI 플래너
          </Link>
          <Link
            to="/planner"
            className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[#d1e5f8] bg-[#f3f9ff] text-[#4b6177] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1] sm:hidden"
            aria-label="AI 플래너"
          >
            <LayoutDashboard size={17} />
          </Link>

          {isLoggedIn && isAdmin && (
            <Link
              to="/admin"
              className="hidden rounded-full px-4 py-2 text-[#4b6177] transition hover:bg-[#e2f1ff] hover:text-[#1f8ef1] sm:inline-flex"
            >
              관리자
            </Link>
          )}

          {isLoggedIn ? (
            <>
              <Link
                to="/mypage"
                className="hidden rounded-full px-4 py-2 text-[#4b6177] transition hover:bg-[#e2f1ff] hover:text-[#1f8ef1] sm:inline-flex"
              >
                내 일정
              </Link>
              <Link
                to="/mypage"
                className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[#d1e5f8] bg-[#f3f9ff] text-[#4b6177] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1] sm:hidden"
                aria-label="내 일정"
              >
                <UserRound size={17} />
              </Link>

              {isAdmin && (
                <Link
                  to="/admin"
                  className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[#d1e5f8] bg-[#f3f9ff] text-[#4b6177] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1] sm:hidden"
                  aria-label="관리자"
                >
                  <ShieldCheck size={17} />
                </Link>
              )}

              <button
                onClick={handleLogout}
                className="ml-1 inline-flex items-center gap-1 rounded-full border border-[#d1e5f8] bg-[#f3f9ff] px-3 py-2 text-[#3c5874] transition hover:border-[#0ea5e9] hover:text-[#0b78ac] sm:px-4"
              >
                <LogOut size={15} />
                <span className="hidden sm:inline">로그아웃</span>
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="ml-1 rounded-full bg-[#1f8ef1] px-4 py-2 text-white shadow-[0_10px_22px_rgba(31,142,241,0.28)] transition hover:-translate-y-0.5 hover:bg-[#1878cc] sm:px-5"
            >
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
            <Route path="/signup" element={<Signup />} />
            <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
            <Route
              path="/planner"
              element={(
                <ProtectedRoute>
                  <Planner />
                </ProtectedRoute>
              )}
            />
            <Route
              path="/mypage"
              element={(
                <ProtectedRoute>
                  <MyPage />
                </ProtectedRoute>
              )}
            />
            <Route
              path="/plans/:id"
              element={(
                <ProtectedRoute>
                  <PlanDetail />
                </ProtectedRoute>
              )}
            />
            <Route
              path="/admin"
              element={(
                <ProtectedRoute>
                  <Admin />
                </ProtectedRoute>
              )}
            />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
