import { Link } from 'react-router-dom';
import { Mail, Lock } from 'lucide-react';
import { GOOGLE_OAUTH2_URL } from '../utils/api';

export default function Login() {
    const handleGoogleLogin = () => {
        window.location.href = GOOGLE_OAUTH2_URL;
    };

    return (
        <div className="flex min-h-[calc(100vh-76px)] items-center justify-center px-4 py-8">
            <div className="grid w-full max-w-5xl overflow-hidden rounded-[34px] border border-[#d9cfbf] bg-[#fffdf8]/95 shadow-[0_28px_70px_rgba(82,72,46,0.15)] lg:grid-cols-[1.05fr,1fr]">
                <div className="hidden bg-gradient-to-br from-[#0f7a80] via-[#0f7a80] to-[#14656f] p-10 text-white lg:block">
                    <p className="text-xs font-bold uppercase tracking-[0.18em] text-white/70">Compass Identity</p>
                    <h2 className="serif-display mt-6 text-5xl leading-tight">Plan Less. Travel Better.</h2>
                    <p className="mt-6 max-w-sm text-sm leading-relaxed text-white/80">
                        로그인 후 바로 AI 플래너를 사용해 일정 초안을 만들고, 지도 기반으로 여행 동선을 빠르게 확인할 수 있습니다.
                    </p>
                    <div className="mt-10 space-y-3 text-sm">
                        <p className="flex items-center gap-2 rounded-xl border border-white/20 bg-white/10 px-3 py-2">
                            <Mail size={16} /> OAuth2 기반 간편 로그인
                        </p>
                        <p className="flex items-center gap-2 rounded-xl border border-white/20 bg-white/10 px-3 py-2">
                            <Lock size={16} /> JWT 인증으로 세션 유지
                        </p>
                    </div>
                </div>

                <div className="p-8 md:p-10">
                    <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8a7c65]">Welcome Back</p>
                    <h1 className="mt-3 text-3xl font-black text-[#1a212c]">로그인</h1>
                    <p className="mt-2 text-sm text-[#5e6673]">구글 계정으로 빠르게 로그인하고 여행 계획을 이어서 관리하세요.</p>

                    <button onClick={handleGoogleLogin} className="mt-8 flex w-full items-center justify-center gap-2 rounded-2xl border border-[#d7cbb8] bg-white px-5 py-3.5 text-sm font-bold text-[#2f3542] transition hover:-translate-y-0.5 hover:border-[#0f7a80] hover:shadow-[0_10px_24px_rgba(15,122,128,0.15)]">
                        <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="h-5 w-5" />
                        Google 계정으로 계속하기
                    </button>

                    <div className="mt-8">
                        <div className="relative">
                            <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-[#ddd2bf]" /></div>
                            <div className="relative flex justify-center">
                                <span className="bg-[#fffdf8] px-3 text-xs font-semibold text-[#8a7c65]">이메일 로그인은 준비 중</span>
                            </div>
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={() => alert('이메일 로그인은 준비 중입니다! 구글 로그인을 이용해주세요.')}
                        className="mt-6 w-full rounded-2xl bg-[#1e2430] py-3.5 text-sm font-bold text-white transition hover:bg-[#111827]"
                    >
                        이메일로 로그인
                    </button>

                    <p className="mt-8 text-center text-sm text-[#5e6673]">
                        아직 계정이 없으신가요?{' '}
                        <Link to="/signup" className="font-bold text-[#0f7a80] underline-offset-4 hover:underline">
                            회원가입
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}
