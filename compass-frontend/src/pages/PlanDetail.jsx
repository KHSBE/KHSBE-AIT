import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
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
  RefreshCcw,
  Search
} from 'lucide-react';
import { GoogleMap, useJsApiLoader, Marker, Polyline } from '@react-google-maps/api';
import { fetchWithAuth, buildApiUrl } from '../utils/api';

const GOOGLE_MAP_LIBRARIES = ['places'];

const MAP_SIMPLIFIED_STYLES = [
  { featureType: 'poi.business', stylers: [{ visibility: 'off' }] },
  { featureType: 'poi.attraction', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'road', elementType: 'labels.icon', stylers: [{ visibility: 'off' }] },
  { featureType: 'administrative.neighborhood', stylers: [{ visibility: 'off' }] }
];

const cloneItineraries = (itineraries = []) =>
  itineraries.map((day) => ({
    ...day,
    places: (day.places || []).map((place) => ({ ...place }))
  }));

const hasValidCoordinates = (place) => {
  const lat = Number(place.latitude);
  const lng = Number(place.longitude);
  return Number.isFinite(lat) && Number.isFinite(lng) && (lat !== 0 || lng !== 0);
};

const buildPlaceKey = (dayNumber, index, place) =>
  `${dayNumber}-${place.id ?? place.googlePlaceId ?? place.placeName ?? 'place'}-${index}`;

const toLatLngLiteral = (place) => ({
  lat: Number(place.latitude),
  lng: Number(place.longitude)
});

const getDirectionsSegmentPath = (service, origin, destination) =>
  new Promise((resolve, reject) => {
    service.route(
      {
        origin,
        destination,
        travelMode: window.google.maps.TravelMode.DRIVING
      },
      (result, status) => {
        if (status === window.google.maps.DirectionsStatus.OK && result?.routes?.length) {
          const path = result.routes[0].overview_path.map((point) => ({
            lat: point.lat(),
            lng: point.lng()
          }));
          resolve(path);
          return;
        }
        reject(new Error(`Directions 실패: ${status}`));
      }
    );
  });

const buildDirectionsPath = async (service, places) => {
  const mergedPath = [];

  for (let i = 0; i < places.length - 1; i += 1) {
    const origin = toLatLngLiteral(places[i]);
    const destination = toLatLngLiteral(places[i + 1]);
    const segment = await getDirectionsSegmentPath(service, origin, destination);
    if (!segment.length) continue;

    if (mergedPath.length > 0) segment.shift();
    mergedPath.push(...segment);
  }

  return mergedPath;
};

const toNumberOrZero = (value) => {
  if (value === '' || value === null || value === undefined) return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const searchPlaces = (service, query) =>
  new Promise((resolve, reject) => {
    service.textSearch({ query }, (results, status) => {
      if (status === window.google.maps.places.PlacesServiceStatus.OK) {
        resolve(results || []);
        return;
      }
      if (status === window.google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
        resolve([]);
        return;
      }
      reject(new Error(`Places 검색 실패: ${status}`));
    });
  });

const findSinglePlace = (service, query) =>
  new Promise((resolve, reject) => {
    service.findPlaceFromQuery(
      {
        query,
        fields: ['place_id', 'name', 'formatted_address', 'geometry']
      },
      (results, status) => {
        if (status === window.google.maps.places.PlacesServiceStatus.OK && results?.length) {
          resolve(results[0]);
          return;
        }
        if (status === window.google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
          resolve(null);
          return;
        }
        reject(new Error(`Place 탐색 실패: ${status}`));
      }
    );
  });

const getPlaceDetail = (service, placeId) =>
  new Promise((resolve, reject) => {
    service.getDetails(
      {
        placeId,
        fields: [
          'place_id',
          'name',
          'formatted_address',
          'rating',
          'user_ratings_total',
          'opening_hours',
          'reviews',
          'website',
          'formatted_phone_number',
          'url',
          'business_status'
        ]
      },
      (result, status) => {
        if (status === window.google.maps.places.PlacesServiceStatus.OK && result) {
          resolve(result);
          return;
        }
        reject(new Error(`상세 조회 실패: ${status}`));
      }
    );
  });

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
  const [replaceCandidates, setReplaceCandidates] = useState([]);
  const [isSearchingPlace, setIsSearchingPlace] = useState(false);
  const [replaceSearchError, setReplaceSearchError] = useState('');
  const [detailPlace, setDetailPlace] = useState(null);
  const [detailState, setDetailState] = useState({ loading: false, error: '', data: null });
  const [focusedPlaceKey, setFocusedPlaceKey] = useState(null);
  const [routePath, setRoutePath] = useState([]);
  const [isRouteLoading, setIsRouteLoading] = useState(false);
  const [isMapCollapsed, setIsMapCollapsed] = useState(false);
  const placesServiceRef = useRef(null);

  const { isLoaded } = useJsApiLoader({
    id: 'google-map-script',
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    libraries: GOOGLE_MAP_LIBRARIES
  });

  useEffect(() => {
    if (!isLoaded || !window.google?.maps?.places) return;
    if (!placesServiceRef.current) {
      placesServiceRef.current = new window.google.maps.places.PlacesService(document.createElement('div'));
    }
  }, [isLoaded]);

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
    const placesWithIndex = (dayData?.places || []).map((place, index) => ({
      ...place,
      sourceIndex: index,
      placeKey: buildPlaceKey(selectedDay, index, place)
    }));

    const withCoordinates = placesWithIndex.filter(hasValidCoordinates);
    return withCoordinates.map((place, mapIndex) => ({
      ...place,
      latitude: Number(place.latitude),
      longitude: Number(place.longitude),
      markerOrder: mapIndex + 1
    }));
  }, [displayItineraries, selectedDay]);

  const mapOptions = useMemo(
    () => ({
      disableDefaultUI: true,
      zoomControl: true,
      styles: MAP_SIMPLIFIED_STYLES
    }),
    []
  );

  const fitCurrentDayBounds = useCallback(() => {
    if (!mapInstance || !window.google || currentDayPlaces.length === 0) return;

    if (currentDayPlaces.length === 1) {
      mapInstance.panTo(toLatLngLiteral(currentDayPlaces[0]));
      mapInstance.setZoom(15);
      return;
    }

    const bounds = new window.google.maps.LatLngBounds();
    currentDayPlaces.forEach((place) => bounds.extend(toLatLngLiteral(place)));
    mapInstance.fitBounds(bounds);
  }, [mapInstance, currentDayPlaces]);

  useEffect(() => {
    if (isMapCollapsed) return;
    fitCurrentDayBounds();
  }, [fitCurrentDayBounds, isMapCollapsed]);

  useEffect(() => {
    if (!mapInstance || !focusedPlaceKey) return;
    const target = currentDayPlaces.find((place) => place.placeKey === focusedPlaceKey);
    if (!target) return;

    mapInstance.panTo({ lat: target.latitude, lng: target.longitude });
  }, [mapInstance, currentDayPlaces, focusedPlaceKey]);

  useEffect(() => {
    setFocusedPlaceKey(null);
  }, [selectedDay]);

  useEffect(() => {
    if (!isLoaded || !window.google?.maps || currentDayPlaces.length < 2) {
      setRoutePath([]);
      setIsRouteLoading(false);
      return;
    }

    let cancelled = false;

    const loadRoutePath = async () => {
      setIsRouteLoading(true);
      try {
        const directionsService = new window.google.maps.DirectionsService();
        const path = await buildDirectionsPath(directionsService, currentDayPlaces);
        if (cancelled) return;
        if (path.length > 1) {
          setRoutePath(path);
        } else {
          setRoutePath(currentDayPlaces.map(toLatLngLiteral));
        }
      } catch (error) {
        console.error('Directions 경로 계산 실패:', error);
        if (!cancelled) {
          setRoutePath(currentDayPlaces.map(toLatLngLiteral));
        }
      } finally {
        if (!cancelled) setIsRouteLoading(false);
      }
    };

    loadRoutePath();
    return () => {
      cancelled = true;
    };
  }, [isLoaded, currentDayPlaces]);

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
    setReplaceCandidates([]);
    setReplaceSearchError('');
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

  const searchReplaceCandidates = async () => {
    if (!replaceTarget) return;
    const service = placesServiceRef.current;
    if (!service) {
      setReplaceSearchError('Google Places가 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.');
      return;
    }

    const query = `${replaceTarget.placeName} ${plan?.region || ''}`.trim();
    if (!query) {
      setReplaceSearchError('검색어를 입력해 주세요.');
      return;
    }

    setIsSearchingPlace(true);
    setReplaceSearchError('');

    try {
      const results = await searchPlaces(service, query);
      setReplaceCandidates(
        results.slice(0, 6).map((item) => ({
          placeName: item.name || '',
          googlePlaceId: item.place_id || '',
          latitude: item.geometry?.location?.lat?.() ?? 0,
          longitude: item.geometry?.location?.lng?.() ?? 0,
          address: item.formatted_address || '',
          rating: item.rating ?? null,
          userRatingsTotal: item.user_ratings_total ?? null
        }))
      );
    } catch (error) {
      console.error('Places 검색 실패:', error);
      setReplaceSearchError('Places 검색에 실패했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setIsSearchingPlace(false);
    }
  };

  const selectReplaceCandidate = (candidate) => {
    setReplaceTarget((prev) => ({
      ...prev,
      placeName: candidate.placeName,
      googlePlaceId: candidate.googlePlaceId,
      latitude: candidate.latitude,
      longitude: candidate.longitude
    }));
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
    setReplaceCandidates([]);
    setReplaceSearchError('');
  };

  const loadPlaceDetail = useCallback(async (place) => {
    const service = placesServiceRef.current;
    if (!service) {
      setDetailState({ loading: false, error: 'Google Places가 아직 준비되지 않았습니다.', data: null });
      return;
    }

    setDetailState({ loading: true, error: '', data: null });

    try {
      let placeId = place.googlePlaceId;
      if (!placeId) {
        const found = await findSinglePlace(service, `${place.placeName} ${plan?.region || ''}`.trim());
        placeId = found?.place_id;
      }

      if (!placeId) {
        setDetailState({ loading: false, error: '해당 장소의 Google 상세 정보를 찾지 못했습니다.', data: null });
        return;
      }

      const detail = await getPlaceDetail(service, placeId);
      setDetailState({
        loading: false,
        error: '',
        data: {
          placeId: detail.place_id || placeId,
          name: detail.name || place.placeName,
          address: detail.formatted_address || '',
          rating: detail.rating ?? null,
          userRatingsTotal: detail.user_ratings_total ?? null,
          openingHours: detail.opening_hours?.weekday_text || [],
          isOpenNow:
            typeof detail.opening_hours?.isOpen === 'function'
              ? detail.opening_hours.isOpen()
              : null,
          phone: detail.formatted_phone_number || '',
          website: detail.website || '',
          mapUrl: detail.url || '',
          businessStatus: detail.business_status || '',
          reviews: (detail.reviews || []).slice(0, 3).map((review) => ({
            author: review.author_name || '사용자',
            rating: review.rating ?? null,
            text: review.text || ''
          }))
        }
      });
    } catch (error) {
      console.error('Places 상세 조회 실패:', error);
      setDetailState({ loading: false, error: '장소 상세 조회에 실패했습니다.', data: null });
    }
  }, [plan?.region]);

  const openPlaceDetail = (place) => {
    setDetailPlace(place);
    loadPlaceDetail(place);
  };

  const getTodayOpeningLine = (openingHours = []) => {
    if (!openingHours.length) return '';
    const weekday = new Date().toLocaleDateString('en-US', { weekday: 'long' }).toLowerCase();
    return openingHours.find((line) => line.toLowerCase().startsWith(weekday)) || '';
  };

  const isTodayClosed = (openingHours = []) => {
    const todayLine = getTodayOpeningLine(openingHours).toLowerCase();
    if (!todayLine) return false;
    return (
      todayLine.includes('closed') ||
      todayLine.includes('휴무') ||
      todayLine.includes('영업 종료')
    );
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
    setReplaceCandidates([]);
    setReplaceSearchError('');
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
      <div
        className={`h-auto w-full overflow-visible border-r-0 border-[#d6e9fa] bg-[#f9fdff] lg:h-full lg:overflow-y-auto lg:border-r ${
          isMapCollapsed ? 'lg:w-full' : 'lg:w-5/12'
        }`}
      >
        <div className="sticky top-0 z-10 border-b border-[#d6e9fa] bg-[#f9fdff]/95 p-4 shadow-sm backdrop-blur-md sm:p-6">
          <div className="mb-4 flex items-start justify-between gap-3">
            <Link
              to="/mypage"
              className="inline-flex items-center text-sm font-bold text-[#4b6177] transition hover:text-[#1f8ef1]"
            >
              <ChevronLeft size={16} /> 목록으로 돌아가기
            </Link>

            <div className="flex items-center gap-2">
              {!isEditMode ? (
                <button
                  onClick={startEditMode}
                  className="inline-flex items-center gap-2 rounded-full border border-[#cde4f8] bg-white px-4 py-2 text-sm font-bold text-[#2d587a] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1]"
                >
                  <PencilLine size={16} /> 일정 편집
                </button>
              ) : (
                <>
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
                </>
              )}

              <button
                onClick={() => setIsMapCollapsed((prev) => !prev)}
                className="hidden rounded-full border border-[#cde4f8] bg-white px-3 py-2 text-xs font-bold text-[#2d587a] transition hover:border-[#1f8ef1] hover:text-[#1f8ef1] lg:inline-flex"
              >
                {isMapCollapsed ? '지도 펼치기' : '지도 숨기기'}
              </button>
            </div>
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
                  (() => {
                    const placeKey = buildPlaceKey(day.dayNumber, idx, place);
                    const focusable = day.dayNumber === selectedDay && hasValidCoordinates(place);
                    const isFocused = focusedPlaceKey === placeKey;
                    return (
                  <div
                    key={placeKey}
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
                          : isFocused
                            ? 'cursor-pointer border-[#1f8ef1] ring-2 ring-[#8ec9f8] shadow-md'
                            : 'cursor-pointer border-[#d6e9fa] hover:border-[#1f8ef1] hover:shadow-md'
                      }`}
                      onMouseEnter={() => focusable && setFocusedPlaceKey(placeKey)}
                      onMouseLeave={() => focusable && setFocusedPlaceKey((prev) => (prev === placeKey ? null : prev))}
                      onClick={() => {
                        if (focusable) setFocusedPlaceKey(placeKey);
                        openPlaceDetail(place);
                      }}
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
                    );
                  })()
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

      <div
        className={`relative h-[44vh] w-full border-t border-[#d6e9fa] bg-[#dceefe] lg:h-full lg:border-t-0 ${
          isMapCollapsed ? 'lg:hidden' : 'lg:w-7/12'
        }`}
      >
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

        <div className="absolute right-3 top-3 z-10 flex items-center gap-2 lg:right-6 lg:top-6">
          <button
            onClick={fitCurrentDayBounds}
            className="rounded-full border border-[#cde4f8] bg-white/95 px-3 py-2 text-xs font-bold text-[#2d587a] shadow-md backdrop-blur hover:border-[#1f8ef1] hover:text-[#1f8ef1]"
          >
            전체보기
          </button>
          <button
            onClick={() => setIsMapCollapsed(true)}
            className="hidden rounded-full border border-[#cde4f8] bg-white/95 px-3 py-2 text-xs font-bold text-[#2d587a] shadow-md backdrop-blur hover:border-[#1f8ef1] hover:text-[#1f8ef1] lg:inline-flex"
          >
            지도 닫기
          </button>
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
            options={mapOptions}
          >
            {currentDayPlaces.map((place) => {
              const isFocused = focusedPlaceKey === place.placeKey;
              return (
              <Marker
                key={place.placeKey}
                position={{ lat: place.latitude, lng: place.longitude }}
                label={{ text: String(place.markerOrder), color: 'white', fontWeight: 'bold' }}
                zIndex={isFocused ? 1000 : 1}
                icon={{
                  path: window.google.maps.SymbolPath.CIRCLE,
                  fillColor: isFocused ? '#ff7a45' : '#1f8ef1',
                  fillOpacity: 1,
                  strokeColor: '#ffffff',
                  strokeWeight: 2,
                  scale: isFocused ? 13 : 11
                }}
                onMouseOver={() => setFocusedPlaceKey(place.placeKey)}
                onMouseOut={() => setFocusedPlaceKey((prev) => (prev === place.placeKey ? null : prev))}
                onClick={() => {
                  setFocusedPlaceKey(place.placeKey);
                  openPlaceDetail(place);
                }}
              />
              );
            })}

            {currentDayPlaces.length > 1 && (
              <Polyline
                path={routePath.length > 1 ? routePath : currentDayPlaces.map(toLatLngLiteral)}
                options={{ strokeColor: '#1f8ef1', strokeOpacity: 0.92, strokeWeight: 5 }}
              />
            )}
          </GoogleMap>
        )}

        {isRouteLoading && (
          <div className="absolute bottom-3 left-3 z-10 flex items-center gap-2 rounded-full border border-[#cde4f8] bg-white/95 px-3 py-1.5 text-xs font-bold text-[#2d587a] shadow-lg backdrop-blur">
            <Loader2 size={12} className="animate-spin text-[#1f8ef1]" />
            이동 경로 계산 중
          </div>
        )}
      </div>

      {replaceTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-3xl border border-[#d5e8fa] bg-white p-5 shadow-2xl">
            <h3 className="mb-4 text-lg font-black text-[#1f3550]">장소 교체</h3>
            <div className="space-y-3">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={replaceTarget.placeName}
                  onChange={(event) => setReplaceTarget((prev) => ({ ...prev, placeName: event.target.value }))}
                  placeholder="장소명"
                  className="w-full rounded-xl border border-[#d2e8fb] px-3 py-2 text-sm outline-none focus:border-[#1f8ef1]"
                />
                <button
                  onClick={searchReplaceCandidates}
                  disabled={isSearchingPlace}
                  className="inline-flex shrink-0 items-center gap-1 rounded-xl border border-[#cde4f8] bg-[#f5fbff] px-3 py-2 text-sm font-bold text-[#2d587a] hover:border-[#1f8ef1] hover:text-[#1f8ef1] disabled:opacity-60"
                >
                  {isSearchingPlace ? <Loader2 size={14} className="animate-spin" /> : <Search size={14} />}
                  검색
                </button>
              </div>

              {!!replaceSearchError && <p className="text-xs font-semibold text-[#b54747]">{replaceSearchError}</p>}

              {replaceCandidates.length > 0 && (
                <div className="max-h-56 space-y-2 overflow-y-auto rounded-xl border border-[#dbeaf9] bg-[#f8fcff] p-2">
                  {replaceCandidates.map((candidate, index) => (
                    <button
                      key={`${candidate.googlePlaceId || candidate.placeName}-${index}`}
                      onClick={() => selectReplaceCandidate(candidate)}
                      className="w-full rounded-lg border border-[#dbeaf9] bg-white p-2 text-left text-sm hover:border-[#1f8ef1]"
                    >
                      <p className="font-bold text-[#1f3550]">{candidate.placeName}</p>
                      <p className="mt-0.5 text-xs text-[#5b7d9a]">{candidate.address || '주소 정보 없음'}</p>
                      <p className="mt-0.5 text-xs text-[#4b6177]">
                        {candidate.rating ? `평점 ${candidate.rating}` : '평점 없음'}
                        {candidate.userRatingsTotal ? ` (${candidate.userRatingsTotal})` : ''}
                      </p>
                    </button>
                  ))}
                </div>
              )}

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
        <div className="fixed inset-0 z-[80] flex items-end justify-center bg-black/45 p-3 pt-20 backdrop-blur-sm sm:items-start sm:px-4 sm:pb-6 sm:pt-20">
          <div className="flex max-h-[82vh] w-full max-w-xl flex-col rounded-2xl border border-[#d5e8fa] bg-white p-4 shadow-2xl sm:max-h-[calc(100vh-6rem)] sm:rounded-3xl sm:p-5">
            <div className="mb-3 border-b border-[#e5eff8] pb-3">
              <h3 className="text-base font-black leading-snug text-[#1f3550] sm:text-lg">{detailPlace.placeName}</h3>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto pr-1">
              {detailState.loading && (
                <div className="flex items-center gap-2 rounded-xl border border-[#dbeaf9] bg-[#f6fbff] p-3 text-sm text-[#4b6177]">
                  <Loader2 size={16} className="animate-spin text-[#1f8ef1]" />
                  장소 상세 정보를 불러오는 중입니다...
                </div>
              )}

              {!!detailState.error && (
                <p className="rounded-xl border border-[#f1c7c7] bg-[#fff5f5] p-3 text-sm font-semibold text-[#b54747]">
                  {detailState.error}
                </p>
              )}

              {detailState.data && (
                <div className="space-y-3 text-sm text-[#3f5f7b]">
                <div className="flex flex-wrap gap-2">
                  {detailState.data.isOpenNow === true && (
                    <span className="rounded-full border border-emerald-200 bg-emerald-100 px-3 py-1.5 text-xs font-black text-emerald-700">
                      현재 영업중
                    </span>
                  )}
                  {detailState.data.isOpenNow === false && (
                    <span className="rounded-full border border-rose-200 bg-rose-100 px-3 py-1.5 text-xs font-black text-rose-700">
                      현재 영업 종료
                    </span>
                  )}
                  {isTodayClosed(detailState.data.openingHours) && (
                    <span className="rounded-full border border-orange-200 bg-orange-100 px-3 py-1.5 text-xs font-black text-orange-700">
                      오늘 휴무
                    </span>
                  )}
                </div>

                <p><span className="font-bold text-[#1f3550]">주소</span>: {detailState.data.address || '정보 없음'}</p>
                <p>
                  <span className="font-bold text-[#1f3550]">평점</span>:{' '}
                  {detailState.data.rating ? `${detailState.data.rating} (${detailState.data.userRatingsTotal || 0})` : '정보 없음'}
                </p>
                <p><span className="font-bold text-[#1f3550]">영업 상태</span>: {detailState.data.businessStatus || '정보 없음'}</p>
                <p><span className="font-bold text-[#1f3550]">전화번호</span>: {detailState.data.phone || '정보 없음'}</p>

                <div>
                  <p className="font-bold text-[#1f3550]">영업시간</p>
                  {detailState.data.openingHours.length > 0 ? (
                    <ul className="mt-1 space-y-1 rounded-xl border border-[#dbeaf9] bg-[#f8fcff] p-3 text-xs">
                      {detailState.data.openingHours.map((line) => (
                        <li
                          key={line}
                          className={line === getTodayOpeningLine(detailState.data.openingHours) ? 'font-bold text-[#1f3550]' : ''}
                        >
                          {line}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-xs text-[#5f89ad]">영업시간 정보 없음</p>
                  )}
                </div>

                {detailState.data.reviews.length > 0 && (
                  <div>
                    <p className="font-bold text-[#1f3550]">리뷰 요약</p>
                    <div className="mt-1 space-y-2">
                      {detailState.data.reviews.map((review, idx) => (
                        <div key={`${review.author}-${idx}`} className="rounded-xl border border-[#dbeaf9] bg-[#f8fcff] p-3 text-sm sm:text-xs">
                          <p className="font-bold text-[#2f5f86]">
                            {review.author}
                            {review.rating ? ` · ${review.rating}` : ''}
                          </p>
                          <p className="mt-1 text-[#4b6177]">{review.text || '리뷰 본문 없음'}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex flex-wrap gap-2 pt-1">
                  {detailState.data.website && (
                    <a
                      href={detailState.data.website}
                      target="_blank"
                      rel="noreferrer"
                      className="rounded-lg border border-[#cde4f8] bg-[#f5fbff] px-3 py-1.5 text-xs font-bold text-[#2d587a]"
                    >
                      웹사이트
                    </a>
                  )}
                  {detailState.data.mapUrl && (
                    <a
                      href={detailState.data.mapUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="rounded-lg border border-[#cde4f8] bg-[#f5fbff] px-3 py-1.5 text-xs font-bold text-[#2d587a]"
                    >
                      지도에서 보기
                    </a>
                  )}
                </div>
                </div>
              )}
            </div>

            <div className="mt-4 flex justify-end border-t border-[#e5eff8] pt-3">
              <button
                onClick={() => {
                  setDetailPlace(null);
                  setDetailState({ loading: false, error: '', data: null });
                }}
                className="w-full rounded-xl bg-[#1f8ef1] px-4 py-2 text-sm font-bold text-white sm:w-auto"
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



