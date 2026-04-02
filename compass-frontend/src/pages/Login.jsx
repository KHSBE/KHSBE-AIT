import { Link } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import { GOOGLE_OAUTH2_URL } from '../utils/api';

export default function Login() {
  const handleGoogleLogin = () => {
    window.location.href = GOOGLE_OAUTH2_URL;
  };

  return (
    <div className="flex min-h-[calc(100vh-76px)] items-center justify-center px-4 py-8">
      <div className="grid w-full max-w-5xl overflow-hidden rounded-[34px] border border-[#cfe4f7] bg-[#f9fdff]/95 shadow-[0_30px_76px_rgba(68,130,189,0.18)] lg:grid-cols-[1.05fr,1fr]">
        <div className="hidden bg-gradient-to-br from-[#1f8ef1] via-[#2aa5f8] to-[#0ea5e9] p-10 text-white lg:block">
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-white/80">Compass Identity</p>
          <h2 className="serif-display mt-6 text-5xl leading-tight">Plan Less. Travel Better.</h2>
          <p className="mt-6 max-w-sm text-sm leading-relaxed text-white/85">
            로그인하면 AI 플래너를 바로 사용할 수 있습니다.
            국내/해외 여행 일정을 간단한 입력으로 빠르게 만들어 보세요.
          </p>
        </div>

        <div className="p-8 md:p-10">
          <p className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.16em] text-[#4d7fa8]">
            <Sparkles size={14} /> Welcome Back
          </p>
          <h1 className="mt-3 text-3xl font-black text-[#18324d]">로그인</h1>
          <p className="mt-2 text-sm text-[#4b6177]">Google 계정으로 바로 로그인하고 여행 계획을 시작하세요.</p>

          <button
            onClick={handleGoogleLogin}
            className="mt-8 flex w-full items-center justify-center gap-2 rounded-2xl border border-[#cfe4f7] bg-white px-5 py-3.5 text-sm font-bold text-[#2f4459] transition hover:-translate-y-0.5 hover:border-[#1f8ef1] hover:shadow-[0_10px_24px_rgba(31,142,241,0.18)]"
          >
            <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="h-5 w-5" />
            Google 계정으로 계속하기
          </button>

          <p className="mt-8 text-center text-sm text-[#4b6177]">
            아직 계정이 없나요?{' '}
            <Link to="/signup" className="font-bold text-[#1f8ef1] underline-offset-4 hover:underline">
              회원가입
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
