import { Link } from 'react-router-dom';
import { Mail, Lock } from 'lucide-react';

export default function Login() {
    // ★ 구글 로그인 버튼을 눌렀을 때 백엔드로 보내는 함수
    const handleGoogleLogin = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    };

    return (
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-4">
            <div className="bg-white p-8 rounded-3xl shadow-lg border border-gray-100 w-full max-w-md animate-in fade-in zoom-in duration-300">
                <h2 className="text-3xl font-black text-center text-gray-800 mb-2">환영합니다! 👋</h2>
                <p className="text-center text-gray-500 mb-8">Compass와 함께 완벽한 여행을 준비하세요</p>

                {/* 이메일 로그인 폼 (나중을 위해 틀만 유지) */}
                <form className="space-y-5">
                    {/* ... (이전 이메일/비밀번호 input 코드와 동일) ... */}
                    <button type="button" onClick={() => alert('이메일 로그인은 준비 중입니다! 구글 로그인을 이용해주세요.')} className="w-full py-3.5 bg-gray-800 text-white font-bold rounded-xl hover:bg-gray-900 transition shadow-md">
                        이메일로 로그인
                    </button>
                </form>

                <div className="mt-8">
                    <div className="relative">
                        <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-200"></div></div>
                        <div className="relative flex justify-center text-sm"><span className="px-2 bg-white text-gray-500">간편 로그인 (추천)</span></div>
                    </div>

                    <div className="mt-6 flex flex-col gap-3">
                        {/* ★ 구글 로그인 버튼에 onClick 이벤트 연결! */}
                        <button onClick={handleGoogleLogin} className="w-full py-3 border border-gray-200 rounded-xl font-bold text-gray-700 hover:bg-gray-50 transition flex items-center justify-center gap-2">
                            <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="w-5 h-5" />
                            Google 계정으로 계속하기
                        </button>
                    </div>
                </div>

                <p className="mt-8 text-center text-sm text-gray-600">
                    아직 계정이 없으신가요? <Link to="/signup" className="text-blue-600 font-bold hover:underline">회원가입</Link>
                </p>
            </div>
        </div>
    );
}