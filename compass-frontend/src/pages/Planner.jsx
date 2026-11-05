import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useJsApiLoader } from "@react-google-maps/api";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Compass,
  Loader2,
  MapPin,
  Sparkles,
  X,
} from "lucide-react";
import PlaceSearch from "../components/PlaceSearch";
import { fetchWithAuth, buildApiUrl } from "../utils/api";
import "./Planner.css";

const LIBRARIES = ["places"];
const STEPS = [
  "여행지와 날짜",
  "함께하는 사람",
  "나의 여행 스타일",
  "꼭 반영할 조건",
  "여행 준비 완료",
];
const COMPANIONS = [
  ["SOLO", "혼자", "내 취향대로, 가볍게"],
  ["COUPLE", "연인과", "둘만의 시간을 여유롭게"],
  ["FRIENDS", "친구와", "함께 즐기는 새로운 경험"],
  ["FAMILY", "가족과", "모두에게 편안한 여행"],
];
const INTERESTS = [
  ["FOOD", "현지 맛집"],
  ["CAFE", "카페와 디저트"],
  ["NATURE", "자연과 산책"],
  ["CULTURE", "문화와 명소"],
  ["SHOPPING", "쇼핑"],
  ["EXPERIENCE", "체험과 액티비티"],
];
const PACES = [
  ["RELAXED", "여유롭게", "하루 2~3곳 · 충분한 휴식"],
  ["BALANCED", "균형 있게", "하루 3~4곳 · 적당한 활동"],
  ["ACTIVE", "알차게", "하루 4~5곳 · 다양한 경험"],
];
const initial = {
  destination: null,
  startDate: "",
  endDate: "",
  arrivalTime: "",
  departureTime: "",
  companion: "SOLO",
  adults: 1,
  children: 0,
  seniors: 0,
  interests: ["FOOD", "CULTURE"],
  pace: "BALANCED",
  accommodation: null,
  required: [],
  additionalNotes: "",
};
const label = (list, key) => list.find(([id]) => id === key)?.[1] || key;
const today = () =>
  new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(
    new Date(),
  );
const readDraft = () => {
  try {
    const d = JSON.parse(localStorage.getItem("compass.tripDraft.v1"));
    return d ? { ...initial, ...d } : initial;
  } catch {
    return initial;
  }
};

export default function Planner() {
  const navigate = useNavigate();
  const [form, setForm] = useState(readDraft);
  const [step, setStep] = useState(0);
  const [quota, setQuota] = useState(null);
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const requestRef = useRef(localStorage.getItem("compass.tripRequest.v1"));
  const sending = useRef(false);
  const { isLoaded, loadError } = useJsApiLoader({
    id: "google-map-script",
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY || "",
    libraries: LIBRARIES,
    language: "ko",
    region: "KR",
  });
  const update = (patch) => {
    setForm((v) => ({ ...v, ...patch }));
    setError("");
    requestRef.current = null;
    localStorage.removeItem("compass.tripRequest.v1");
  };
  useEffect(() => {
    localStorage.setItem("compass.tripDraft.v1", JSON.stringify(form));
  }, [form]);
  useEffect(() => {
    fetchWithAuth(buildApiUrl("/api/agent/quota"))
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => {
        if (d?.data) setQuota(d.data);
      })
      .catch(() => {});
  }, []);
  useEffect(() => {
    if (!pending) return;
    const timer = setInterval(() => setElapsed((v) => v + 1), 1000);
    return () => clearInterval(timer);
  }, [pending]);
  const validate = () => {
    if (step === 0) {
      if (!form.destination)
        return "검색 결과에서 여행할 도시를 선택해 주세요.";
      const days =
        (Date.parse(form.endDate) - Date.parse(form.startDate)) / 86400000 + 1;
      if (
        !form.startDate ||
        !form.endDate ||
        form.startDate < today() ||
        days < 1 ||
        days > 7
      )
        return "오늘 이후의 1~7일 여행 날짜를 선택해 주세요.";
      if (
        days === 1 &&
        form.arrivalTime &&
        form.departureTime &&
        form.arrivalTime >= form.departureTime
      )
        return "출발 시각은 도착 시각보다 늦어야 해요.";
    }
    if (
      step === 1 &&
      (!Number.isInteger(Number(form.adults)) ||
        Number(form.adults) < 1 ||
        Number(form.adults) > 20 ||
        !Number.isInteger(Number(form.children)) ||
        Number(form.children) < 0 ||
        Number(form.children) > 10 ||
        !Number.isInteger(Number(form.seniors)) ||
        Number(form.seniors) < 0 ||
        Number(form.seniors) > 20)
    )
      return "여행 인원을 범위 안의 정수로 입력해 주세요.";
    if (step === 1 && Number(form.seniors) > Number(form.adults))
      return "어르신 수는 성인 수를 넘을 수 없어요.";
    if (step === 2 && !form.interests.length)
      return "관심 있는 여행 스타일을 하나 이상 선택해 주세요.";
    return "";
  };
  const next = () => {
    const message = validate();
    if (message) {
      setError(message);
      return;
    }
    setError("");
    setStep((s) => Math.min(4, s + 1));
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  const generate = async () => {
    if (sending.current) return;
    sending.current = true;
    setPending(true);
    setElapsed(0);
    setError("");
    const id = requestRef.current || crypto.randomUUID();
    requestRef.current = id;
    localStorage.setItem("compass.tripRequest.v1", id);
    const payload = {
      requestId: id,
      destinationPlaceId: form.destination.place_id,
      startDate: form.startDate,
      endDate: form.endDate,
      arrivalTime: form.arrivalTime || null,
      departureTime: form.departureTime || null,
      companion: form.companion,
      adults: Number(form.adults),
      children: Number(form.children),
      seniors: Number(form.seniors),
      interests: form.interests,
      pace: form.pace,
      accommodationPlaceId: form.accommodation?.place_id || null,
      requiredPlaceIds: form.required.map((p) => p.place_id),
      additionalNotes: form.additionalNotes.trim(),
    };
    try {
      const response = await fetchWithAuth(buildApiUrl("/api/agent/plan"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const data = await response.json();
      if (!response.ok || !data.success)
        throw new Error(
          data.message ||
            "일정을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.",
        );
      if (!data.data?.planId)
        throw new Error(
          "저장된 일정 정보를 확인하지 못했습니다. 내 일정에서 확인해 주세요.",
        );
      localStorage.removeItem("compass.tripDraft.v1");
      localStorage.removeItem("compass.tripRequest.v1");
      navigate(`/plans/${data.data.planId}`);
    } catch (e) {
      setError(
        e instanceof TypeError
          ? "서버 연결이 지연되거나 끊겼습니다. 잠시 후 같은 요청으로 다시 시도해 주세요."
          : e.message,
      );
    } finally {
      sending.current = false;
      setPending(false);
      fetchWithAuth(buildApiUrl("/api/agent/quota"))
        .then((response) => (response.ok ? response.json() : null))
        .then((result) => {
          if (result?.data) setQuota(result.data);
        })
        .catch(() => {});
    }
  };
  return (
    <div className="trip-builder">
      <header className="builder-intro">
        <div>
          <span className="builder-eyebrow">
            <Compass size={15} /> YOUR NEXT JOURNEY
          </span>
          <h1>가고 싶은 여행을 골라보세요.</h1>
          <p>
            당신의 선택을 바탕으로 실제 장소를 확인하고 여행 초안을 만들어요.
          </p>
        </div>
        <span className="quota-chip">
          {quota
            ? `오늘 ${quota.planPerDayRemaining}/${quota.planPerDayLimit}회 남음`
            : "나만의 여행 계획"}
        </span>
      </header>
      <div className="builder-layout">
        <aside className="builder-progress">
          <p>여행 만들기</p>
          <ol>
            {STEPS.map((name, i) => (
              <li
                key={name}
                className={step === i ? "current" : step > i ? "complete" : ""}
              >
                <button
                  type="button"
                  disabled={pending || i > step}
                  onClick={() => {
                    setStep(i);
                    setError("");
                  }}
                  aria-current={step === i ? "step" : undefined}
                >
                  <span>{step > i ? <Check size={15} /> : `0${i + 1}`}</span>
                  {name}
                </button>
              </li>
            ))}
          </ol>
          <div className="builder-note">
            <MapPin size={20} />
            <strong>실제 장소를 바탕으로</strong>
            <p>
              Google Maps 정보를 참고해 장소를 확인해요. 영업시간과 예약 여부는
              방문 전 다시 확인해 주세요.
            </p>
          </div>
        </aside>
        <section className="builder-panel" aria-busy={pending}>
          <div className="step-caption">
            STEP {String(step + 1).padStart(2, "0")} / 05
          </div>
          {pending ? (
            <div className="generation-state" role="status">
              <div className="generation-orbit">
                <Sparkles size={36} />
              </div>
              <h2>당신의 여행을 설계하고 있어요.</h2>
              <p>
                {elapsed < 15
                  ? "여행 조건과 장소 정보를 확인하고 있어요."
                  : elapsed < 55
                    ? "방문할 장소를 찾고, 날짜별로 배치하고 있어요."
                    : "장소와 시간표를 확인하고 있어요. 잠시만 더 기다려 주세요."}
              </p>
              <span>{elapsed}초 경과</span>
              <small>
                확인할 장소가 많으면 몇 분 걸릴 수 있어요.
                <br />
                같은 요청을 여러 번 보내지 않아도 됩니다.
              </small>
            </div>
          ) : (
            <>
              {step === 0 && (
                <>
                  <h2>어디로, 언제 떠나나요?</h2>
                  <p className="step-description">
                    도시를 고르면 주변 장소까지 함께 살펴볼게요.
                  </p>
                  <PlaceSearch
                    label="여행할 도시"
                    value={form.destination}
                    onChange={(destination) =>
                      update({ destination, required: [], accommodation: null })
                    }
                    ready={isLoaded}
                    city
                  />
                  {(loadError || !import.meta.env.VITE_GOOGLE_MAPS_API_KEY) && (
                    <p role="alert" className="field-error">
                      지도 검색을 불러오지 못했어요. 지도 API 키 설정을 확인해
                      주세요.
                    </p>
                  )}
                  <div className="field-grid">
                    <label>
                      가는 날
                      <input
                        type="date"
                        min={today()}
                        value={form.startDate}
                        onChange={(e) => update({ startDate: e.target.value })}
                      />
                    </label>
                    <label>
                      오는 날
                      <input
                        type="date"
                        min={form.startDate || today()}
                        value={form.endDate}
                        onChange={(e) => update({ endDate: e.target.value })}
                      />
                    </label>
                  </div>
                  <div className="field-grid">
                    <label>
                      첫날 여행 시작 시각 <small>선택</small>
                      <input
                        type="time"
                        value={form.arrivalTime}
                        onChange={(e) =>
                          update({ arrivalTime: e.target.value })
                        }
                      />
                    </label>
                    <label>
                      마지막 날 여행 종료 시각 <small>선택</small>
                      <input
                        type="time"
                        value={form.departureTime}
                        onChange={(e) =>
                          update({ departureTime: e.target.value })
                        }
                      />
                    </label>
                  </div>
                  <p className="field-hint">
                    우선 1~7일 여행을 지원해요. 비워둔 시각은 오전 9시~오후 9시
                    안에서 계획해요.
                  </p>
                </>
              )}
              {step === 1 && (
                <>
                  <h2>누구와 함께하나요?</h2>
                  <p className="step-description">
                    함께하는 사람에게 맞춰 여행의 속도를 조절해요.
                  </p>
                  <div className="choice-grid">
                    {COMPANIONS.map(([id, name, desc]) => (
                      <button
                        type="button"
                        key={id}
                        className={`choice-card ${form.companion === id ? "selected" : ""}`}
                        aria-pressed={form.companion === id}
                        onClick={() =>
                          update({
                            companion: id,
                            ...(id === "SOLO"
                              ? { adults: 1, children: 0, seniors: 0 }
                              : {}),
                          })
                        }
                      >
                        <strong>{name}</strong>
                        <small>{desc}</small>
                      </button>
                    ))}
                  </div>
                  <div className="field-grid counts">
                    <label>
                      성인
                      <input
                        type="number"
                        min="1"
                        max="20"
                        disabled={form.companion === "SOLO"}
                        value={form.adults}
                        onChange={(e) => update({ adults: e.target.value })}
                      />
                    </label>
                    <label>
                      아이
                      <input
                        type="number"
                        min="0"
                        max="10"
                        disabled={form.companion === "SOLO"}
                        value={form.children}
                        onChange={(e) => update({ children: e.target.value })}
                      />
                    </label>
                    <label>
                      성인 중 어르신
                      <input
                        type="number"
                        min="0"
                        max="20"
                        value={form.seniors}
                        onChange={(e) => update({ seniors: e.target.value })}
                      />
                    </label>
                  </div>
                </>
              )}
              {step === 2 && (
                <>
                  <h2>어떤 순간을 좋아하세요?</h2>
                  <p className="step-description">
                    관심사를 최대 5개 골라주세요.
                  </p>
                  <div className="interest-grid">
                    {INTERESTS.map(([id, name]) => (
                      <button
                        key={id}
                        type="button"
                        className={
                          form.interests.includes(id) ? "selected" : ""
                        }
                        aria-pressed={form.interests.includes(id)}
                        onClick={() =>
                          update({
                            interests: form.interests.includes(id)
                              ? form.interests.filter((v) => v !== id)
                              : [...form.interests, id].slice(0, 5),
                          })
                        }
                      >
                        {name}
                        {form.interests.includes(id) && <Check size={16} />}
                      </button>
                    ))}
                  </div>
                  <h3>하루를 얼마나 채울까요?</h3>
                  <div className="pace-list">
                    {PACES.map(([id, name, desc]) => (
                      <button
                        key={id}
                        type="button"
                        className={form.pace === id ? "selected" : ""}
                        aria-pressed={form.pace === id}
                        onClick={() => update({ pace: id })}
                      >
                        <strong>{name}</strong>
                        <span>{desc}</span>
                        <span className="radio-dot" />
                      </button>
                    ))}
                  </div>
                </>
              )}
              {step === 3 && (
                <>
                  <h2>꼭 반영할 게 있나요?</h2>
                  <p className="step-description">
                    모두 선택사항이에요. 여행할 지역 안의 장소를 골라주세요.
                  </p>
                  <PlaceSearch
                    label="예약한 숙소"
                    value={form.accommodation}
                    onChange={(accommodation) => update({ accommodation })}
                    ready={isLoaded}
                    lodging
                    hint="미정이면 비워두세요. 예약이나 숙박 가능 여부를 보장하지 않아요."
                  />
                  {form.required.length < 5 && (
                    <PlaceSearch
                      key={form.required.length}
                      label="꼭 가고 싶은 장소"
                      value={null}
                      onChange={(p) => {
                        if (
                          p &&
                          !form.required.some((v) => v.place_id === p.place_id)
                        )
                          update({ required: [...form.required, p] });
                      }}
                      ready={isLoaded}
                    />
                  )}
                  <div className="required-chips">
                    {form.required.map((p) => (
                      <span key={p.place_id}>
                        {p.name}
                        <button
                          type="button"
                          aria-label={`${p.name} 제외`}
                          onClick={() =>
                            update({
                              required: form.required.filter(
                                (v) => v.place_id !== p.place_id,
                              ),
                            })
                          }
                        >
                          <X size={14} />
                        </button>
                      </span>
                    ))}
                  </div>
                  <label className="notes-label">
                    피하고 싶은 것, 추가 요청
                    <textarea
                      maxLength={500}
                      rows={3}
                      value={form.additionalNotes}
                      placeholder="예: 해산물은 제외하고, 계단이 적은 곳을 선호해요."
                      onChange={(e) =>
                        update({ additionalNotes: e.target.value })
                      }
                    />
                  </label>
                </>
              )}
              {step === 4 && (
                <>
                  <h2>{form.destination?.name}, 이렇게 떠나볼까요?</h2>
                  <p className="step-description">
                    선택한 조건을 확인해 주세요. 각 항목을 눌러 바꿀 수 있어요.
                  </p>
                  <div className="trip-review">
                    {[
                      [
                        "여행지와 날짜",
                        `${form.destination?.name} · ${form.startDate} ~ ${form.endDate}`,
                        0,
                      ],
                      [
                        "함께하는 사람",
                        `${label(COMPANIONS, form.companion)} · 성인 ${form.adults}명 / 아이 ${form.children}명 / 어르신 ${form.seniors}명`,
                        1,
                      ],
                      [
                        "여행 스타일",
                        `${form.interests.map((v) => label(INTERESTS, v)).join(", ")} · ${label(PACES, form.pace)}`,
                        2,
                      ],
                      [
                        "숙소와 필수 장소",
                        `${form.accommodation?.name || "숙소 미정"} · 필수 장소 ${form.required.length}곳`,
                        3,
                      ],
                    ].map(([title, text, target]) => (
                      <button
                        type="button"
                        key={title}
                        onClick={() => setStep(target)}
                      >
                        <span>
                          {title}
                          <strong>{text}</strong>
                        </span>
                        <ArrowRight size={17} />
                      </button>
                    ))}
                  </div>
                  <div className="generation-disclosure">
                    <Sparkles size={18} />
                    <p>
                      AI가 실제 장소 정보를 참고해 만드는 여행 초안이에요. 임시
                      휴무·예약 여부·실제 이동시간은 방문 전에 확인해 주세요.
                    </p>
                  </div>
                </>
              )}
              {error && (
                <div className="builder-error" role="alert">
                  {error}
                </div>
              )}
              <footer className="builder-actions">
                <button
                  type="button"
                  className="back-action"
                  disabled={step === 0}
                  onClick={() => {
                    setStep((v) => v - 1);
                    setError("");
                  }}
                >
                  <ArrowLeft size={17} /> 이전
                </button>
                {step < 4 ? (
                  <button
                    className="primary-action"
                    type="button"
                    onClick={next}
                  >
                    다음 <ArrowRight size={17} />
                  </button>
                ) : (
                  <button
                    className="primary-action"
                    type="button"
                    disabled={quota?.planPerDayRemaining === 0}
                    onClick={generate}
                  >
                    <Sparkles size={17} /> 여행 일정 만들기
                  </button>
                )}
              </footer>
            </>
          )}
          {pending && (
            <div className="builder-loading-bar">
              <Loader2 size={16} className="animate-spin" /> 장소 확인과 일정
              생성을 진행 중입니다.
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
