import { Link } from 'react-router-dom';
import { Map, Plane, CalendarDays } from 'lucide-react';

export default function Home() {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-20">
      <section className="reveal-up relative overflow-hidden rounded-[28px] border border-[#d3e8fb] bg-[#f9fdff]/95 px-5 py-8 shadow-[0_26px_70px_rgba(63,128,197,0.16)] md:rounded-[34px] md:px-12 md:py-14">
        <div className="absolute -left-24 top-10 h-64 w-64 rounded-full bg-[#38bdf8]/20 blur-3xl" />
        <div className="absolute -right-12 bottom-6 h-72 w-72 rounded-full bg-[#60a5fa]/24 blur-3xl" />

        <div className="relative z-10">
          <h1 className="serif-display max-w-3xl text-[34px] leading-tight text-[#13263a] md:text-6xl">
            여행 일정 초안을
            <span className="ml-2 text-[#1f8ef1]">실행 가능한 동선</span>으로.
          </h1>

          <p className="mt-6 max-w-2xl text-base leading-relaxed text-[#4b6177] md:text-lg">
            Compass는 취향, 이동 시간, 여행 스타일을 반영해 일정의 뼈대를 빠르게 만들어줍니다.
            국내 여행도 보기 쉬운 한국어 중심으로 정리하고, 이후에 직접 수정하며 완성할 수 있습니다.
          </p>

          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Link
              to="/planner"
              className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-[#1f8ef1] px-7 py-3.5 text-base font-bold text-white shadow-[0_16px_30px_rgba(31,142,241,0.3)] transition hover:-translate-y-0.5 hover:bg-[#1878cc] sm:w-auto"
            >
              <Plane size={18} />
              AI 플래너 시작하기
            </Link>
            <Link
              to="/mypage"
              className="inline-flex w-full items-center justify-center rounded-full border border-[#cfe3f7] bg-[#f2f9ff] px-7 py-3.5 text-base font-bold text-[#335a7d] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1] sm:w-auto"
            >
              내 일정 보기
            </Link>
          </div>
        </div>
      </section>

      <section className="mt-7 grid grid-cols-1 gap-4 md:mt-10 md:grid-cols-3 md:gap-5">
        <article className="reveal-up rounded-3xl border border-[#d7e9fa] bg-[#f9fdff]/95 p-6 shadow-[0_12px_30px_rgba(80,126,173,0.12)]">
          <Map className="mb-4 h-11 w-11 rounded-2xl bg-[#dff1ff] p-2.5 text-[#1f8ef1]" />
          <h3 className="text-lg font-extrabold text-[#1f3550]">동선 중심 추천</h3>
          <p className="mt-2 text-sm leading-relaxed text-[#4b6177]">
            장소를 나열하는 수준이 아니라, 이동 부담을 고려한 순서로 하루를 구성합니다.
          </p>
        </article>

        <article className="reveal-up rounded-3xl border border-[#d7e9fa] bg-[#f9fdff]/95 p-6 shadow-[0_12px_30px_rgba(80,126,173,0.12)] [animation-delay:120ms]">
          <CalendarDays className="mb-4 h-11 w-11 rounded-2xl bg-[#eaf6ff] p-2.5 text-[#0ea5e9]" />
          <h3 className="text-lg font-extrabold text-[#1f3550]">입력 기반 일정 생성</h3>
          <p className="mt-2 text-sm leading-relaxed text-[#4b6177]">
            여행지, 기간, 동행, 스타일을 입력하면 바로 확인 가능한 일정 초안을 만듭니다.
          </p>
        </article>

        <article className="reveal-up rounded-3xl border border-[#d7e9fa] bg-[#f9fdff]/95 p-6 shadow-[0_12px_30px_rgba(80,126,173,0.12)] [animation-delay:220ms]">
          <Plane className="mb-4 h-11 w-11 rounded-2xl bg-[#dff1ff] p-2.5 text-[#1f8ef1]" />
          <h3 className="text-lg font-extrabold text-[#1f3550]">바로 실행 가능한 형태</h3>
          <p className="mt-2 text-sm leading-relaxed text-[#4b6177]">
            생성한 일정을 저장하고 상세 페이지에서 지도 기반으로 확인할 수 있습니다.
          </p>
        </article>
      </section>
    </div>
  );
}
