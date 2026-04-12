import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, Loader2, Compass, Plus, AlertCircle, Trash2 } from 'lucide-react';
import { fetchWithAuth, buildApiUrl } from '../utils/api';

export default function MyPage() {
  const [plans, setPlans] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [deletingPlanId, setDeletingPlanId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

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

  const openDeleteModal = (plan) => {
    setDeleteTarget(plan);
  };

  const closeDeleteModal = () => {
    if (deletingPlanId) return;
    setDeleteTarget(null);
  };

  const confirmDeletePlan = async () => {
    if (!deleteTarget) return;

    const planId = deleteTarget.id;
    setDeletingPlanId(planId);
    try {
      const response = await fetchWithAuth(buildApiUrl(`/api/plans/${planId}`), {
        method: 'DELETE'
      });

      if (!response.ok) {
        let message = '일정 삭제에 실패했습니다.';
        try {
          const errorData = await response.json();
          message = errorData?.message || errorData?.error || message;
        } catch {
          // ignore parse error
        }
        throw new Error(message);
      }

      setPlans((prev) => prev.filter((plan) => plan.id !== planId));
      setDeleteTarget(null);
    } catch (error) {
      console.error('일정 삭제 실패:', error);
      alert(error?.message || '일정 삭제 중 오류가 발생했습니다.');
    } finally {
      setDeletingPlanId(null);
    }
  };

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
              <article
                key={plan.id}
                className="group overflow-hidden rounded-3xl border border-[#d6e9fa] bg-white shadow-[0_12px_34px_rgba(68,130,189,0.12)] transition-all hover:-translate-y-1.5 hover:shadow-[0_18px_42px_rgba(68,130,189,0.18)]"
              >
                <Link to={`/plans/${plan.id}`} className="block">
                  <div className="relative flex h-40 items-center justify-center overflow-hidden bg-gradient-to-br from-[#1f8ef1] to-[#0ea5e9]">
                    <div className="absolute inset-0 bg-black/10 transition duration-300 group-hover:bg-black/0" />
                    <span className="text-3xl font-black tracking-[0.08em] text-white drop-shadow-md transition duration-500 group-hover:scale-110">
                      {plan.region}
                    </span>
                  </div>

                  <div className="p-5 pb-3">
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

                <div className="px-5 pb-5">
                  <button
                    type="button"
                    onClick={() => openDeleteModal(plan)}
                    disabled={deletingPlanId === plan.id}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-2xl border border-[#f3caca] bg-[#fff6f6] px-4 py-2.5 text-sm font-black text-[#b54747] transition hover:border-[#ef8f8f] hover:bg-[#fff0f0] disabled:cursor-wait disabled:opacity-60"
                  >
                    {deletingPlanId === plan.id ? (
                      <Loader2 size={16} className="animate-spin" />
                    ) : (
                      <Trash2 size={16} />
                    )}
                    일정 삭제
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      {deleteTarget && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#102033]/45 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl border border-[#d6e9fa] bg-white p-6 shadow-[0_24px_70px_rgba(15,35,56,0.28)]">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff0f0] text-[#b54747]">
              <Trash2 size={22} />
            </div>
            <h2 className="text-xl font-black text-[#1f3550]">일정을 삭제할까요?</h2>
            <p className="mt-2 text-sm leading-relaxed text-[#4b6177]">
              <span className="font-bold text-[#1f3550]">{deleteTarget.title}</span> 일정이 삭제됩니다.
              삭제한 일정은 다시 복구할 수 없습니다.
            </p>

            <div className="mt-6 flex flex-col gap-2 sm:flex-row">
              <button
                type="button"
                onClick={closeDeleteModal}
                disabled={!!deletingPlanId}
                className="flex-1 rounded-2xl border border-[#d6e9fa] bg-[#f8fcff] px-4 py-3 text-sm font-black text-[#4b6177] transition hover:bg-[#eef7ff] disabled:opacity-60"
              >
                취소
              </button>
              <button
                type="button"
                onClick={confirmDeletePlan}
                disabled={!!deletingPlanId}
                className="flex-1 rounded-2xl bg-[#e54848] px-4 py-3 text-sm font-black text-white shadow-[0_12px_24px_rgba(229,72,72,0.24)] transition hover:bg-[#c93636] disabled:cursor-wait disabled:opacity-70"
              >
                {deletingPlanId === deleteTarget.id ? (
                  <span className="inline-flex items-center gap-2">
                    <Loader2 size={16} className="animate-spin" />
                    삭제 중
                  </span>
                ) : (
                  '삭제하기'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
