import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, Loader2, Compass, Plus } from 'lucide-react';
// ★ 토큰 자동 갱신 마법 함수 가져오기!
import { fetchWithAuth } from '../utils/api';

export default function MyPage() {
    const [plans, setPlans] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchMyPlans = async () => {
            try {
                // 헤더 셋업은 필요시 fetchWithAuth 쪽 옵션으로 주거나, 함수 안에서 처리하게 맡깁니다.
                // ★ fetch -> fetchWithAuth 로 변경!
                const response = await fetchWithAuth('http://localhost:8080/api/plans', {
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
        return <div className="flex justify-center items-center h-[calc(100vh-80px)]"><Loader2 className="animate-spin text-blue-500" size={40} /></div>;
    }

    return (
        <div className="max-w-5xl mx-auto p-6 min-h-[calc(100vh-80px)] animate-in fade-in duration-500">
            <div className="flex justify-between items-end mb-8 border-b pb-4">
                <div>
                    <h1 className="text-3xl font-black text-gray-800 flex items-center gap-3">
                        <Compass className="text-blue-600" size={32} />
                        내 여행 일정
                    </h1>
                    <p className="text-gray-500 mt-2">지금까지 Compass AI와 함께 계획한 여행들입니다.</p>
                </div>
                <Link to="/planner" className="hidden sm:flex items-center gap-2 px-5 py-2.5 bg-gray-900 text-white font-bold rounded-xl hover:bg-gray-800 transition">
                    <Plus size={18} /> 새 일정 만들기
                </Link>
            </div>

            {plans.length === 0 ? (
                <div className="bg-white rounded-3xl p-16 text-center border border-gray-100 shadow-sm flex flex-col items-center">
                    <div className="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center mb-4">
                        <MapPin size={32} className="text-blue-500" />
                    </div>
                    <p className="text-gray-800 font-bold text-xl mb-2">아직 계획된 여행이 없네요!</p>
                    <p className="text-gray-500 mb-6">첫 번째 여행을 AI와 함께 1분 만에 짜보세요.</p>
                    <Link to="/planner" className="px-8 py-3.5 bg-blue-600 text-white font-bold rounded-full hover:bg-blue-700 transition shadow-md">
                        AI 플래너 시작하기
                    </Link>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {plans.map(plan => (
                        <Link to={`/plans/${plan.id}`} key={plan.id} className="bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-xl transition-all transform hover:-translate-y-1 overflow-hidden group">
                            <div className="h-40 bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center relative overflow-hidden">
                                <div className="absolute inset-0 bg-black/10 group-hover:bg-black/0 transition duration-300"></div>
                                <span className="text-white font-black text-3xl tracking-wider drop-shadow-md transform group-hover:scale-110 transition duration-500">
                                  {plan.region}
                                </span>
                            </div>

                            <div className="p-5">
                                <h3 className="text-lg font-bold text-gray-800 mb-3 truncate">{plan.title}</h3>
                                <div className="space-y-2 text-sm font-medium text-gray-500">
                                    <p className="flex items-center gap-2.5 bg-gray-50 p-2 rounded-lg">
                                        <MapPin size={16} className="text-blue-500" /> {plan.region}
                                    </p>
                                    <p className="flex items-center gap-2.5 bg-gray-50 p-2 rounded-lg">
                                        <Calendar size={16} className="text-blue-500" /> {plan.startDate} ~ {plan.endDate}
                                    </p>
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );
}