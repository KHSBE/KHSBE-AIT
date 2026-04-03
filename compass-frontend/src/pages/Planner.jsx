import { useState, useRef, useEffect, useCallback } from 'react';
import {
  Send,
  Bot,
  Calendar,
  Users,
  Star,
  Loader2,
  PlaneTakeoff,
  PlaneLanding,
  BedDouble,
  MessageSquare,
  Plus,
  Trash2,
  PanelLeftClose,
  PanelLeftOpen
} from 'lucide-react';
import { fetchWithAuth, buildApiUrl } from '../utils/api';

const BOT_WELCOME_MESSAGE = '??덈??뤾쉭?? ??逾믤에???六???醫딄돌????좎몵?醫???';
const BOT_NEW_CHAT_MESSAGE = '??덉쨮????六??④쑵?????뽰삂???득틦??뒄? 筌뤴뫗?삼쭪????????깆뱽 ???젻雅뚯눘苑??';

export default function Planner() {
  const [sessions, setSessions] = useState([]);
  const [currentSessionId, setCurrentSessionId] = useState(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isMobile, setIsMobile] = useState(false);

  const [messages, setMessages] = useState([{ sender: 'bot', text: BOT_WELCOME_MESSAGE }]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [toast, setToast] = useState({ visible: false, message: '' });
  const [quota, setQuota] = useState(null);
  const [isQuotaLoading, setIsQuotaLoading] = useState(false);
  const [formData, setFormData] = useState({
    departure: '',
    dest: '',
    accommodation: '',
    startDate: '',
    endDate: '',
    companion: '??깆쁽',
    style: ''
  });

  const messagesEndRef = useRef(null);

  useEffect(() => {
    const syncLayout = () => {
      const mobile = window.innerWidth < 1024;
      setIsMobile(mobile);
      setIsSidebarOpen(!mobile);
    };

    syncLayout();
    window.addEventListener('resize', syncLayout);
    return () => window.removeEventListener('resize', syncLayout);
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!toast.visible) return undefined;
    const timer = setTimeout(() => {
      setToast({ visible: false, message: '' });
    }, 2600);
    return () => clearTimeout(timer);
  }, [toast.visible]);

  const showQuotaToast = (message) => {
    setToast({
      visible: true,
      message: message || '?遺욧퍕 ??뺣즲???λ뜃???됰뮸??덈뼄. ?醫롫뻻 ????쇰뻻 ??뺣즲??雅뚯눘苑??'
    });
  };

  const getHeaders = (isJson = false) => {
    const headers = {};
    if (isJson) headers['Content-Type'] = 'application/json';

    const token = localStorage.getItem('accessToken');
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
  };

  const fetchSessions = useCallback(async () => {
    if (!localStorage.getItem('accessToken')) return;

    try {
      const response = await fetchWithAuth(buildApiUrl('/api/chat/sessions'), { headers: getHeaders() });
      if (!response.ok) return;
      const data = await response.json();

      if (data.success) {
        setSessions(data.data || []);
      }
    } catch (error) {
      console.error('?紐꾨?筌뤴뫖以?嚥≪뮆逾???쎈솭:', error);
    }
  }, []);

  const fetchQuota = useCallback(async () => {
    if (!localStorage.getItem('accessToken')) return;

    setIsQuotaLoading(true);
    try {
      const response = await fetchWithAuth(buildApiUrl('/api/agent/quota'), {
        headers: getHeaders()
      });

      if (!response.ok) return;

      const data = await response.json();
      if (data?.success) {
        setQuota(data.data);
      }
    } catch (error) {
      console.error('?묒눛苑?鈺곌퀬????쎈솭:', error);
      setQuota(null);
    } finally {
      setIsQuotaLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  useEffect(() => {
    fetchQuota();
  }, [currentSessionId, fetchQuota]);

  const loadSession = async (sessionId) => {
    setCurrentSessionId(sessionId);
    setMessages([]);
    if (isMobile) setIsSidebarOpen(false);
    setIsLoading(true);

    try {
      const response = await fetchWithAuth(buildApiUrl(`/api/chat/sessions/${sessionId}/messages`), {
        headers: getHeaders()
      });
      const data = await response.json();

      if (data.success) {
        const history = (data.data || []).map((msg) => ({
          sender: msg.senderRole === 'USER' ? 'user' : 'bot',
          text: msg.content
        }));
        setMessages(history.length > 0 ? history : [{ sender: 'bot', text: BOT_WELCOME_MESSAGE }]);
      }
    } catch (error) {
      console.error('?紐꾨??븍뜄???븍┛ ??쎈솭:', error);
      setMessages([{ sender: 'bot', text: '?紐꾨???븍뜄???? 筌륁궢六??щ빍?? ?醫롫뻻 ????쇰뻻 ??뺣즲??雅뚯눘苑??' }]);
    } finally {
      setIsLoading(false);
    }
  };

  const startNewChat = () => {
    setCurrentSessionId(null);
    setMessages([{ sender: 'bot', text: BOT_NEW_CHAT_MESSAGE }]);
    if (isMobile) setIsSidebarOpen(false);
  };

  const deleteSession = async (event, sessionId) => {
    event.stopPropagation();
    if (!window.confirm('??????疫꿸퀡以???????뤿뻻野껋쥙???뉙돱?')) return;

    try {
      await fetchWithAuth(buildApiUrl(`/api/chat/sessions/${sessionId}`), {
        method: 'DELETE',
        headers: getHeaders()
      });

      fetchSessions();
      if (currentSessionId === sessionId) startNewChat();
    } catch (error) {
      console.error('?紐꾨???????쎈솭:', error);
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userText = inputMessage.trim();
    setMessages((prev) => [...prev, { sender: 'user', text: userText }]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const response = await fetchWithAuth(buildApiUrl('/api/agent/chat'), {
        method: 'POST',
        headers: getHeaders(true),
        body: JSON.stringify({
          message: userText,
          sessionId: currentSessionId
        })
      });

      if (!response.ok) {
        let errorMessage = '筌?쑵???遺욧퍕????쎈솭??됰뮸??덈뼄.';
        try {
          const errorData = await response.json();
          errorMessage = errorData?.message || errorData?.error || errorMessage;
        } catch {
          // ignore parse error
        }
        if (response.status === 429) {
          showQuotaToast(errorMessage);
        }
        throw new Error(errorMessage);
      }

      const data = await response.json();
      const payload = data?.data ?? data;
      const sessionId = payload?.sessionId;
      const botMessage = payload?.message || '?臾먮뼗????밴쉐??? 筌륁궢六??щ빍??';
      const intent = payload?.intent;

      if (!currentSessionId && sessionId) {
        setCurrentSessionId(sessionId);
        fetchSessions();
      }

      setMessages((prev) => [...prev, { sender: 'bot', text: botMessage }]);

      if (intent === 'PLAN_CREATION') {
        setTimeout(() => setIsModalOpen(true), 600);
      }

      await fetchQuota();
    } catch (error) {
      console.error('筌롫뗄?놅쭪? ?袁⑸꽊 ??쎈솭:', error);
      setMessages((prev) => [...prev, { sender: 'bot', text: '??뺤쒔 ?怨뚭퍙????쎈솭??됰뮸??덈뼄. ?醫롫뻻 ????쇰뻻 ??뺣즲??雅뚯눘苑??' }]);
      await fetchQuota();
    } finally {
      setIsLoading(false);
    }
  };

  const submitPlan = async () => {
    if (!formData.departure || !formData.dest || !formData.startDate || !formData.endDate) {
      alert('?곗뮆而삼쭪?, 筌뤴뫗?삼쭪?, ??六??醫롮????袁⑸땾??낅빍??');
      return;
    }

    const finalRequest = `?곗뮆而삼쭪?: ${formData.departure}, 筌뤴뫗?삼쭪?: ${formData.dest}, ??덈꺖: ${
      formData.accommodation || '沃섎챷??AI揶쎛 ??덇퐨??筌띿쉸??1???곕뗄荑?'
    }, ?醫롮?: ${formData.startDate} ~ ${formData.endDate}, ??됰뻬: ${formData.companion}, ????? ${formData.style}`;

    setIsModalOpen(false);
    setMessages((prev) => [...prev, { sender: 'user', text: '[??六??類ｋ궖 ??낆젾 ?袁⑥┷]' }]);
    setIsLoading(true);

    try {
      const response = await fetchWithAuth(buildApiUrl('/api/agent/plan'), {
        method: 'POST',
        headers: getHeaders(true),
        body: JSON.stringify({
          request: finalRequest,
          sessionId: currentSessionId
        })
      });

      if (!response.ok) {
        let errorMessage = '??깆젟 ??밴쉐????쎈솭??됰뮸??덈뼄.';
        try {
          const errorData = await response.json();
          errorMessage = errorData?.message || errorData?.error || errorMessage;
        } catch {
          // ignore parse error
        }
        if (response.status === 429) {
          showQuotaToast(errorMessage);
        }
        throw new Error(errorMessage);
      }

      const data = await response.json();

      if (data.success && data.data) {
        setMessages((prev) => [
          ...prev,
          { sender: 'bot', text: '?ル뿭釉?? ?袁⑥삋????六??λ뜆釉??筌띾슢諭??됰선?? ?類ㅼ뵥????紐꾩뒄.' },
          { sender: 'bot', isPlanCard: true, planData: data.data }
        ]);
        fetchSessions();
      } else {
        setMessages((prev) => [...prev, { sender: 'bot', text: '??깆젟 ??밴쉐????쎈솭??됰뮸??덈뼄. ??쇰뻻 ??뺣즲??雅뚯눘苑??' }]);
      }
    } catch (error) {
      console.error('??깆젟 ??밴쉐 ??쎈솭:', error);
      setMessages((prev) => [...prev, { sender: 'bot', text: error?.message || '??깆젟 ??밴쉐 餓???뺤쒔 ??살첒揶쎛 獄쏆뮇源??됰뮸??덈뼄.' }]);
    } finally {
      setIsLoading(false);
      await fetchQuota();
    }
  };

  const quotaText = isQuotaLoading && !quota
    ? '??? ??쏅땾 ?類ㅼ뵥 餓?..'
    : quota
      ? `??깆젟 ${quota.planPerDayRemaining}/${quota.planPerDayLimit} 夷?筌?쑵??${quota.chatPerDayRemaining}/${quota.chatPerDayLimit}`
      : '??? ??쏅땾 -';

  return (
    <div className="relative flex h-[calc(100vh-76px)] overflow-hidden bg-[#eef7ff]">
      {toast.visible && (
        <div className="pointer-events-none absolute right-4 top-4 z-[100] max-w-[360px] animate-in fade-in slide-in-from-top-2 rounded-xl border border-[#f4c9c9] bg-[#fff4f4] px-4 py-3 text-sm font-semibold text-[#a83f3f] shadow-lg">
          {toast.message}
        </div>
      )}

      {isMobile && isSidebarOpen && (
        <button
          type="button"
          aria-label="?????뺤뺍 ??る┛"
          onClick={() => setIsSidebarOpen(false)}
          className="absolute inset-0 z-10 bg-black/35"
        />
      )}

      <div
        className={[
          'flex flex-col overflow-hidden border-r border-[#d6e9fa] bg-[#f9fdff] transition-all duration-300',
          isMobile
            ? `absolute left-0 top-0 z-20 h-full w-[86%] max-w-[340px] shrink-0 transform ${
                isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
              }`
            : `${isSidebarOpen ? 'w-72' : 'w-0'} relative h-full shrink-0`
        ].join(' ')}
      >
        <div className="p-4">
          <button
            onClick={startNewChat}
            className="flex w-full items-center gap-2 rounded-2xl border border-[#d2e8fb] bg-white p-3 font-bold text-[#305676] shadow-sm transition hover:border-[#1f8ef1] hover:text-[#1f8ef1]"
          >
            <Plus size={18} /> ????六??④쑵???띾┛
          </button>
        </div>

        <div className="flex-1 space-y-2 overflow-y-auto p-4 pt-0">
          <p className="mb-3 ml-2 text-xs font-bold uppercase tracking-[0.12em] text-[#4d7fa8]">최근 대화</p>

          {sessions.length > 0 ? (
            sessions.map((session) => (
              <div
                key={session.id}
                onClick={() => loadSession(session.id)}
                className={`group flex cursor-pointer items-center justify-between rounded-xl p-3 transition ${
                  currentSessionId === session.id
                    ? 'border border-[#b8dbf8] bg-[#eaf6ff] text-[#1f8ef1]'
                    : 'text-[#4b6177] hover:bg-[#f2f9ff]'
                }`}
              >
                <div className="flex items-center gap-3 overflow-hidden">
                  <MessageSquare
                    size={16}
                    className={currentSessionId === session.id ? 'text-[#1f8ef1]' : 'text-[#8aa1b7]'}
                  />
                  <span className="truncate text-sm font-semibold">{session.sessionTitle}</span>
                </div>
                <button
                  onClick={(event) => deleteSession(event, session.id)}
                  className="p-1 opacity-0 transition group-hover:opacity-100 hover:text-red-500"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))
          ) : (
            <p className="mt-10 text-center text-sm text-[#8aa1b7]">???貫留????遺? ??곷뮸??덈뼄.</p>
          )}
        </div>
      </div>

      <div className="relative flex flex-1 flex-col bg-transparent">
        <button
          onClick={() => setIsSidebarOpen(!isSidebarOpen)}
          className="absolute left-4 top-4 z-10 rounded-lg border border-[#d2e8fb] bg-white p-2 text-[#4b6177] shadow-sm transition hover:text-[#1f8ef1] lg:hidden"
        >
          {isSidebarOpen ? <PanelLeftClose size={20} /> : <PanelLeftOpen size={20} />}
        </button>

        <div className="flex-1 overflow-y-auto p-4 pt-16 md:p-8 md:pt-8">
          <div className="mx-auto max-w-3xl space-y-8">
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`flex gap-4 animate-in fade-in slide-in-from-bottom-2 ${
                  msg.sender === 'user' ? 'justify-end' : 'justify-start'
                }`}
              >
                {msg.sender === 'bot' && (
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#dff1ff] text-[#1f8ef1] shadow-sm">
                    <Bot size={20} />
                  </div>
                )}

                <div
                  className={`max-w-[92%] rounded-2xl p-4 sm:max-w-[85%] sm:p-5 ${
                    msg.sender === 'user'
                      ? 'rounded-tr-sm bg-[#1f8ef1] text-white shadow-md'
                      : 'rounded-tl-sm border border-[#d6e9fa] bg-[#f9fdff] text-[#1f3550]'
                  }`}
                >
                  {!msg.isPlanCard && <p className="whitespace-pre-wrap text-[15px] leading-relaxed">{msg.text}</p>}

                  {msg.isPlanCard && msg.planData && (
                    <div className="space-y-4">
                      <div className="mb-4 border-b border-[#d6e9fa] pb-3">
                        <h3 className="text-xl font-bold text-[#1f8ef1]">{msg.planData.title}</h3>
                        <p className="mt-1 text-sm text-[#4b6177]">
                          {msg.planData.region} | {msg.planData.startDate} ~ {msg.planData.endDate}
                        </p>
                        {msg.planData.accommodation && (
                          <p className="mt-1 flex items-center gap-1 text-sm font-semibold text-[#2c6ea0]">
                            <BedDouble size={14} /> {msg.planData.accommodation}
                          </p>
                        )}
                      </div>

                      {msg.planData.itineraries?.map((day, dayIndex) => {
                        const dayValue = day.dayNumber ?? day.day ?? dayIndex + 1;
                        return (
                        <div key={day.id ?? dayValue} className="rounded-xl border border-[#cfe4f7] bg-white p-4 shadow-sm">
                          <h4 className="mb-2 flex items-center gap-2 font-bold text-[#1f3550]">
                            <span className="rounded bg-[#eaf6ff] px-2 py-0.5 text-sm text-[#1f8ef1]">Day {dayValue}</span>
                            {day.theme}
                          </h4>
                          <ul className="mt-3 space-y-3">
                            {day.places?.map((place, placeIndex) => (
                              <li key={placeIndex} className="flex gap-3 text-sm">
                                <div className="mt-1 flex flex-col items-center">
                                  <div className="h-2 w-2 rounded-full bg-[#1f8ef1]" />
                                  {placeIndex !== day.places.length - 1 && (
                                    <div className="my-1 h-full w-0.5 bg-[#b8dbf8]" />
                                  )}
                                </div>
                                <div>
                                  <p className="font-semibold text-[#1f3550]">{place.placeName || place.name}</p>
                                  <p className="mt-0.5 text-xs text-[#4b6177]">{place.memo}</p>
                                </div>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )})}
                    </div>
                  )}
                </div>
              </div>
            ))}

            {isLoading && (
              <div className="flex animate-pulse justify-start gap-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#dff1ff] text-[#1f8ef1]">
                  <Loader2 className="animate-spin" size={20} />
                </div>
                <div className="rounded-2xl rounded-tl-sm border border-[#d6e9fa] bg-[#f9fdff] p-4 font-medium text-[#4b6177]">
                  AI揶쎛 ???????밴쉐??롫뮉 餓λ쵐???덈뼄...
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        </div>

        <div className="border-t border-[#d6e9fa] bg-[#f9fdff] p-4">
          <div className="relative mx-auto flex max-w-3xl gap-2 rounded-full border border-[#d2e8fb] bg-white shadow-sm transition-all focus-within:border-[#1f8ef1] focus-within:ring-2 focus-within:ring-[#dff1ff]">
            <div className="ml-3 flex shrink-0 items-center">
              <span className="rounded-full border border-[#d9ebfb] bg-[#f3f9ff] px-3 py-1 text-[11px] font-bold text-[#3d6f95]">
                {quotaText}
              </span>
            </div>
            <input
              type="text"
              value={inputMessage}
              onChange={(event) => setInputMessage(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && sendMessage()}
              placeholder="예: 제주도 2박 3일, 맛집 중심 일정 짜줘"
              className="flex-1 rounded-full border-none bg-transparent px-6 py-4 text-[15px] focus:outline-none"
            />
            <button
              onClick={sendMessage}
              disabled={isLoading}
              className="absolute bottom-2 right-2 top-2 flex items-center justify-center rounded-full bg-[#1f8ef1] px-5 text-white shadow-sm transition hover:bg-[#1878cc] disabled:bg-gray-300"
            >
              <Send size={18} />
            </button>
          </div>
          <p className="mt-3 text-center text-xs text-[#8aa1b7]">
            ??밴쉐????깆젟?? 筌〓㈇???뱀뵠筌? ??쇱젫 ??됰튋/?怨몃씜 ?類ｋ궖??癰귢쑬猷꾣에??類ㅼ뵥??雅뚯눘苑??
          </p>
        </div>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <div className="max-h-[92vh] w-full max-w-lg overflow-y-auto rounded-[2rem] border border-[#d3e8fb] bg-white shadow-2xl duration-200 animate-in fade-in zoom-in">
            <div className="relative overflow-hidden bg-gradient-to-r from-[#1f8ef1] to-[#0ea5e9] p-6 text-center text-white sm:p-8">
              <h2 className="relative z-10 text-3xl font-black">??六??類ｋ궖 ??낆젾</h2>
              <p className="relative z-10 mt-2 text-sm font-medium text-white/85">???類μ넇????깆젟 ??밴쉐???袁る퉸 ?類ｋ궖????낆젾??雅뚯눘苑??</p>
            </div>

            <div className="space-y-4 bg-[#eef7ff] p-5 sm:p-8">
              <div className="flex flex-col gap-4 sm:flex-row">
                <div className="flex-1 rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                  <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                    <PlaneTakeoff size={14} /> ?곗뮆而삼쭪?
                  </label>
                  <input
                    type="text"
                    className="w-full bg-transparent p-2 font-semibold text-[#1f3550] outline-none"
                    placeholder="예: 부산"
                    value={formData.departure}
                    onChange={(event) => setFormData({ ...formData, departure: event.target.value })}
                  />
                </div>
                <div className="flex-1 rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                  <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                    <PlaneLanding size={14} /> 筌뤴뫗?삼쭪?
                  </label>
                  <input
                    type="text"
                    className="w-full bg-transparent p-2 font-semibold text-[#1f3550] outline-none"
                    placeholder="예: 제주도"
                    value={formData.dest}
                    onChange={(event) => setFormData({ ...formData, dest: event.target.value })}
                  />
                </div>
              </div>

              <div className="w-full rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                  <BedDouble size={14} /> ??덈꺖(?醫뤾문)
                </label>
                <input
                  type="text"
                  className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f3550] outline-none"
                  placeholder="예: 서귀포 시내 호텔"
                  value={formData.accommodation}
                  onChange={(event) => setFormData({ ...formData, accommodation: event.target.value })}
                />
              </div>

              <div className="flex flex-col gap-4 sm:flex-row">
                <div className="flex-1 rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                  <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                    <Calendar size={14} /> ??뽰삂??                  </label>
                  <input
                    type="date"
                    className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f3550] outline-none"
                    value={formData.startDate}
                    onChange={(event) => setFormData({ ...formData, startDate: event.target.value })}
                  />
                </div>
                <div className="flex-1 rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                  <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                    <Calendar size={14} /> ?ル굝利??                  </label>
                  <input
                    type="date"
                    className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f3550] outline-none"
                    value={formData.endDate}
                    onChange={(event) => setFormData({ ...formData, endDate: event.target.value })}
                  />
                </div>
              </div>

              <div className="flex flex-col gap-4 sm:flex-row">
                <div className="flex-1 rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                  <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                    <Users size={14} /> ??됰뻬
                  </label>
                  <select
                    className="w-full cursor-pointer bg-transparent p-2 text-sm font-semibold text-[#1f3550] outline-none"
                    value={formData.companion}
                    onChange={(event) => setFormData({ ...formData, companion: event.target.value })}
                  >
                    <option>혼자</option>
                    <option>친구와</option>
                    <option>연인과</option>
                    <option>가족과</option>
                  </select>
                </div>
                <div className="flex-[2] rounded-2xl border border-[#d2e8fb] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#dff1ff]">
                  <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#4b6177]">
                    <Star size={14} /> ??六??????                  </label>
                  <input
                    type="text"
                    className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f3550] outline-none"
                    placeholder="예: 맛집, 카페, 자연, 사진"
                    value={formData.style}
                    onChange={(event) => setFormData({ ...formData, style: event.target.value })}
                  />
                </div>
              </div>

              <div className="flex flex-col gap-3 pt-2 sm:flex-row">
                <button
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 rounded-2xl bg-[#dfefff] py-4 font-bold text-[#4b6177] transition hover:bg-[#d0e7ff]"
                >
                  ?띯뫁??                </button>
                <button
                  onClick={submitPlan}
                  className="flex-[2] rounded-2xl bg-[#1f8ef1] py-4 font-black text-white shadow-lg transition hover:-translate-y-0.5 hover:bg-[#1878cc] hover:shadow-xl"
                >
                  AI ??깆젟 ??밴쉐??띾┛
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
