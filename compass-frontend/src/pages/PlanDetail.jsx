import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { MapPin, Calendar, ChevronLeft, Loader2, BedDouble, Bot } from 'lucide-react';
import { GoogleMap, useJsApiLoader, Marker, Polyline } from '@react-google-maps/api';
// ★ 토큰 자동 갱신 마법 함수 가져오기!
import { fetchWithAuth } from '../utils/api';

export default function PlanDetail() {
    const { id } = useParams();
    const [plan, setPlan] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    const [selectedDay, setSelectedDay] = useState(1);
    const [mapInstance, setMapInstance] = useState(null);

    const { isLoaded } = useJsApiLoader({
        id: 'google-map-script',
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY
    });

    useEffect(() => {
        const fetchPlanDetail = async () => {
            try {
                // ★ fetch -> fetchWithAuth 로 변경!
                const response = await fetchWithAuth(`http://localhost:8080/api/plans/${id}`, {
                    headers: { 'Content-Type': 'application/json' }
                });
                if (response.ok) {
                    const result = await response.json();
                    setPlan(result.data);
                }
            } catch (error) {
                console.error("서버 연결 에러:", error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchPlanDetail();
    }, [id]);

    const currentDayPlaces = useMemo(() => {
        if (!plan || !plan.itineraries) return [];
        const dayData = plan.itineraries.find(d => d.dayNumber === selectedDay);
        return (dayData?.places || []).filter(p => p.latitude && p.longitude && p.latitude !== 0.0);
    }, [plan, selectedDay]);

    useEffect(() => {
        if (mapInstance && currentDayPlaces.length > 0) {
            const bounds = new window.google.maps.LatLngBounds();
            currentDayPlaces.forEach(p => bounds.extend({ lat: p.latitude, lng: p.longitude }));
            mapInstance.fitBounds(bounds);
        }
    }, [mapInstance, currentDayPlaces]);

    const onLoadMap = useCallback((map) => {
        setMapInstance(map);
    }, []);

    if (isLoading) return <div className="flex justify-center items-center h-[calc(100vh-80px)]"><Loader2 className="animate-spin text-blue-500" size={40} /></div>;
    if (!plan) return <div className="p-20 text-center font-bold text-gray-500 text-xl">일정을 찾을 수 없습니다. 😢</div>;

    return (
        <div className="flex flex-col lg:flex-row h-[calc(100vh-80px)] bg-gray-50 overflow-hidden animate-in fade-in duration-300">

            {/* ⬅️ 왼쪽 영역 (타임라인) */}
            <div className="w-full lg:w-5/12 h-full overflow-y-auto border-r border-gray-200 bg-white">
                <div className="sticky top-0 bg-white/95 backdrop-blur-md z-10 p-6 border-b border-gray-100 shadow-sm">
                    <Link to="/mypage" className="inline-flex items-center text-sm font-bold text-gray-500 hover:text-blue-600 mb-4 transition">
                        <ChevronLeft size={16} /> 목록으로 돌아가기
                    </Link>
                    <h1 className="text-2xl font-black text-gray-800 leading-tight mb-3">{plan.title}</h1>

                    <div className="flex flex-wrap items-center gap-2 text-sm font-semibold text-gray-600">
                        <span className="flex items-center gap-1.5 bg-blue-50 text-blue-700 px-3 py-1.5 rounded-lg"><MapPin size={16}/> {plan.region}</span>
                        <span className="flex items-center gap-1.5 bg-gray-100 px-3 py-1.5 rounded-lg"><Calendar size={16}/> {plan.startDate} ~ {plan.endDate}</span>
                    </div>

                    <div className="flex flex-wrap gap-2 mt-3 text-xs font-medium text-gray-500">
                        {plan.departure && <span className="bg-gray-50 border border-gray-200 px-2 py-1 rounded">🛫 {plan.departure} 출발</span>}
                        {plan.companion && <span className="bg-gray-50 border border-gray-200 px-2 py-1 rounded">👥 {plan.companion}</span>}
                        {plan.style && <span className="bg-gray-50 border border-gray-200 px-2 py-1 rounded">✨ {plan.style}</span>}
                        {/* 숙소 표시 영역 추가! */}
                        {plan.accommodation && <span className="bg-amber-50 text-amber-700 border border-amber-200 px-2 py-1 rounded flex items-center gap-1"><BedDouble size={14}/> {plan.accommodation}</span>}
                    </div>
                </div>

                <div className="p-6 space-y-8 pb-20">
                    {plan.itineraries?.map((day) => (
                        <div key={day.id || day.dayNumber} className="relative">
                            <h2 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                                <span className="bg-blue-600 text-white px-3 py-1 rounded-lg text-sm shadow-sm">Day {day.dayNumber}</span>
                                {day.theme}
                            </h2>

                            <div className="space-y-4 pl-4 border-l-2 border-blue-100 ml-4 mt-2">
                                {day.places?.map((place, idx) => (
                                    <div key={place.id || idx} className="relative pl-6">
                                        <div className="absolute -left-[21px] top-1 w-4 h-4 rounded-full bg-white border-4 border-blue-500 shadow-sm"></div>
                                        <div className="bg-gray-50 p-4 rounded-2xl border border-gray-100 hover:border-blue-300 hover:shadow-md transition cursor-pointer group">
                                            <h3 className="text-lg font-bold text-gray-800 group-hover:text-blue-600 transition flex items-center gap-2">
                                                <span className="bg-blue-100 text-blue-600 w-6 h-6 rounded-full flex items-center justify-center text-xs font-black">{idx + 1}</span>
                                                {place.placeName}
                                            </h3>
                                            {place.memo && (
                                                <p className="text-gray-500 text-sm mt-3 leading-relaxed flex items-start gap-2 bg-white p-3 rounded-xl border border-gray-100">
                                                    <Bot size={16} className="text-blue-400 mt-0.5 shrink-0" /> {place.memo}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* ➡️ 오른쪽 영역 (지도) */}
            <div className="hidden lg:block w-7/12 h-full bg-gray-200 relative">

                {/* ★ 마이리얼트립 스타일! 지도 위에 둥둥 떠 있는 Day 선택 탭 */}
                <div className="absolute top-6 left-1/2 -translate-x-1/2 z-10 bg-white/90 backdrop-blur-md rounded-full shadow-lg p-1.5 flex gap-1 border border-gray-200 overflow-x-auto max-w-[90%]">
                    {plan.itineraries?.map(day => (
                        <button
                            key={day.dayNumber}
                            onClick={() => setSelectedDay(day.dayNumber)}
                            className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                                selectedDay === day.dayNumber
                                    ? 'bg-blue-600 text-white shadow-md'
                                    : 'text-gray-500 hover:bg-gray-100'
                            }`}
                        >
                            Day {day.dayNumber}
                        </button>
                    ))}
                </div>

                {!isLoaded ? (
                    <div className="flex flex-col items-center justify-center h-full text-gray-500">
                        <Loader2 className="animate-spin mb-4" size={40} />
                        <p className="font-semibold">지도를 불러오는 중입니다...</p>
                    </div>
                ) : (
                    <GoogleMap
                        mapContainerStyle={{ width: '100%', height: '100%' }}
                        onLoad={onLoadMap}
                        options={{ disableDefaultUI: true, zoomControl: true }}
                    >
                        {/* 선택된 Day의 장소들만 마커로 찍어줍니다! */}
                        {currentDayPlaces.map((place, i) => (
                            <Marker
                                key={i}
                                position={{ lat: place.latitude, lng: place.longitude }}
                                label={{ text: String(i + 1), color: 'white', fontWeight: 'bold' }}
                            />
                        ))}

                        {/* 선택된 Day의 동선만 파란 선으로 연결해 줍니다! */}
                        {currentDayPlaces.length > 1 && (
                            <Polyline
                                path={currentDayPlaces.map(p => ({ lat: p.latitude, lng: p.longitude }))}
                                options={{ strokeColor: '#2563eb', strokeOpacity: 0.8, strokeWeight: 4 }}
                            />
                        )}
                    </GoogleMap>
                )}
            </div>

        </div>
    );
}