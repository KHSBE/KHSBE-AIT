import { Link } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import { GOOGLE_OAUTH2_URL } from '../utils/api';

export default function Signup() {
    const handleGoogleSignup = () => {
        window.location.href = GOOGLE_OAUTH2_URL;
    };

    return (
        <div className="flex min-h-[calc(100vh-76px)] items-center justify-center px-4 py-8">
            <div className="w-full max-w-md rounded-[32px] border border-[#d9cfbf] bg-[#fffdf8]/95 p-8 shadow-[0_24px_60px_rgba(82,72,46,0.15)] md:p-10">
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8a7c65]">Create Account</p>
                <h1 className="mt-3 text-3xl font-black text-[#1a212c]">구글로 회원가입</h1>
                <p className="mt-2 text-sm text-[#5e6673]">Compass는 Google OAuth만 사용합니다. 처음 로그인하면 계정이 자동 생성됩니다.</p>

                <div className="mt-7 rounded-2xl border border-[#ddd2bf] bg-[#fff8ee] p-4 text-sm text-[#5e6673]">
                    <p className="flex items-center gap-2 font-semibold text-[#4a5565]">
                        <Sparkles size={16} className="text-[#ff6a3d]" />
                        별도 회원가입 폼 없이 바로 시작
                    </p>
                    <p className="mt-2 text-xs leading-relaxed">
                        Google 인증 완료 후 자동으로 회원 정보가 생성되고, 바로 플래너 화면으로 이동합니다.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={handleGoogleSignup}
                    className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl border border-[#d7cbb8] bg-white px-5 py-3.5 text-sm font-bold text-[#2f3542] transition hover:-translate-y-0.5 hover:border-[#0f7a80] hover:shadow-[0_10px_24px_rgba(15,122,128,0.15)]"
                >
                    <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="h-5 w-5" />
                    Google 계정으로 회원가입
                </button>

                <p className="mt-8 text-center text-sm text-[#5e6673]">
                    이미 계정이 있으신가요?{' '}
                    <Link to="/login" className="font-bold text-[#0f7a80] underline-offset-4 hover:underline">
                        로그인
                    </Link>
                </p>
            </div>
        </div>
    );
}
