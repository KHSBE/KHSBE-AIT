import { useCallback, useEffect, useMemo, useState } from 'react';
import { fetchWithAuth, buildApiUrl } from '../utils/api';

export default function Admin() {
  const [checkingAdmin, setCheckingAdmin] = useState(true);
  const [isAdmin, setIsAdmin] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [users, setUsers] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [quotaDetail, setQuotaDetail] = useState(null);
  const [planLimitInput, setPlanLimitInput] = useState('');
  const [chatLimitInput, setChatLimitInput] = useState('');
  const [isUsersLoading, setIsUsersLoading] = useState(false);
  const [isQuotaLoading, setIsQuotaLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState('');

  const headers = useMemo(() => {
    const token = localStorage.getItem('accessToken');
    const h = { 'Content-Type': 'application/json' };
    if (token) {
      h.Authorization = `Bearer ${token}`;
    }
    return h;
  }, []);

  const fetchUsers = useCallback(async () => {
    setIsUsersLoading(true);
    try {
      const query = keyword.trim()
        ? `?keyword=${encodeURIComponent(keyword.trim())}&limit=50`
        : '?limit=50';

      const response = await fetchWithAuth(buildApiUrl(`/api/admin/users${query}`), { headers });
      if (response.status === 401 || response.status === 403) {
        setIsAdmin(false);
        return;
      }
      if (!response.ok) {
        throw new Error('사용자 목록 조회 실패');
      }

      const data = await response.json();
      const list = data?.data || [];
      setUsers(list);
      setIsAdmin(true);

      if (!selectedUserId && list.length > 0) {
        setSelectedUserId(list[0].userId);
      }
    } catch (error) {
      console.error(error);
      setFeedback('사용자 목록을 가져오지 못했습니다.');
    } finally {
      setIsUsersLoading(false);
      setCheckingAdmin(false);
    }
  }, [headers, keyword, selectedUserId]);

  const fetchQuota = useCallback(async (userId) => {
    if (!userId || !isAdmin) return;
    setIsQuotaLoading(true);
    try {
      const response = await fetchWithAuth(buildApiUrl(`/api/admin/users/${userId}/quota`), {
        headers
      });
      if (!response.ok) {
        throw new Error('쿼터 상세 조회 실패');
      }

      const data = await response.json();
      const detail = data?.data;
      setQuotaDetail(detail);
      setPlanLimitInput(String(detail?.overridePlanPerDayLimit ?? detail?.effectivePlanPerDayLimit ?? ''));
      setChatLimitInput(String(detail?.overrideChatPerDayLimit ?? detail?.effectiveChatPerDayLimit ?? ''));
    } catch (error) {
      console.error(error);
      setFeedback('선택한 사용자의 쿼터 정보를 가져오지 못했습니다.');
    } finally {
      setIsQuotaLoading(false);
    }
  }, [headers, isAdmin]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  useEffect(() => {
    if (selectedUserId) {
      fetchQuota(selectedUserId);
    }
  }, [selectedUserId, fetchQuota]);

  const handleSaveQuota = async () => {
    if (!selectedUserId) return;
    const planLimit = Number(planLimitInput);
    const chatLimit = Number(chatLimitInput);

    if (!Number.isInteger(planLimit) || planLimit < 1 || !Number.isInteger(chatLimit) || chatLimit < 1) {
      setFeedback('쿼터 값은 1 이상 정수만 입력 가능합니다.');
      return;
    }

    setIsSaving(true);
    setFeedback('');
    try {
      const response = await fetchWithAuth(buildApiUrl(`/api/admin/users/${selectedUserId}/quota`), {
        method: 'PUT',
        headers,
        body: JSON.stringify({
          planPerDayLimit: planLimit,
          chatPerDayLimit: chatLimit
        })
      });

      const data = await response.json();
      if (!response.ok || !data?.success) {
        throw new Error(data?.message || '쿼터 저장 실패');
      }

      setQuotaDetail(data.data);
      setFeedback('쿼터를 저장했습니다.');
    } catch (error) {
      console.error(error);
      setFeedback(error.message || '쿼터 저장에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleResetQuota = async () => {
    if (!selectedUserId) return;
    setIsSaving(true);
    setFeedback('');
    try {
      const response = await fetchWithAuth(buildApiUrl(`/api/admin/users/${selectedUserId}/quota`), {
        method: 'DELETE',
        headers
      });

      const data = await response.json();
      if (!response.ok || !data?.success) {
        throw new Error(data?.message || '쿼터 초기화 실패');
      }

      setQuotaDetail(data.data);
      setPlanLimitInput(String(data.data.effectivePlanPerDayLimit));
      setChatLimitInput(String(data.data.effectiveChatPerDayLimit));
      setFeedback('기본값으로 초기화했습니다.');
    } catch (error) {
      console.error(error);
      setFeedback(error.message || '쿼터 초기화에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  if (checkingAdmin) {
    return (
      <div className="mx-auto max-w-6xl p-6">
        <div className="rounded-2xl border border-[#d8e8f8] bg-white p-6 text-sm text-[#44617b]">권한 확인 중...</div>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-6xl p-6">
        <div className="rounded-2xl border border-[#f2cfcf] bg-[#fff7f7] p-6 text-sm font-semibold text-[#9b4141]">
          관리자 권한이 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl p-4 md:p-8">
      <div className="mb-6 rounded-3xl border border-[#d6e9fa] bg-white p-5 shadow-sm">
        <h1 className="text-2xl font-black text-[#1f3550]">관리자 페이지</h1>
      </div>

      <div className="grid gap-4 lg:grid-cols-[360px_minmax(0,1fr)]">
        <section className="rounded-3xl border border-[#d6e9fa] bg-white p-4 shadow-sm">
          <div className="mb-3 flex gap-2">
            <input
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && fetchUsers()}
              placeholder="이메일/닉네임 검색"
              className="w-full rounded-xl border border-[#cfe4f7] px-3 py-2 text-sm focus:border-[#1f8ef1] focus:outline-none"
            />
            <button
              type="button"
              onClick={fetchUsers}
              className="rounded-xl bg-[#1f8ef1] px-3 py-2 text-sm font-bold text-white hover:bg-[#1878cc]"
            >
              검색
            </button>
          </div>

          <div className="space-y-2">
            {isUsersLoading && <p className="text-sm text-[#5e7d97]">사용자 목록 로딩 중...</p>}
            {!isUsersLoading && users.length === 0 && <p className="text-sm text-[#5e7d97]">사용자가 없습니다.</p>}
            {users.map((user) => (
              <button
                key={user.userId}
                type="button"
                onClick={() => setSelectedUserId(user.userId)}
                className={`w-full rounded-xl border px-3 py-2 text-left transition ${
                  selectedUserId === user.userId
                    ? 'border-[#1f8ef1] bg-[#eff8ff]'
                    : 'border-[#d7e8f8] bg-white hover:border-[#9fcaf1]'
                }`}
              >
                <p className="truncate text-sm font-bold text-[#1f3550]">{user.email}</p>
                <p className="truncate text-xs text-[#5e7d97]">{user.nickname || '(닉네임 없음)'}</p>
              </button>
            ))}
          </div>
        </section>

        <section className="rounded-3xl border border-[#d6e9fa] bg-white p-5 shadow-sm">
          {!selectedUserId && <p className="text-sm text-[#5e7d97]">왼쪽에서 사용자를 선택해 주세요.</p>}
          {selectedUserId && (
            <>
              {isQuotaLoading && <p className="text-sm text-[#5e7d97]">쿼터 정보 로딩 중...</p>}
              {!isQuotaLoading && quotaDetail && (
                <div className="space-y-5">
                  <div className="rounded-2xl border border-[#d9ebfb] bg-[#f7fbff] p-4">
                    <p className="text-sm font-bold text-[#1f3550]">{quotaDetail.email}</p>
                    <p className="mt-1 text-xs text-[#5e7d97]">{quotaDetail.nickname || '(닉네임 없음)'}</p>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    <div className="rounded-2xl border border-[#d9ebfb] p-4">
                      <p className="text-xs font-bold text-[#4f7392]">일정 생성 쿼터</p>
                      <p className="mt-2 text-sm text-[#1f3550]">
                        기본 {quotaDetail.defaultPlanPerDayLimit} / 오버라이드 {quotaDetail.overridePlanPerDayLimit ?? '-'} / 적용 {quotaDetail.effectivePlanPerDayLimit}
                      </p>
                      <p className="mt-1 text-sm text-[#1f3550]">
                        사용 {quotaDetail.planPerDayUsed} · 남음 {quotaDetail.planPerDayRemaining}
                      </p>
                    </div>

                    <div className="rounded-2xl border border-[#d9ebfb] p-4">
                      <p className="text-xs font-bold text-[#4f7392]">채팅 쿼터</p>
                      <p className="mt-2 text-sm text-[#1f3550]">
                        기본 {quotaDetail.defaultChatPerDayLimit} / 오버라이드 {quotaDetail.overrideChatPerDayLimit ?? '-'} / 적용 {quotaDetail.effectiveChatPerDayLimit}
                      </p>
                      <p className="mt-1 text-sm text-[#1f3550]">
                        사용 {quotaDetail.chatPerDayUsed} · 남음 {quotaDetail.chatPerDayRemaining}
                      </p>
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-2">
                    <label className="rounded-2xl border border-[#d9ebfb] p-4 text-sm">
                      <p className="mb-2 font-bold text-[#4f7392]">일정 생성 제한(일)</p>
                      <input
                        type="number"
                        min={1}
                        value={planLimitInput}
                        onChange={(event) => setPlanLimitInput(event.target.value)}
                        className="w-full rounded-xl border border-[#cfe4f7] px-3 py-2 focus:border-[#1f8ef1] focus:outline-none"
                      />
                    </label>

                    <label className="rounded-2xl border border-[#d9ebfb] p-4 text-sm">
                      <p className="mb-2 font-bold text-[#4f7392]">채팅 제한(일)</p>
                      <input
                        type="number"
                        min={1}
                        value={chatLimitInput}
                        onChange={(event) => setChatLimitInput(event.target.value)}
                        className="w-full rounded-xl border border-[#cfe4f7] px-3 py-2 focus:border-[#1f8ef1] focus:outline-none"
                      />
                    </label>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={handleSaveQuota}
                      disabled={isSaving}
                      className="rounded-xl bg-[#1f8ef1] px-4 py-2 text-sm font-bold text-white hover:bg-[#1878cc] disabled:opacity-60"
                    >
                      {isSaving ? '저장 중...' : '쿼터 저장'}
                    </button>
                    <button
                      type="button"
                      onClick={handleResetQuota}
                      disabled={isSaving}
                      className="rounded-xl border border-[#d6e9fa] bg-white px-4 py-2 text-sm font-bold text-[#315a7d] hover:border-[#95c3eb] disabled:opacity-60"
                    >
                      기본값으로 초기화
                    </button>
                  </div>

                  {feedback && (
                    <p className="rounded-xl border border-[#d9ebfb] bg-[#f7fbff] px-3 py-2 text-sm font-semibold text-[#315a7d]">
                      {feedback}
                    </p>
                  )}
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
