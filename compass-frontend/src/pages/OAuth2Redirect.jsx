import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function OAuth2Redirect() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');

    if (token && refreshToken) {
      localStorage.setItem('accessToken', token);
      localStorage.setItem('refreshToken', refreshToken);
      navigate('/planner');
      return;
    }

    alert('로그인 처리에 실패했습니다. 다시 시도해 주세요.');
    navigate('/login');
  }, [searchParams, navigate]);

  return (
    <div className="flex min-h-[calc(100vh-76px)] items-center justify-center px-4">
      <div className="rounded-3xl border border-[#cfe4f7] bg-[#f9fdff]/95 px-8 py-10 text-center shadow-[0_24px_56px_rgba(68,130,189,0.18)]">
        <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#4d7fa8]">OAuth Redirect</p>
        <h2 className="mt-3 text-2xl font-black text-[#1f3550]">로그인 처리 중입니다</h2>
        <p className="mt-2 animate-pulse text-sm text-[#4b6177]">잠시만 기다려 주세요...</p>
      </div>
    </div>
  );
}
