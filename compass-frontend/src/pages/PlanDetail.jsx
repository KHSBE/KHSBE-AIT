import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  MapPin,
  Calendar,
  ChevronLeft,
  Loader2,
  BedDouble,
  Bot,
  PencilLine,
  Save,
  X,
  GripVertical,
  Trash2,
  RefreshCcw
} from 'lucide-react';
import { GoogleMap, useJsApiLoader, Marker, Polyline } from '@react-google-maps/api';
import { fetchWithAuth, buildApiUrl } from '../utils/api';

const cloneItineraries = (itineraries = []) =>
  itineraries.map((day) => ({
    ...day,
    places: (day.places || []).map((place) => ({ ...place }))
  }));

const toNumberOrZero = (value) => {
  if (value === '' || value === null || value === undefined) return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

export default function PlanDetail() {
  const { id } = useParams();
  const [plan, setPlan] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedDay, setSelectedDay] = useState(1);
  const [mapInstance, setMapInstance] = useState(null);

  const [isEditMode, setIsEditMode] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [editableItineraries, setEditableItineraries] = useState([]);
  const [dragging, setDragging] = useState(null);

  const [replaceTarget, setReplaceTarget] = useState(null);
  const [detailPlace, setDetailPlace] = useState(null);

  const { isLoaded } = useJsApiLoader({
    id: 'google-map-script',
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  });

  const fetchPlanDetail = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await fetchWithAuth(buildApiUrl(`/api/plans/${id}`), {
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) {
        throw new Error('일정 상세 조회 실패');
      }

      const result = await response.json();
      const loadedPlan = result?.data;
      setPlan(loadedPlan);
      setEditableItineraries(cloneItineraries(loadedPlan?.itineraries || []));

      if (loadedPlan?.itineraries?.length) {
        setSelectedDay((prevDay) => {
          const hasSelectedDay = loadedPlan.itineraries.some((item) => item.dayNumber === prevDay);
          return hasSelectedDay ? prevDay : loadedPlan.itineraries[0].dayNumber;
        });
      }
    } catch (error) {
      console.error('일정 상세 조회 실패:', error);
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchPlanDetail();
  }, [fetchPlanDetail]);

  const displayItineraries = useMemo(() => {
    if (!plan) return [];
    return isEditMode ? editableItineraries : (plan.itineraries || []);
  }, [plan, isEditMode, editableItineraries]);

  const currentDayPlaces = useMemo(() => {
    if (!displayItineraries.length) return [];
    const dayData = displayItineraries.find((item) => item.dayNumber === selectedDay);
    return (dayData?.places || []).filter(
      (place) => place.latitude && place.longitude && place.latitude !== 0.0 && place.longitude !== 0.0
    );
  }, [displayItineraries, selectedDay]);

  useEffect(() => {
    if (!mapInstance || !window.google || currentDayPlaces.length === 0) return;

    const bounds = new window.google.maps.LatLngBounds();
    currentDayPlaces.forEach((place) => bounds.extend({ lat: place.latitude, lng: place.longitude }));
    mapInstance.fitBounds(bounds);
  }, [mapInstance, currentDayPlaces]);

  const onLoadMap = useCallback((map) => {
    setMapInstance(map);
  }, []);

  const updatePlaces = useCallback((dayNumber, updater) => {
    setEditableItineraries((prev) =>
      prev.map((day) =>
        day.dayNumber === dayNumber
          ? { ...day, places: updater([...(day.places || [])]) }
          : day
      )
    );
  }, []);

  const handleDragStart = (dayNumber, fromIndex) => {
    if (!isEditMode) return;
    setDragging({ dayNumber, fromIndex });
  };

  const handleDrop = (dayNumber, toIndex) => {
    if (!isEditMode || !dragging) return;
    if (dragging.dayNumber !== dayNumber) {
      setDragging(null);
      return;
    }

    updatePlaces(dayNumber, (places) => {
      const next = [...places];
      const [moved] = next.splice(dragging.fromIndex, 1);
      next.splice(toIndex, 0, moved);
      return next;
    });
    setDragging(null);
  };

  const removePlace = (dayNumber, placeIndex) => {
    updatePlaces(dayNumber, (places) => places.filter((_, idx) => idx !== placeIndex));
  };

  const openReplaceModal = (dayNumber, placeIndex, place) => {
    setReplaceTarget({
      dayNumber,
      placeIndex,
      placeName: place.placeName || '',
      memo: place.memo || '',
      latitude: place.latitude ?? '',
      longitude: place.longitude ?? '',
      googlePlaceId: place.googlePlaceId || ''
    });
  };

  const applyReplace = () => {
    if (!replaceTarget) return;

    const trimmedName = replaceTarget.placeName.trim();
    if (!trimmedName) {
      alert('장소명은 비워둘 수 없습니다.');
      return;
    }

    updatePlaces(replaceTarget.dayNumber, (places) =>
      places.map((item, idx) =>
        idx === replaceTarget.placeIndex
          ? {
              ...item,
              placeName: trimmedName,
              memo: replaceTarget.memo,
              latitude: toNumberOrZero(replaceTarget.latitude),
              longitude: toNumberOrZero(replaceTarget.longitude),
              googlePlaceId: replaceTarget.googlePlaceId
            }
          : item
      )
    );

    setReplaceTarget(null);
  };

  const startEditMode = () => {
    if (!plan) return;
    setEditableItineraries(cloneItineraries(plan.itineraries || []));
    setIsEditMode(true);
  };

  const cancelEditMode = () => {
    if (!plan) return;
    setEditableItineraries(cloneItineraries(plan.itineraries || []));
    setDragging(null);
    setReplaceTarget(null);
    setIsEditMode(false);
  };

  const saveEdits = async () => {
    if (!plan) return;
    setIsSaving(true);
    try {
      for (const day of editableItineraries) {
        if (!day.id) continue;

        const payload = {
          places: (day.places || []).map((place, index) => ({
            placeName: (place.placeName || '').trim() || `장소 ${index + 1}`,
            googlePlaceId: place.googlePlaceId || null,
            latitude: place.latitude ?? 0,
            longitude: place.longitude ?? 0,
            memo: place.memo || ''
          }))
        };

        const response = await fetchWithAuth(
          buildApiUrl(`/api/plans/${plan.id}/itineraries/${day.id}/places`),
          {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
          }
        );

        if (!response.ok) {
          const errorText = await response.text();
          throw new Error(errorText || `Day ${day.dayNumber} 저장 실패`);
        }
      }

      await fetchPlanDetail();
      setIsEditMode(false);
      alert('편집한 일정이 저장되었습니다.');
    } catch (error) {
      console.error('일정 저장 실패:', error);
      alert('저장 중 오류가 발생했습니다. 다시 시도해 주세요.');
    } finally {
      setIsSaving(false);
    }
  };

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
          <div className="mb-4 flex items-start justify-between gap-3">
            <Link
              to="/mypage"
              className="inline-flex items-center text-sm font-bold text-[#4b6177] transition hover:text-[#1f8ef1]"
            >
              <ChevronLeft size={16} /> 목록으로 돌아가기
            </Link>

            {!isEditMode ? (
              <button
                onClick={startEditMode}
                className="inline-flex items-center gap-2 rounded-full border border-[#cde4f8] bg-white px-4 py-2 text-sm font-bold text-[#2d587a] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1]"
              >
                <PencilLine size={16} /> 일정 편집
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <button
                  onClick={cancelEditMode}
                  disabled={isSaving}
                  className="inline-flex items-center gap-1 rounded-full border border-[#d4e7f9] bg-white px-3 py-2 text-xs font-bold text-[#4b6177] transition hover:bg-[#f5fafe] disabled:opacity-50"
                >
                  <X size={14} /> 취소
                </button>
                <button
                  onClick={saveEdits}
                  disabled={isSaving}
                  className="inline-flex items-center gap-1 rounded-full bg-[#1f8ef1] px-3 py-2 text-xs font-bold text-white shadow-sm transition hover:bg-[#1878cc] disabled:opacity-60"
                >
                  {isSaving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                  저장
                </button>
              </div>
            )}
          </div>

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
          {displayItineraries.map((day) => (
            <div key={day.id || day.dayNumber} className="relative">
              <h2 className="mb-4 flex items-center gap-2 text-xl font-bold text-[#1f3550]">
                <span className="rounded-lg bg-[#1f8ef1] px-3 py-1 text-sm text-white shadow-sm">Day {day.dayNumber}</span>
                {day.theme}
              </h2>

              <div className="ml-4 mt-2 space-y-4 border-l-2 border-[#b9dcfa] pl-4">
                {(day.places || []).map((place, idx) => (
                  <div
                    key={place.id || `${day.dayNumber}-${idx}`}
                    className="relative pl-6"
                    draggable={isEditMode}
                    onDragStart={() => handleDragStart(day.dayNumber, idx)}
                    onDragOver={(event) => isEditMode && event.preventDefault()}
                    onDrop={() => handleDrop(day.dayNumber, idx)}
                    onDragEnd={() => setDragging(null)}
                  >
                    <div className="absolute -left-[21px] top-1 h-4 w-4 rounded-full border-4 border-[#1f8ef1] bg-white shadow-sm" />
                    <div
                      className={`group rounded-2xl border bg-[#f9fdff] p-4 transition ${
                        isEditMode
                          ? 'cursor-grab border-[#b9dbf8] hover:border-[#1f8ef1] hover:shadow-md'
                          : 'cursor-pointer border-[#d6e9fa] hover:border-[#1f8ef1] hover:shadow-md'
                      }`}
                      onClick={() => setDetailPlace(place)}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <h3 className="flex items-center gap-2 text-lg font-bold text-[#1f3550] transition group-hover:text-[#1f8ef1]">
                          {isEditMode && <GripVertical size={16} className="text-[#5f89ad]" />}
                          <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[#eaf6ff] text-xs font-black text-[#1f8ef1]">
                            {idx + 1}
                          </span>
                          {place.placeName}
                        </h3>

                        {isEditMode && (
                          <div className="flex items-center gap-1">
                            <button
                              onClick={(event) => {
                                event.stopPropagation();
                                openReplaceModal(day.dayNumber, idx, place);
                              }}
                              className="inline-flex items-center gap-1 rounded-lg border border-[#d3e7f9] bg-white px-2 py-1 text-xs font-bold text-[#3c6b91] hover:border-[#1f8ef1] hover:text-[#1f8ef1]"
                            >
                              <RefreshCcw size={12} /> 교체
                            </button>
                            <button
                              onClick={(event) => {
                                event.stopPropagation();
                                removePlace(day.dayNumber, idx);
                              }}
                              className="inline-flex items-center gap-1 rounded-lg border border-[#f4d2d2] bg-white px-2 py-1 text-xs font-bold text-[#b54747] hover:bg-[#fff5f5]"
                            >
                              <Trash2 size={12} /> 삭제
                            </button>
                          </div>
                        )}
                      </div>

                      {place.memo && (
                        <p className="mt-3 flex items-start gap-2 rounded-xl border border-[#d9ebfb] bg-white p-3 text-sm leading-relaxed text-[#4b6177]">
                          <Bot size={16} className="mt-0.5 shrink-0 text-[#1f8ef1]" /> {place.memo}
                        </p>
                      )}
                    </div>
                  </div>
                ))}

                {isEditMode && (day.places || []).length === 0 && (
                  <div className="rounded-xl border border-dashed border-[#b9dbf8] bg-[#f4faff] p-4 text-sm text-[#5f89ad]">
                    이 날은 장소가 없습니다. 다른 장소를 교체로 입력하거나 AI로 다시 생성해 주세요.
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="relative h-[44vh] w-full border-t border-[#d6e9fa] bg-[#dceefe] lg:h-full lg:w-7/12 lg:border-t-0">
        <div className="absolute left-1/2 top-3 z-10 flex max-w-[94%] -translate-x-1/2 gap-1 overflow-x-auto rounded-full border border-[#d3e8fb] bg-[#f9fdff]/92 p-1.5 shadow-lg backdrop-blur-md lg:top-6 lg:max-w-[90%]">
          {displayItineraries.map((day) => (
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

      {replaceTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl border border-[#d5e8fa] bg-white p-5 shadow-2xl">
            <h3 className="mb-4 text-lg font-black text-[#1f3550]">장소 교체</h3>
            <div className="space-y-3">
              <input
                type="text"
                value={replaceTarget.placeName}
                onChange={(event) => setReplaceTarget((prev) => ({ ...prev, placeName: event.target.value }))}
                placeholder="장소명"
                className="w-full rounded-xl border border-[#d2e8fb] px-3 py-2 text-sm outline-none focus:border-[#1f8ef1]"
              />
              <input
                type="text"
                value={replaceTarget.googlePlaceId}
                onChange={(event) => setReplaceTarget((prev) => ({ ...prev, googlePlaceId: event.target.value }))}
                placeholder="Google Place ID (선택)"
                className="w-full rounded-xl border border-[#d2e8fb] px-3 py-2 text-sm outline-none focus:border-[#1f8ef1]"
              />
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  value={replaceTarget.latitude}
                  onChange={(event) => setReplaceTarget((prev) => ({ ...prev, latitude: event.target.value }))}
                  placeholder="위도"
                  className="w-full rounded-xl border border-[#d2e8fb] px-3 py-2 text-sm outline-none focus:border-[#1f8ef1]"
                />
                <input
                  type="number"
                  value={replaceTarget.longitude}
                  onChange={(event) => setReplaceTarget((prev) => ({ ...prev, longitude: event.target.value }))}
                  placeholder="경도"
                  className="w-full rounded-xl border border-[#d2e8fb] px-3 py-2 text-sm outline-none focus:border-[#1f8ef1]"
                />
              </div>
              <textarea
                value={replaceTarget.memo}
                onChange={(event) => setReplaceTarget((prev) => ({ ...prev, memo: event.target.value }))}
                placeholder="메모"
                rows={3}
                className="w-full rounded-xl border border-[#d2e8fb] px-3 py-2 text-sm outline-none focus:border-[#1f8ef1]"
              />
            </div>
            <div className="mt-4 flex justify-end gap-2">
              <button
                onClick={() => setReplaceTarget(null)}
                className="rounded-xl border border-[#d3e7f9] bg-white px-4 py-2 text-sm font-bold text-[#4b6177]"
              >
                취소
              </button>
              <button
                onClick={applyReplace}
                className="rounded-xl bg-[#1f8ef1] px-4 py-2 text-sm font-bold text-white"
              >
                적용
              </button>
            </div>
          </div>
        </div>
      )}

      {detailPlace && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl border border-[#d5e8fa] bg-white p-5 shadow-2xl">
            <h3 className="mb-3 text-lg font-black text-[#1f3550]">{detailPlace.placeName}</h3>
            <div className="space-y-2 text-sm text-[#3f5f7b]">
              <p><span className="font-bold text-[#1f3550]">메모</span>: {detailPlace.memo || '없음'}</p>
              <p><span className="font-bold text-[#1f3550]">위도</span>: {detailPlace.latitude ?? '-'}</p>
              <p><span className="font-bold text-[#1f3550]">경도</span>: {detailPlace.longitude ?? '-'}</p>
              <p><span className="font-bold text-[#1f3550]">Place ID</span>: {detailPlace.googlePlaceId || '-'}</p>
            </div>
            <p className="mt-3 rounded-xl border border-[#dbeaf9] bg-[#f6fbff] p-3 text-xs text-[#5f89ad]">
              장소 상세(영업시간/평점/리뷰)는 Places API 검증 레이어를 붙이는 다음 단계에서 실시간으로 연동됩니다.
            </p>
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setDetailPlace(null)}
                className="rounded-xl bg-[#1f8ef1] px-4 py-2 text-sm font-bold text-white"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
