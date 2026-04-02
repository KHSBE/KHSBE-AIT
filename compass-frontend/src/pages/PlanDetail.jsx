import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { MapPin, Calendar, ChevronLeft, Loader2, BedDouble, Bot } from 'lucide-react';
import { GoogleMap, useJsApiLoader, Marker, Polyline } from '@react-google-maps/api';
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
        const response = await fetchWithAuth(buildApiUrl(`/api/plans/${id}`), {
          headers: { 'Content-Type': 'application/json' }
        });

        if (!response.ok) {
          throw new Error('일정 상세 조회 실패');
        }

        const result = await response.json();
        setPlan(result.data);
      } catch (error) {
        console.error('일정 상세 조회 실패:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchPlanDetail();
  }, [id]);

  const currentDayPlaces = useMemo(() => {
    if (!plan || !plan.itineraries) return [];
    const dayData = plan.itineraries.find((item) => item.dayNumber === selectedDay);
    return (dayData?.places || []).filter(
      (place) => place.latitude && place.longitude && place.latitude !== 0.0 && place.longitude !== 0.0
    );
  }, [plan, selectedDay]);

  useEffect(() => {
    if (!mapInstance || !window.google || currentDayPlaces.length === 0) return;

    const bounds = new window.google.maps.LatLngBounds();
    currentDayPlaces.forEach((place) => bounds.extend({ lat: place.latitude, lng: place.longitude }));
    mapInstance.fitBounds(bounds);
  }, [mapInstance, currentDayPlaces]);

  const onLoadMap = useCallback((map) => {
    setMapInstance(map);
  }, []);

  if (isLoading) {
    return (
      <div className="flex h-[calc(100vh-76px)] items-center justify-center">
        <Loader2 className="animate-spin text-[#1f8ef1]" size={40} />
      </div>
    );
  }

  if (!plan) {
    return <div className="p-20 text-center text-xl font-bold text-[#4b6177]">일정을 찾을 수 없습니다.</div>;
  }

  return (
    <div className="flex min-h-[calc(100vh-76px)] flex-col bg-[#eef7ff] lg:h-[calc(100vh-76px)] lg:flex-row lg:overflow-hidden">
      <div className="h-auto w-full overflow-visible border-r-0 border-[#d6e9fa] bg-[#f9fdff] lg:h-full lg:w-5/12 lg:overflow-y-auto lg:border-r">
        <div className="sticky top-0 z-10 border-b border-[#d6e9fa] bg-[#f9fdff]/95 p-4 shadow-sm backdrop-blur-md sm:p-6">
          <Link
            to="/mypage"
            className="mb-4 inline-flex items-center text-sm font-bold text-[#4b6177] transition hover:text-[#1f8ef1]"
          >
            <ChevronLeft size={16} /> 목록으로 돌아가기
          </Link>
          <h1 className="mb-3 text-xl font-black leading-tight text-[#1f3550] sm:text-2xl">{plan.title}</h1>

          <div className="flex flex-wrap items-center gap-2 text-sm font-semibold text-[#4b6177]">
            <span className="flex items-center gap-1.5 rounded-lg bg-[#eaf6ff] px-3 py-1.5 text-[#1f8ef1]">
              <MapPin size={16} /> {plan.region}
            </span>
            <span className="flex items-center gap-1.5 rounded-lg bg-[#f2f9ff] px-3 py-1.5">
              <Calendar size={16} /> {plan.startDate} ~ {plan.endDate}
            </span>
          </div>

          <div className="mt-3 flex flex-wrap gap-2 text-xs font-medium text-[#4b6177]">
            {plan.departure && <span className="rounded border border-[#d6e9fa] bg-[#f2f9ff] px-2 py-1">출발: {plan.departure}</span>}
            {plan.companion && <span className="rounded border border-[#d6e9fa] bg-[#f2f9ff] px-2 py-1">동행: {plan.companion}</span>}
            {plan.style && <span className="rounded border border-[#d6e9fa] bg-[#f2f9ff] px-2 py-1">스타일: {plan.style}</span>}
            {plan.accommodation && (
              <span className="flex items-center gap-1 rounded border border-[#c8e5ff] bg-[#eaf6ff] px-2 py-1 text-[#276899]">
                <BedDouble size={14} /> {plan.accommodation}
              </span>
            )}
          </div>
        </div>

        <div className="space-y-8 p-4 pb-8 sm:p-6 sm:pb-20">
          {plan.itineraries?.map((day) => (
            <div key={day.id || day.dayNumber} className="relative">
              <h2 className="mb-4 flex items-center gap-2 text-xl font-bold text-[#1f3550]">
                <span className="rounded-lg bg-[#1f8ef1] px-3 py-1 text-sm text-white shadow-sm">Day {day.dayNumber}</span>
                {day.theme}
              </h2>

              <div className="ml-4 mt-2 space-y-4 border-l-2 border-[#b9dcfa] pl-4">
                {day.places?.map((place, idx) => (
                  <div key={place.id || idx} className="relative pl-6">
                    <div className="absolute -left-[21px] top-1 h-4 w-4 rounded-full border-4 border-[#1f8ef1] bg-white shadow-sm" />
                    <div className="group cursor-pointer rounded-2xl border border-[#d6e9fa] bg-[#f9fdff] p-4 transition hover:border-[#1f8ef1] hover:shadow-md">
                      <h3 className="flex items-center gap-2 text-lg font-bold text-[#1f3550] transition group-hover:text-[#1f8ef1]">
                        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[#eaf6ff] text-xs font-black text-[#1f8ef1]">
                          {idx + 1}
                        </span>
                        {place.placeName}
                      </h3>
                      {place.memo && (
                        <p className="mt-3 flex items-start gap-2 rounded-xl border border-[#d9ebfb] bg-white p-3 text-sm leading-relaxed text-[#4b6177]">
                          <Bot size={16} className="mt-0.5 shrink-0 text-[#1f8ef1]" /> {place.memo}
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

      <div className="relative h-[44vh] w-full border-t border-[#d6e9fa] bg-[#dceefe] lg:h-full lg:w-7/12 lg:border-t-0">
        <div className="absolute left-1/2 top-3 z-10 flex max-w-[94%] -translate-x-1/2 gap-1 overflow-x-auto rounded-full border border-[#d3e8fb] bg-[#f9fdff]/92 p-1.5 shadow-lg backdrop-blur-md lg:top-6 lg:max-w-[90%]">
          {plan.itineraries?.map((day) => (
            <button
              key={day.dayNumber}
              onClick={() => setSelectedDay(day.dayNumber)}
              className={`whitespace-nowrap rounded-full px-5 py-2.5 text-sm font-bold transition-all ${
                selectedDay === day.dayNumber ? 'bg-[#1f8ef1] text-white shadow-md' : 'text-[#4b6177] hover:bg-[#eaf6ff]'
              }`}
            >
              Day {day.dayNumber}
            </button>
          ))}
        </div>

        {!isLoaded ? (
          <div className="flex h-full flex-col items-center justify-center text-[#4b6177]">
            <Loader2 className="mb-4 animate-spin" size={40} />
            <p className="font-semibold">지도를 불러오는 중입니다...</p>
          </div>
        ) : (
          <GoogleMap
            mapContainerStyle={{ width: '100%', height: '100%' }}
            onLoad={onLoadMap}
            options={{ disableDefaultUI: true, zoomControl: true }}
          >
            {currentDayPlaces.map((place, index) => (
              <Marker
                key={index}
                position={{ lat: place.latitude, lng: place.longitude }}
                label={{ text: String(index + 1), color: 'white', fontWeight: 'bold' }}
              />
            ))}

            {currentDayPlaces.length > 1 && (
              <Polyline
                path={currentDayPlaces.map((place) => ({ lat: place.latitude, lng: place.longitude }))}
                options={{ strokeColor: '#1f8ef1', strokeOpacity: 0.82, strokeWeight: 4 }}
              />
            )}
          </GoogleMap>
        )}
      </div>
    </div>
  );
}
