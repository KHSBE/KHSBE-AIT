import { BrowserRouter as Router, Routes, Route, Link, useNavigate, Navigate } from 'react-router-dom';
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
      // ★ 글래스모피즘(반투명 블러) 적용! 스크롤 내릴 때 뒤에가 살짝 비칩니다.
      <nav className="bg-white/80 backdrop-blur-md border-b border-gray-100 p-4 flex justify-between items-center px-6 md:px-10 sticky top-0 z-50 transition-all duration-300">

        {/* 로고: 그라데이션 컬러로 트렌디하게 변경 */}
        <Link to="/" className="text-2xl font-black text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-indigo-600 tracking-tight hover:opacity-80 transition">
          Compass
        </Link>

        {/* 메뉴 버튼들: 간격과 호버(Hover) 효과를 고급스럽게 다듬음 */}
        <div className="font-bold flex items-center text-[15px] gap-1 md:gap-2">
          <Link to="/planner" className="px-4 py-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-full transition">AI 플래너</Link>

          {isLoggedIn ? (
              <>
                <Link to="/mypage" className="px-4 py-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-full transition">마이페이지</Link>
                <button onClick={handleLogout} className="ml-2 px-5 py-2 bg-gray-100 text-gray-600 rounded-full hover:bg-red-50 hover:text-red-600 transition">로그아웃</button>
              </>
          ) : (
              <Link to="/login" className="ml-2 px-6 py-2 bg-blue-600 text-white rounded-full hover:bg-blue-700 shadow-sm hover:shadow-md transition transform hover:-translate-y-0.5">로그인</Link>
          )}
        </div>
      </nav>
  );
}

function App() {
  return (
      <Router>
        {/* 전체 배경색을 살짝 차분한 회색(gray-50)으로 깔아줍니다 */}
        <div className="min-h-screen bg-gray-50 text-gray-800">
          <Navigation />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />

            <Route path="/planner" element={<ProtectedRoute><Planner /></ProtectedRoute>} />
            <Route path="/mypage" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
            <Route path="/plans/:id" element={<ProtectedRoute><PlanDetail /></ProtectedRoute>} />
          </Routes>
        </div>
      </Router>
  );
}

export default App;