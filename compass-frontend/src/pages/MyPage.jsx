import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, Loader2, Compass, Plus } from 'lucide-react';
// ★ 토큰 자동 갱신 마법 함수 가져오기!
import { fetchWithAuth, buildApiUrl } from '../utils/api';

export default function MyPage() {
    const [plans, setPlans] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchMyPlans = async () => {
            try {
                // 헤더 셋업은 필요시 fetchWithAuth 쪽 옵션으로 주거나, 함수 안에서 처리하게 맡깁니다.
                // ★ fetch -> fetchWithAuth 로 변경!
                const response = await fetchWithAuth(buildApiUrl('/api/plans'), {
                    headers: { 'Content-Type': 'application/json' }
                });

                if (response.ok) {
                    const data = await response.json();
                    setPlans(data.data || []);
                } else {
                    console.warn("목록 조회 실패! UI 테스트용 임시 데이터를 표시합니다.");
                    setPlans([
                        { id: 1, title: '후쿠오카 힐링 및 미식 나홀로 여행', region: '후쿠오카', startDate: '2026-03-23', endDate: '2026-03-26' },
                        { id: 2, title: '오사카 핫플 정복 2박 3일', region: '오사카', startDate: '2026-04-10', endDate: '2026-04-12' }
                    ]);
                }
            } catch (error) {
                console.error("서버 연결 에러:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchMyPlans();
    }, []);

    if (isLoading) {
        return <div className="flex h-[calc(100vh-76px)] items-center justify-center"><Loader2 className="animate-spin text-[#0f7a80]" size={40} /></div>;
    }

    return (
        <div className="mx-auto min-h-[calc(100vh-76px)] w-full max-w-6xl p-4 md:p-8">
            <div className="reveal-up rounded-[30px] border border-[#d9cfbf] bg-[#fffdf8]/95 p-6 shadow-[0_20px_48px_rgba(82,72,46,0.12)] md:p-8">
                <div className="mb-8 border-b border-[#ddd2bf] pb-4">
                    <div className="flex items-end justify-between gap-3">
                        <div>
                            <h1 className="flex items-center gap-3 text-2xl font-black text-[#1a212c] sm:text-3xl">
                                <Compass className="text-[#0f7a80]" size={30} />
                                내 여행 일정
                            </h1>
                            <p className="mt-2 text-sm text-[#5e6673]">저장된 일정을 열어 상세 동선과 지도 정보를 바로 확인하세요.</p>
                        </div>
                        <Link to="/planner" className="hidden items-center gap-2 rounded-full bg-[#0f7a80] px-5 py-2.5 text-sm font-bold text-white shadow-[0_12px_24px_rgba(15,122,128,0.28)] transition hover:-translate-y-0.5 hover:bg-[#0c686d] sm:flex">
                            <Plus size={18} /> 새 일정 만들기
                        </Link>
                    </div>
                    <Link to="/planner" className="mt-4 inline-flex items-center gap-2 rounded-full border border-[#c7e1de] bg-[#def3f1] px-4 py-2 text-sm font-bold text-[#0f7a80] sm:hidden">
                        <Plus size={16} /> 새 일정 만들기
                    </Link>
                </div>

            {plans.length === 0 ? (
                <div className="flex flex-col items-center rounded-3xl border border-[#ddd2bf] bg-[#fff9ef] p-10 text-center sm:p-14">
                    <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-[#d6f0ee]">
                        <MapPin size={32} className="text-[#0f7a80]" />
                    </div>
                    <p className="mb-2 text-xl font-bold text-[#1f2430]">아직 저장된 여행이 없습니다.</p>
                    <p className="mb-6 text-sm text-[#5e6673]">AI 플래너로 첫 일정을 생성하면 여기에 자동으로 쌓입니다.</p>
                    <Link to="/planner" className="rounded-full bg-[#ff6a3d] px-8 py-3.5 text-sm font-bold text-white shadow-[0_12px_24px_rgba(255,106,61,0.3)] transition hover:bg-[#e85a30]">
                        AI 플래너 시작하기
                    </Link>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {plans.map(plan => (
                        <Link to={`/plans/${plan.id}`} key={plan.id} className="group overflow-hidden rounded-3xl border border-[#dbcfbe] bg-white shadow-[0_12px_34px_rgba(74,66,46,0.1)] transition-all hover:-translate-y-1.5 hover:shadow-[0_18px_42px_rgba(74,66,46,0.16)]">
                            <div className="relative flex h-40 items-center justify-center overflow-hidden bg-gradient-to-br from-[#0f7a80] to-[#144f5a]">
                                <div className="absolute inset-0 bg-black/15 transition duration-300 group-hover:bg-black/0" />
                                <span className="text-3xl font-black tracking-[0.08em] text-white drop-shadow-md transition duration-500 group-hover:scale-110">
                                  {plan.region}
                                </span>
                            </div>

                            <div className="p-5">
                                <h3 className="mb-3 truncate text-lg font-bold text-[#1f2430]">{plan.title}</h3>
                                <div className="space-y-2 text-sm font-medium text-[#5e6673]">
                                    <p className="flex items-center gap-2.5 rounded-lg bg-[#f7f1e6] p-2">
                                        <MapPin size={16} className="text-[#0f7a80]" /> {plan.region}
                                    </p>
                                    <p className="flex items-center gap-2.5 rounded-lg bg-[#f7f1e6] p-2">
                                        <Calendar size={16} className="text-[#0f7a80]" /> {plan.startDate} ~ {plan.endDate}
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
