import { Link } from 'react-router-dom';
import { Map, Plane, Calendar } from 'lucide-react';

export default function Home() {
    return (
        <div className="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-20">
            <div className="reveal-up relative overflow-hidden rounded-[28px] border border-[#ddd2bf] bg-[#fffdf8]/90 px-5 py-8 shadow-[0_30px_80px_rgba(91,80,52,0.12)] md:rounded-[34px] md:px-12 md:py-14">
                <div className="absolute -left-20 top-16 h-52 w-52 rounded-full bg-[#ff6a3d]/15 blur-3xl" />
                <div className="absolute -right-12 bottom-10 h-64 w-64 rounded-full bg-[#0f7a80]/18 blur-3xl" />

                <div className="relative z-10">
                    <p className="mb-5 inline-flex items-center rounded-full border border-[#d7cbb8] bg-[#f7efe1] px-4 py-2 text-xs font-bold uppercase tracking-[0.18em] text-[#6b5e4b]">
                        AI Personal Travel Studio
                    </p>

                    <h1 className="serif-display max-w-3xl text-[34px] leading-tight text-[#1a212c] md:text-6xl">
                        여행의 첫 초안을
                        <span className="ml-1 text-[#0f7a80] md:ml-2">가장 실용적인 동선</span>으로.
                    </h1>

                    <p className="mt-6 max-w-2xl text-base leading-relaxed text-[#5e6673] md:text-lg">
                        Compass는 취향, 이동 거리, 일정 밀도를 함께 고려해 여행 계획을 만들어 줍니다.
                        마음에 들지 않으면 바로 수정하고, 일정은 지도에서 바로 확인할 수 있습니다.
                    </p>

                    <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                        <Link to="/planner" className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-[#0f7a80] px-7 py-3.5 text-base font-bold text-white shadow-[0_16px_30px_rgba(15,122,128,0.3)] transition hover:-translate-y-0.5 hover:bg-[#0c686d] sm:w-auto">
                            <Plane size={18} />
                            AI 플래너 시작하기
                        </Link>
                        <Link to="/mypage" className="inline-flex w-full items-center justify-center rounded-full border border-[#d8ccba] bg-[#fffaf0] px-7 py-3.5 text-base font-bold text-[#5f4d3e] transition hover:border-[#ff6a3d] hover:text-[#c0502a] sm:w-auto">
                            저장된 일정 보기
                        </Link>
                    </div>
                </div>
            </div>

            <div className="mt-7 grid grid-cols-1 gap-4 md:mt-10 md:gap-5 md:grid-cols-3">
                <div className="reveal-up rounded-3xl border border-[#dfd3c1] bg-[#fffdf8]/95 p-6 shadow-[0_12px_36px_rgba(77,68,45,0.08)]">
                    <Map className="mb-4 h-11 w-11 rounded-2xl bg-[#d6f0ee] p-2.5 text-[#0f7a80]" />
                    <h3 className="text-lg font-extrabold text-[#1f2430]">동선 중심 추천</h3>
                    <p className="mt-2 text-sm leading-relaxed text-[#616978]">장소를 예쁘게 나열하는 대신, 실제 이동이 편한 순서로 일정을 구성합니다.</p>
                </div>
                <div className="reveal-up rounded-3xl border border-[#dfd3c1] bg-[#fffdf8]/95 p-6 shadow-[0_12px_36px_rgba(77,68,45,0.08)] [animation-delay:120ms]">
                    <Calendar className="mb-4 h-11 w-11 rounded-2xl bg-[#ffe4d9] p-2.5 text-[#ff6a3d]" />
                    <h3 className="text-lg font-extrabold text-[#1f2430]">대화형 일정 설계</h3>
                    <p className="mt-2 text-sm leading-relaxed text-[#616978]">대화로 방향을 잡고, 세부 정보는 모달 입력으로 정교하게 완성할 수 있습니다.</p>
                </div>
                <div className="reveal-up rounded-3xl border border-[#dfd3c1] bg-[#fffdf8]/95 p-6 shadow-[0_12px_36px_rgba(77,68,45,0.08)] [animation-delay:220ms]">
                    <Plane className="mb-4 h-11 w-11 rounded-2xl bg-[#d6f0ee] p-2.5 text-[#0f7a80]" />
                    <h3 className="text-lg font-extrabold text-[#1f2430]">지도 연동 확인</h3>
                    <p className="mt-2 text-sm leading-relaxed text-[#616978]">생성된 일정은 장소 좌표와 함께 저장되어 계획을 바로 지도 위에서 확인할 수 있습니다.</p>
                </div>
            </div>
        </div>
    );
}
