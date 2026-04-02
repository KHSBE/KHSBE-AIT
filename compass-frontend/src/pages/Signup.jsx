import { Link } from 'react-router-dom';
import { Sparkles, ShieldCheck } from 'lucide-react';
import { GOOGLE_OAUTH2_URL } from '../utils/api';

export default function Signup() {
  const handleGoogleSignup = () => {
    window.location.href = GOOGLE_OAUTH2_URL;
  };

  return (
    <div className="flex min-h-[calc(100vh-76px)] items-center justify-center px-4 py-8">
      <div className="w-full max-w-md rounded-[32px] border border-[#cfe4f7] bg-[#f9fdff]/95 p-8 shadow-[0_24px_60px_rgba(68,130,189,0.18)] md:p-10">
        <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#4d7fa8]">Create Account</p>
        <h1 className="mt-3 text-3xl font-black text-[#18324d]">Google로 회원가입</h1>
        <p className="mt-2 text-sm text-[#4b6177]">
          Compass는 Google OAuth 로그인을 사용합니다. 처음 로그인하면 계정이 자동 생성됩니다.
        </p>

        <div className="mt-7 rounded-2xl border border-[#d7e8f8] bg-[#edf7ff] p-4 text-sm text-[#406382]">
          <p className="flex items-center gap-2 font-semibold text-[#2f5475]">
            <Sparkles size={16} className="text-[#1f8ef1]" />
            별도 정보 입력 없이 바로 시작
          </p>
          <p className="mt-2 text-xs leading-relaxed">
            Google 인증 이후 자동으로 회원 정보가 등록되고, 바로 플래너 화면으로 이동합니다.
          </p>
          <p className="mt-2 flex items-center gap-1 text-xs font-semibold text-[#2f5475]">
            <ShieldCheck size={14} /> 인증/로그인 처리 일원화
          </p>
        </div>

        <button
          type="button"
          onClick={handleGoogleSignup}
          className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl border border-[#cfe4f7] bg-white px-5 py-3.5 text-sm font-bold text-[#2f4459] transition hover:-translate-y-0.5 hover:border-[#1f8ef1] hover:shadow-[0_10px_24px_rgba(31,142,241,0.18)]"
        >
          <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="h-5 w-5" />
          Google 계정으로 회원가입
        </button>

        <p className="mt-8 text-center text-sm text-[#4b6177]">
          이미 계정이 있나요?{' '}
          <Link to="/login" className="font-bold text-[#1f8ef1] underline-offset-4 hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
