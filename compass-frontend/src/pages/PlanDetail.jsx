import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { MapPin, Calendar, ChevronLeft, Loader2, BedDouble, Bot } from 'lucide-react';
import { GoogleMap, useJsApiLoader, Marker, Polyline } from '@react-google-maps/api';
// ★ 토큰 자동 갱신 마법 함수 가져오기!
import { fetchWithAuth, buildApiUrl } from '../utils/api';

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
                const response = await fetchWithAuth(buildApiUrl(`/api/plans/${id}`), {
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

    if (isLoading) return <div className="flex h-[calc(100vh-76px)] items-center justify-center"><Loader2 className="animate-spin text-[#0f7a80]" size={40} /></div>;
    if (!plan) return <div className="p-20 text-center text-xl font-bold text-[#5e6673]">일정을 찾을 수 없습니다.</div>;

    return (
        <div className="flex min-h-[calc(100vh-76px)] flex-col bg-[#f7f1e6] lg:h-[calc(100vh-76px)] lg:flex-row lg:overflow-hidden">

            {/* ⬅️ 왼쪽 영역 (타임라인) */}
            <div className="h-auto w-full overflow-visible border-r-0 border-[#dbcfbd] bg-[#fffdf8] lg:h-full lg:w-5/12 lg:overflow-y-auto lg:border-r">
                <div className="sticky top-0 z-10 border-b border-[#ddd2bf] bg-[#fffdf8]/95 p-4 shadow-sm backdrop-blur-md sm:p-6">
                    <Link to="/mypage" className="mb-4 inline-flex items-center text-sm font-bold text-[#6a7280] transition hover:text-[#0f7a80]">
                        <ChevronLeft size={16} /> 목록으로 돌아가기
                    </Link>
                    <h1 className="mb-3 text-xl font-black leading-tight text-[#1f2430] sm:text-2xl">{plan.title}</h1>

                    <div className="flex flex-wrap items-center gap-2 text-sm font-semibold text-[#5e6673]">
                        <span className="flex items-center gap-1.5 rounded-lg bg-[#d6f0ee] px-3 py-1.5 text-[#0f7a80]"><MapPin size={16}/> {plan.region}</span>
                        <span className="flex items-center gap-1.5 rounded-lg bg-[#efe6d6] px-3 py-1.5"><Calendar size={16}/> {plan.startDate} ~ {plan.endDate}</span>
                    </div>

                    <div className="mt-3 flex flex-wrap gap-2 text-xs font-medium text-[#5e6673]">
                        {plan.departure && <span className="rounded border border-[#ddd1bf] bg-[#f8f1e3] px-2 py-1">출발: {plan.departure}</span>}
                        {plan.companion && <span className="rounded border border-[#ddd1bf] bg-[#f8f1e3] px-2 py-1">일행: {plan.companion}</span>}
                        {plan.style && <span className="rounded border border-[#ddd1bf] bg-[#f8f1e3] px-2 py-1">테마: {plan.style}</span>}
                        {/* 숙소 표시 영역 추가! */}
                        {plan.accommodation && <span className="flex items-center gap-1 rounded border border-[#ffd2c2] bg-[#fff0ea] px-2 py-1 text-[#b8512d]"><BedDouble size={14}/> {plan.accommodation}</span>}
                    </div>
                </div>

                <div className="space-y-8 p-4 pb-8 sm:p-6 sm:pb-20">
                    {plan.itineraries?.map((day) => (
                        <div key={day.id || day.dayNumber} className="relative">
                            <h2 className="mb-4 flex items-center gap-2 text-xl font-bold text-[#1f2430]">
                                <span className="rounded-lg bg-[#0f7a80] px-3 py-1 text-sm text-white shadow-sm">Day {day.dayNumber}</span>
                                {day.theme}
                            </h2>

                            <div className="ml-4 mt-2 space-y-4 border-l-2 border-[#bce0dd] pl-4">
                                {day.places?.map((place, idx) => (
                                    <div key={place.id || idx} className="relative pl-6">
                                        <div className="absolute -left-[21px] top-1 h-4 w-4 rounded-full border-4 border-[#0f7a80] bg-white shadow-sm" />
                                        <div className="group cursor-pointer rounded-2xl border border-[#ddd1bf] bg-[#fffaf1] p-4 transition hover:border-[#0f7a80] hover:shadow-md">
                                            <h3 className="flex items-center gap-2 text-lg font-bold text-[#1f2430] transition group-hover:text-[#0f7a80]">
                                                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[#d6f0ee] text-xs font-black text-[#0f7a80]">{idx + 1}</span>
                                                {place.placeName}
                                            </h3>
                                            {place.memo && (
                                                <p className="mt-3 flex items-start gap-2 rounded-xl border border-[#e4dac9] bg-white p-3 text-sm leading-relaxed text-[#5e6673]">
                                                    <Bot size={16} className="mt-0.5 shrink-0 text-[#0f7a80]" /> {place.memo}
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
            <div className="relative h-[44vh] w-full border-t border-[#d8ccbb] bg-[#d9ddd8] lg:h-full lg:w-7/12 lg:border-t-0">

                {/* ★ 마이리얼트립 스타일! 지도 위에 둥둥 떠 있는 Day 선택 탭 */}
                <div className="absolute left-1/2 top-3 z-10 flex max-w-[94%] -translate-x-1/2 gap-1 overflow-x-auto rounded-full border border-[#d8ccbb] bg-[#fffdf8]/92 p-1.5 shadow-lg backdrop-blur-md lg:top-6 lg:max-w-[90%]">
                    {plan.itineraries?.map(day => (
                        <button
                            key={day.dayNumber}
                            onClick={() => setSelectedDay(day.dayNumber)}
                            className={`px-5 py-2.5 rounded-full text-sm font-bold transition-all whitespace-nowrap ${
                                selectedDay === day.dayNumber
                                    ? 'bg-[#0f7a80] text-white shadow-md'
                                    : 'text-[#5e6673] hover:bg-[#efe7d8]'
                            }`}
                        >
                            Day {day.dayNumber}
                        </button>
                    ))}
                </div>

                {!isLoaded ? (
                    <div className="flex h-full flex-col items-center justify-center text-[#5e6673]">
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
                                options={{ strokeColor: '#0f7a80', strokeOpacity: 0.82, strokeWeight: 4 }}
                            />
                        )}
                    </GoogleMap>
                )}
            </div>

        </div>
    );
}
