import { useCallback, useEffect, useState } from "react";
import { fetchWithAuth, buildApiUrl } from "../utils/api";
export default function Admin() {
  const [keyword, setKeyword] = useState("");
  const [users, setUsers] = useState([]);
  const [selected, setSelected] = useState(null);
  const [detail, setDetail] = useState(null);
  const [limit, setLimit] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const loadUsers = useCallback(async () => {
    try {
      const r = await fetchWithAuth(
        buildApiUrl(
          `/api/admin/users?keyword=${encodeURIComponent(keyword)}&limit=50`,
        ),
      );
      const d = await r.json();
      if (!r.ok) throw new Error(d.message || "관리자 권한을 확인해 주세요.");
      setUsers(d.data);
    } catch (e) {
      setMessage(e.message);
    }
  }, [keyword]);
  const loadQuota = useCallback(async (id) => {
    try {
      const r = await fetchWithAuth(
        buildApiUrl(`/api/admin/users/${id}/quota`),
      );
      const d = await r.json();
      if (!r.ok) throw new Error(d.message);
      setDetail(d.data);
      setLimit(String(d.data.effectivePlanPerDayLimit));
    } catch (e) {
      setMessage(e.message || "쿼터를 가져오지 못했습니다.");
    }
  }, []);
  useEffect(() => {
    loadUsers();
  }, [loadUsers]);
  useEffect(() => {
    if (selected) loadQuota(selected);
  }, [selected, loadQuota]);
  const save = async (reset = false) => {
    if (
      !reset &&
      (!Number.isInteger(Number(limit)) ||
        Number(limit) < 1 ||
        Number(limit) > 1000)
    ) {
      setMessage("1~1000 사이 정수를 입력해 주세요.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const r = await fetchWithAuth(
        buildApiUrl(`/api/admin/users/${selected}/quota`),
        {
          method: reset ? "DELETE" : "PUT",
          headers: { "Content-Type": "application/json" },
          body: reset
            ? undefined
            : JSON.stringify({ planPerDayLimit: Number(limit) }),
        },
      );
      const d = await r.json();
      if (!r.ok) throw new Error(d.message);
      setDetail(d.data);
      setLimit(String(d.data.effectivePlanPerDayLimit));
      setMessage(
        reset
          ? "기본 한도로 초기화하고 오늘 사용량을 리필했습니다. 진행 중 예약은 유지됩니다."
          : "일정 생성 한도를 저장했습니다.",
      );
    } catch (e) {
      setMessage(e.message || "저장하지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="mx-auto max-w-5xl px-5 py-10">
      <h1 className="text-2xl font-bold">관리자</h1>
      <p className="mt-2 text-sm text-slate-500">
        사용자별 일정 생성 한도와 오늘 사용량을 관리해요.
      </p>
      <div className="mt-7 grid gap-5 md:grid-cols-[260px_1fr]">
        <aside className="rounded-2xl bg-white p-4">
          <input
            aria-label="이메일 또는 닉네임 검색"
            className="mb-4 w-full rounded-lg bg-slate-50 p-3 text-sm"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="이메일·닉네임 검색"
          />
          {users.map((user) => (
            <button
              key={user.userId}
              onClick={() => {
                setSelected(user.userId);
                setDetail(null);
              }}
              className={`mb-2 block w-full rounded-xl p-3 text-left text-sm ${selected === user.userId ? "bg-blue-50 text-blue-700" : "bg-slate-50"}`}
            >
              <strong className="break-all">{user.email}</strong>
              <small className="mt-1 block text-slate-500">
                {user.nickname}
              </small>
            </button>
          ))}
        </aside>
        <section className="rounded-2xl bg-white p-6">
          {detail ? (
            <>
              <h2 className="font-bold">{detail.email}</h2>
              <div className="my-6 rounded-xl bg-blue-50 p-4 text-sm leading-7">
                <p>
                  일정 생성: 사용·예약 {detail.planPerDayUsed}회 / 남음{" "}
                  {detail.planPerDayRemaining}회
                </p>
                <p>
                  기본 {detail.defaultPlanPerDayLimit}회 · 적용 한도{" "}
                  {detail.effectivePlanPerDayLimit}회
                </p>
              </div>
              <label className="text-sm font-bold">
                하루 일정 생성 한도
                <input
                  className="mt-2 block w-full rounded-lg bg-slate-50 p-3"
                  type="number"
                  min="1"
                  max="1000"
                  value={limit}
                  onChange={(e) => setLimit(e.target.value)}
                />
              </label>
              <div className="mt-5 flex flex-wrap gap-3">
                <button
                  disabled={busy}
                  onClick={() => save()}
                  className="rounded-lg bg-blue-600 px-4 py-3 text-sm font-bold text-white disabled:opacity-50"
                >
                  저장
                </button>
                <button
                  disabled={busy}
                  onClick={() => save(true)}
                  className="rounded-lg bg-slate-100 px-4 py-3 text-sm font-bold disabled:opacity-50"
                >
                  기본값으로 초기화·리필
                </button>
              </div>
            </>
          ) : (
            <p className="text-sm text-slate-500">사용자를 선택해 주세요.</p>
          )}
          {message && (
            <p
              className="mt-5 text-sm leading-relaxed text-slate-600"
              role="status"
            >
              {message}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}
