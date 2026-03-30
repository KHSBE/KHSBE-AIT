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
        <div className="flex h-screen items-center justify-center">
            <h2 className="text-2xl font-bold text-gray-600 animate-pulse">로그인 처리 중입니다... 🚀</h2>
        </div>
    );
}