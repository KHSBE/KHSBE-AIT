import { Link } from 'react-router-dom';
import { Map, Plane, Calendar } from 'lucide-react';

export default function Home() {
    return (
        <div className="flex flex-col items-center justify-center py-20 px-4">
            <h1 className="text-5xl font-extrabold text-center mb-6 leading-tight">
                나만의 완벽한 여행, <br />
                <span className="text-blue-600">AI가 1분 만에</span> 짜드립니다
            </h1>
            <p className="text-lg text-gray-500 mb-10 text-center max-w-xl">
                어디로 갈지, 무엇을 할지 고민하지 마세요. Compass AI가 당신의 취향과 동선을 완벽하게 분석하여 맞춤형 일정을 제안합니다.
            </p>

            {/* AI 플래너로 넘어가는 핵심 버튼 */}
            <Link to="/planner" className="px-8 py-4 bg-blue-600 text-white text-xl font-bold rounded-full shadow-lg hover:bg-blue-700 hover:-translate-y-1 transition transform flex items-center gap-2">
                <Plane size={24} />
                AI 플래너 시작하기
            </Link>

            {/* 서비스 특징 소개 (와이어프레임 느낌 살리기) */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mt-24 max-w-5xl w-full">
                <div className="p-6 bg-white rounded-2xl shadow-sm text-center border border-gray-100">
                    <Map className="w-12 h-12 text-blue-500 mx-auto mb-4" />
                    <h3 className="text-xl font-bold mb-2">동선 최적화</h3>
                    <p className="text-gray-500">길에서 버리는 시간 없이, 가장 효율적인 이동 경로를 찾아드려요.</p>
                </div>
                <div className="p-6 bg-white rounded-2xl shadow-sm text-center border border-gray-100">
                    <Calendar className="w-12 h-12 text-blue-500 mx-auto mb-4" />
                    <h3 className="text-xl font-bold mb-2">맞춤형 일정</h3>
                    <p className="text-gray-500">누구와 함께 가는지, 어떤 테마를 원하는지 알려주시면 딱 맞게 짜드려요.</p>
                </div>
                <div className="p-6 bg-white rounded-2xl shadow-sm text-center border border-gray-100">
                    <Plane className="w-12 h-12 text-blue-500 mx-auto mb-4" />
                    <h3 className="text-xl font-bold mb-2">원클릭 저장</h3>
                    <p className="text-gray-500">마음에 드는 일정은 저장하고 언제든 마이페이지에서 꺼내볼 수 있어요.</p>
                </div>
            </div>
        </div>
    );
}