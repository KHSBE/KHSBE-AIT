import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function OAuth2Redirect() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const token = searchParams.get('token');
        const refreshToken = searchParams.get('refreshToken'); // ★ 추가!

        if (token && refreshToken) {
            // 두 토큰 모두 저장!
            localStorage.setItem('accessToken', token);
            localStorage.setItem('refreshToken', refreshToken);
            navigate('/planner');
        } else {
            alert('로그인에 실패했습니다.');
            navigate('/login');
        }
    }, [searchParams, navigate]);

    return (
        <div className="flex min-h-[calc(100vh-76px)] items-center justify-center px-4">
            <div className="rounded-3xl border border-[#d9cfbf] bg-[#fffdf8]/95 px-8 py-10 text-center shadow-[0_24px_56px_rgba(82,72,46,0.15)]">
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8a7c65]">OAuth Redirect</p>
                <h2 className="mt-3 text-2xl font-black text-[#1f2430]">로그인 처리 중입니다</h2>
                <p className="mt-2 animate-pulse text-sm text-[#5e6673]">잠시만 기다려주세요...</p>
            </div>
        </div>
    );
}
