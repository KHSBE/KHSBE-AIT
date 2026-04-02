import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, Loader2, Compass, Plus, AlertCircle } from 'lucide-react';
import { fetchWithAuth, buildApiUrl } from '../utils/api';

export default function MyPage() {
  const [plans, setPlans] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const fetchMyPlans = async () => {
      try {
        const response = await fetchWithAuth(buildApiUrl('/api/plans'), {
          headers: { 'Content-Type': 'application/json' }
        });

        if (!response.ok) {
          throw new Error('일정 목록을 불러오지 못했습니다.');
        }

        const result = await response.json();
        setPlans(result?.data || []);
      } catch (error) {
        console.error('내 일정 조회 실패:', error);
        setErrorMessage('일정 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchMyPlans();
  }, []);

  if (isLoading) {
    return (
      <div className="flex h-[calc(100vh-76px)] items-center justify-center">
        <Loader2 className="animate-spin text-[#1f8ef1]" size={40} />
      </div>
    );
  }

  return (
    <div className="mx-auto min-h-[calc(100vh-76px)] w-full max-w-6xl p-4 md:p-8">
      <div className="reveal-up rounded-[30px] border border-[#d3e8fb] bg-[#f9fdff]/95 p-6 shadow-[0_20px_48px_rgba(68,130,189,0.16)] md:p-8">
        <div className="mb-8 border-b border-[#d6e9fa] pb-4">
          <div className="flex items-end justify-between gap-3">
            <div>
              <h1 className="flex items-center gap-3 text-2xl font-black text-[#1f3550] sm:text-3xl">
                <Compass className="text-[#1f8ef1]" size={30} />
                내 여행 일정
              </h1>
              <p className="mt-2 text-sm text-[#4b6177]">저장된 일정을 확인하고 상세 동선/장소 정보를 검토하세요.</p>
            </div>
            <Link
              to="/planner"
              className="hidden items-center gap-2 rounded-full bg-[#1f8ef1] px-5 py-2.5 text-sm font-bold text-white shadow-[0_12px_24px_rgba(31,142,241,0.28)] transition hover:-translate-y-0.5 hover:bg-[#1878cc] sm:flex"
            >
              <Plus size={18} /> 새 일정 만들기
            </Link>
          </div>
          <Link
            to="/planner"
            className="mt-4 inline-flex items-center gap-2 rounded-full border border-[#cce3f8] bg-[#eaf6ff] px-4 py-2 text-sm font-bold text-[#246392] sm:hidden"
          >
            <Plus size={16} /> 새 일정 만들기
          </Link>
        </div>

        {errorMessage ? (
          <div className="flex items-start gap-3 rounded-2xl border border-[#f1c7c7] bg-[#fff5f5] p-4 text-sm text-[#9f2c2c]">
            <AlertCircle size={18} className="mt-0.5 shrink-0" />
            {errorMessage}
          </div>
        ) : plans.length === 0 ? (
          <div className="flex flex-col items-center rounded-3xl border border-[#d6e9fa] bg-[#eff8ff] p-10 text-center sm:p-14">
            <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-[#dff1ff]">
              <MapPin size={32} className="text-[#1f8ef1]" />
            </div>
            <p className="mb-2 text-xl font-bold text-[#1f3550]">아직 저장된 여행이 없습니다.</p>
            <p className="mb-6 text-sm text-[#4b6177]">AI 플래너에서 첫 일정을 만들면 여기에 자동으로 저장됩니다.</p>
            <Link
              to="/planner"
              className="rounded-full bg-[#1f8ef1] px-8 py-3.5 text-sm font-bold text-white shadow-[0_12px_24px_rgba(31,142,241,0.28)] transition hover:bg-[#1878cc]"
            >
              AI 플래너 시작하기
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {plans.map((plan) => (
              <Link
                to={`/plans/${plan.id}`}
                key={plan.id}
                className="group overflow-hidden rounded-3xl border border-[#d6e9fa] bg-white shadow-[0_12px_34px_rgba(68,130,189,0.12)] transition-all hover:-translate-y-1.5 hover:shadow-[0_18px_42px_rgba(68,130,189,0.18)]"
              >
                <div className="relative flex h-40 items-center justify-center overflow-hidden bg-gradient-to-br from-[#1f8ef1] to-[#0ea5e9]">
                  <div className="absolute inset-0 bg-black/10 transition duration-300 group-hover:bg-black/0" />
                  <span className="text-3xl font-black tracking-[0.08em] text-white drop-shadow-md transition duration-500 group-hover:scale-110">
                    {plan.region}
                  </span>
                </div>

                <div className="p-5">
                  <h3 className="mb-3 truncate text-lg font-bold text-[#1f3550]">{plan.title}</h3>
                  <div className="space-y-2 text-sm font-medium text-[#4b6177]">
                    <p className="flex items-center gap-2.5 rounded-lg bg-[#f2f9ff] p-2">
                      <MapPin size={16} className="text-[#1f8ef1]" /> {plan.region}
                    </p>
                    <p className="flex items-center gap-2.5 rounded-lg bg-[#f2f9ff] p-2">
                      <Calendar size={16} className="text-[#1f8ef1]" /> {plan.startDate} ~ {plan.endDate}
                    </p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
