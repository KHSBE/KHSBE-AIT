import { BrowserRouter as Router, Routes, Route, Link, useNavigate, Navigate } from 'react-router-dom';
import { LayoutDashboard, UserRound, LogOut } from 'lucide-react';
import Home from './pages/Home';
import Planner from './pages/Planner';
import Login from './pages/Login';
import Signup from './pages/Signup';
import OAuth2Redirect from './pages/OAuth2Redirect';
import MyPage from './pages/MyPage';
import PlanDetail from './pages/PlanDetail';

function ProtectedRoute({ children }) {
  const token = localStorage.getItem('accessToken');

  if (!token) {
    alert('로그인이 필요한 서비스입니다! 🔐');
    return <Navigate to="/login" replace />;
  }
  return children;
}

function Navigation() {
  const navigate = useNavigate();
  const token = localStorage.getItem('accessToken');
  const isLoggedIn = !!token;

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    alert('로그아웃 되었습니다. 👋');
    navigate('/');
  };

  return (
      <nav className="sticky top-0 z-50 border-b border-[#d9cfbf]/70 bg-[#fffdf8]/85 backdrop-blur-xl">
        <div className="mx-auto flex h-[76px] w-full max-w-7xl items-center justify-between px-4 md:px-8">
          <Link to="/" className="group flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-[#ff6a3d] shadow-[0_0_0_6px_rgba(255,106,61,0.15)] transition group-hover:scale-110" />
            <span className="serif-display text-[30px] leading-none tracking-tight text-[#1a212c] md:text-3xl">Compass</span>
          </Link>

          <div className="flex items-center gap-1 text-sm font-semibold md:gap-2">
            <Link to="/planner" className="hidden rounded-full px-4 py-2 text-[#586171] transition hover:bg-[#ece5d8] hover:text-[#0f7a80] sm:inline-flex">AI 플래너</Link>
            <Link to="/planner" className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[#d7ccbb] bg-[#fff8ed] text-[#5a4d3b] transition hover:border-[#0f7a80] hover:text-[#0f7a80] sm:hidden" aria-label="AI 플래너">
              <LayoutDashboard size={17} />
            </Link>

            {isLoggedIn ? (
              <>
                <Link to="/mypage" className="hidden rounded-full px-4 py-2 text-[#586171] transition hover:bg-[#ece5d8] hover:text-[#0f7a80] sm:inline-flex">마이페이지</Link>
                <Link to="/mypage" className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[#d7ccbb] bg-[#fff8ed] text-[#5a4d3b] transition hover:border-[#0f7a80] hover:text-[#0f7a80] sm:hidden" aria-label="마이페이지">
                  <UserRound size={17} />
                </Link>
                <button onClick={handleLogout} className="ml-1 inline-flex items-center gap-1 rounded-full border border-[#d7ccbb] bg-[#fff8ed] px-3 py-2 text-[#55493a] transition hover:border-[#ff6a3d] hover:text-[#c0502a] sm:px-4">
                  <LogOut size={15} />
                  <span className="hidden sm:inline">로그아웃</span>
                </button>
              </>
            ) : (
              <Link to="/login" className="ml-1 rounded-full bg-[#0f7a80] px-4 py-2 text-white shadow-[0_10px_20px_rgba(15,122,128,0.25)] transition hover:-translate-y-0.5 hover:bg-[#0c686d] sm:px-5">로그인</Link>
            )}
          </div>
        </div>
      </nav>
  );
}

function App() {
  return (
      <Router>
        <div className="page-shell text-[#1a212c]">
          <Navigation />
          <main className="relative z-10">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/signup" element={<Signup />} />
              <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />

              <Route path="/planner" element={<ProtectedRoute><Planner /></ProtectedRoute>} />
              <Route path="/mypage" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
              <Route path="/plans/:id" element={<ProtectedRoute><PlanDetail /></ProtectedRoute>} />
            </Routes>
          </main>
        </div>
      </Router>
  );
}

export default App;
