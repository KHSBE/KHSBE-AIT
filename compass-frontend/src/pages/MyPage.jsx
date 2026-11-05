import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  Plus,
  Search,
  MoreHorizontal,
  Trash2,
  Loader2,
  ArrowRight,
  MapPin,
  RefreshCw,
} from "lucide-react";
import { fetchWithAuth, buildApiUrl } from "../utils/api";
import {
  localDateKey,
  selectTrips,
  tripDateLabel,
  tripLength,
  tripPhase,
} from "../utils/trips.mjs";

const phaseLabels = {
  ongoing: "여행 중",
  upcoming: "다가오는 여행",
  past: "지난 여행",
  undated: "날짜 미정",
};

function DeletePlanDialog({
  plan,
  busy,
  error,
  onCancel,
  onConfirm,
  onReturnFocus,
}) {
  const dialogRef = useRef(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    dialog.showModal();
    return () => {
      dialog.close();
      onReturnFocus();
    };
  }, [onReturnFocus]);
  return (
    <dialog
      ref={dialogRef}
      className="delete-dialog"
      aria-labelledby="delete-title"
      aria-describedby="delete-description"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onCancel();
      }}
    >
      <div className="dialog-content">
        <span className="delete-dialog-icon">
          <Trash2 size={22} />
        </span>
        <h2 id="delete-title">이 여행을 삭제할까요?</h2>
        <p id="delete-description">
          <strong>{plan.title}</strong>
          <br />
          삭제한 일정은 다시 복구할 수 없어요.
        </p>
        {error && (
          <p className="inline-error" role="alert">
            {error}
          </p>
        )}
        <div className="dialog-actions">
          <button
            type="button"
            className="button button-secondary"
            onClick={onCancel}
            disabled={busy}
            autoFocus
          >
            남겨 두기
          </button>
          <button
            type="button"
            className="button button-danger"
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? (
              <>
                <Loader2 size={16} className="animate-spin" /> 삭제 중
              </>
            ) : (
              "삭제하기"
            )}
          </button>
        </div>
      </div>
    </dialog>
  );
}

function TripCard({ plan, today, onDelete }) {
  const phase = tripPhase(plan, today);
  return (
    <article className="trip-card">
      <div className="trip-card-top">
        <span className={`trip-phase phase-${phase}`}>
          <span />
          {phaseLabels[phase]}
        </span>
        <details
          className="trip-menu"
          name="trip-actions"
          onBlur={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget))
              event.currentTarget.open = false;
          }}
          onKeyDown={(event) => {
            if (event.key === "Escape") {
              event.currentTarget.open = false;
              event.currentTarget.querySelector("summary").focus();
            }
          }}
        >
          <summary aria-label={`${plan.title} 더 보기`}>
            <MoreHorizontal size={20} />
          </summary>
          <div>
            <button
              type="button"
              onClick={(event) => {
                const menu = event.currentTarget.closest("details");
                menu.open = false;
                onDelete(plan, menu.querySelector("summary"));
              }}
            >
              <Trash2 size={15} /> 일정 삭제
            </button>
          </div>
        </details>
      </div>
      <Link className="trip-card-link" to={`/plans/${plan.id}`}>
        <p className="trip-destination">{plan.region || "여행지 미정"}</p>
        <h2>{plan.title || "나의 여행"}</h2>
        <p className="trip-date">{tripDateLabel(plan)}</p>
        <div className="trip-card-bottom">
          <span>{tripLength(plan) || "여행 일정"}</span>
          <span className="trip-open">
            일정 열기 <ArrowUpRight size={18} />
          </span>
        </div>
      </Link>
    </article>
  );
}

export default function MyPage() {
  const [plans, setPlans] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [deletingPlanId, setDeletingPlanId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteError, setDeleteError] = useState("");
  const [filter, setFilter] = useState("all");
  const [query, setQuery] = useState("");
  const today = localDateKey();
  const requestRef = useRef(0);
  const deleteTriggerRef = useRef(null);
  const headingRef = useRef(null);
  const returnDeleteFocus = useCallback(() => {
    const target = deleteTriggerRef.current?.isConnected
      ? deleteTriggerRef.current
      : headingRef.current;
    target?.focus();
  }, []);

  const fetchMyPlans = useCallback(async () => {
    const request = ++requestRef.current;
    setIsLoading(true);
    setErrorMessage("");
    try {
      const response = await fetchWithAuth(buildApiUrl("/api/plans"));
      if (!response.ok) throw new Error("일정 목록을 불러오지 못했습니다.");
      const result = await response.json();
      if (request === requestRef.current)
        setPlans(Array.isArray(result?.data) ? result.data : []);
    } catch {
      if (request === requestRef.current)
        setErrorMessage(
          "일정 목록을 불러오지 못했어요. 연결 상태를 확인하고 다시 시도해 주세요.",
        );
    } finally {
      if (request === requestRef.current) setIsLoading(false);
    }
  }, []);
  useEffect(() => {
    fetchMyPlans();
    return () => {
      requestRef.current += 1;
    };
  }, [fetchMyPlans]);

  const confirmDeletePlan = async () => {
    if (!deleteTarget || deletingPlanId !== null) return;
    const planId = deleteTarget.id;
    setDeletingPlanId(planId);
    setDeleteError("");
    try {
      const response = await fetchWithAuth(
        buildApiUrl(`/api/plans/${planId}`),
        { method: "DELETE" },
      );
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(
          data?.message || "일정 삭제에 실패했어요. 다시 시도해 주세요.",
        );
      }
      setPlans((previous) => previous.filter((plan) => plan.id !== planId));
      setDeleteTarget(null);
    } catch (error) {
      setDeleteError(error.message || "일정 삭제에 실패했어요.");
    } finally {
      setDeletingPlanId(null);
    }
  };

  const visiblePlans = selectTrips(plans, { filter, query, today });
  const upcomingCount = plans.filter((plan) =>
    ["upcoming", "ongoing"].includes(tripPhase(plan, today)),
  ).length;
  const filters = [
    { value: "all", label: "전체", count: plans.length },
    { value: "upcoming", label: "예정 · 진행 중", count: upcomingCount },
    {
      value: "past",
      label: "지난 여행",
      count: plans.filter((plan) => tripPhase(plan, today) === "past").length,
    },
  ];

  return (
    <div className="trips-page app-container">
      <header className="trips-heading">
        <div>
          <p className="eyebrow">MY TRIPS</p>
          <h1 ref={headingRef} tabIndex={-1}>
            내 여행
          </h1>
          <p>떠날 계획도, 다녀온 기록도 한곳에.</p>
        </div>
        <Link to="/planner" className="button button-primary">
          <Plus size={17} /> 새 여행 만들기
        </Link>
      </header>
      <div className="trips-toolbar">
        <div className="trip-filters" role="group" aria-label="여행 시기 필터">
          {filters.map((item) => (
            <button
              type="button"
              key={item.value}
              aria-pressed={filter === item.value}
              onClick={() => setFilter(item.value)}
            >
              {item.label}
              <span>{isLoading || errorMessage ? "–" : item.count}</span>
            </button>
          ))}
        </div>
        <label className="trip-search">
          <Search size={17} />
          <span className="sr-only">여행지 또는 일정 이름 검색</span>
          <input
            type="search"
            placeholder="여행지 또는 일정 검색"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
      </div>
      {isLoading ? (
        <div
          className="trip-grid"
          aria-label="내 여행 불러오는 중"
          aria-busy="true"
        >
          {[0, 1, 2].map((key) => (
            <div className="trip-skeleton" key={key}>
              <span />
              <span />
              <span />
            </div>
          ))}
        </div>
      ) : errorMessage ? (
        <div className="trips-state" role="alert">
          <RefreshCw size={26} strokeWidth={1.5} />
          <h2>여행 목록에 연결하지 못했어요</h2>
          <p>{errorMessage}</p>
          <button
            type="button"
            className="button button-secondary"
            onClick={fetchMyPlans}
          >
            다시 불러오기 <RefreshCw size={15} />
          </button>
        </div>
      ) : plans.length === 0 ? (
        <div className="trips-state">
          <MapPin size={30} strokeWidth={1.4} />
          <h2>첫 여행을 여기 남겨 보세요.</h2>
          <p>여행지와 취향을 선택하면 나만의 일정이 저장돼요.</p>
          <Link to="/planner" className="button button-primary">
            첫 여행 만들기 <ArrowRight size={16} />
          </Link>
        </div>
      ) : (
        <>
          <p className="trips-result" role="status">
            {query
              ? `검색 결과 ${visiblePlans.length}개`
              : "진행 중인 여행과 가까운 여행부터 보여 드려요."}
          </p>
          {visiblePlans.length > 0 ? (
            <div className="trip-grid">
              {visiblePlans.map((plan) => (
                <TripCard
                  key={plan.id}
                  plan={plan}
                  today={today}
                  onDelete={(target, trigger) => {
                    deleteTriggerRef.current = trigger;
                    setDeleteError("");
                    setDeleteTarget(target);
                  }}
                />
              ))}
            </div>
          ) : (
            <div className="trips-state">
              <Search size={27} strokeWidth={1.5} />
              <h2>
                {query
                  ? "일치하는 여행이 없어요."
                  : "아직 이 시기의 여행이 없어요."}
              </h2>
              <p>다른 검색어나 시기 필터를 선택해 보세요.</p>
              <button
                type="button"
                className="button button-secondary"
                onClick={() => {
                  setQuery("");
                  setFilter("all");
                }}
              >
                전체 여행 보기
              </button>
            </div>
          )}
        </>
      )}
      {deleteTarget && (
        <DeletePlanDialog
          plan={deleteTarget}
          busy={deletingPlanId !== null}
          error={deleteError}
          onCancel={() => {
            if (deletingPlanId === null) setDeleteTarget(null);
          }}
          onConfirm={confirmDeletePlan}
          onReturnFocus={returnDeleteFocus}
        />
      )}
    </div>
  );
}
