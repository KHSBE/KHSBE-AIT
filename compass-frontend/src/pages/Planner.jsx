import { useState, useRef, useEffect } from 'react';
import { Send, Bot, Calendar, Users, Star, Loader2, PlaneTakeoff, PlaneLanding, BedDouble, MessageSquare, Plus, Trash2, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
// ★ 토큰 자동 갱신 마법 함수 가져오기!
import { fetchWithAuth, buildApiUrl } from '../utils/api';

export default function Planner() {
    const [sessions, setSessions] = useState([]);
    const [currentSessionId, setCurrentSessionId] = useState(null);
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [isMobile, setIsMobile] = useState(false);

    const [messages, setMessages] = useState([{ sender: 'bot', text: '안녕하세요! 어디로 여행을 떠나고 싶으신가요? ✈️' }]);
    const [inputMessage, setInputMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({
        departure: '', dest: '', accommodation: '', startDate: '', endDate: '', companion: '혼자', style: ''
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

    const getHeaders = (isJson = false) => {
        const headers = {};
        if (isJson) headers['Content-Type'] = 'application/json';
        // fetchWithAuth 내부에서 Authorization 헤더를 덮어씌워주지만, 명시적으로 둬도 무방합니다.
        const storedToken = localStorage.getItem('accessToken');
        if (storedToken) headers['Authorization'] = `Bearer ${storedToken}`;
        return headers;
    };

    const fetchSessions = async () => {
        try {
            // ★ fetch -> fetchWithAuth 로 변경
            const response = await fetchWithAuth(buildApiUrl('/api/chat/sessions'), { headers: getHeaders() });
            const data = await response.json();

            if (data.success) {
                setSessions(data.data || []);
            }
        } catch (error) {
            console.error('세션 목록 불러오기 실패:', error);
        }
    };

    useEffect(() => {
        fetchSessions();
    }, []);

    const loadSession = async (sessionId) => {
        setCurrentSessionId(sessionId);
        setMessages([]);
        if (isMobile) setIsSidebarOpen(false);
        setIsLoading(true);
        try {
            // ★ fetch -> fetchWithAuth 로 변경
            const response = await fetchWithAuth(buildApiUrl(`/api/chat/sessions/${sessionId}/messages`), { headers: getHeaders() });
            const data = await response.json();

            if (data.success) {
                const history = (data.data || []).map(msg => ({
                    sender: msg.senderRole === 'USER' ? 'user' : 'bot',
                    text: msg.content
                }));
                setMessages(history.length > 0 ? history : [{ sender: 'bot', text: '안녕하세요! 어디로 여행을 떠나고 싶으신가요? ✈️' }]);
            }
        } catch (error) {
            console.error('메시지 불러오기 실패:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const startNewChat = () => {
        setCurrentSessionId(null);
        setMessages([{ sender: 'bot', text: '새로운 여행 계획을 세워볼까요? 어디로 가고 싶으신가요? ✈️' }]);
        if (isMobile) setIsSidebarOpen(false);
    };

    const deleteSession = async (e, sessionId) => {
        e.stopPropagation();
        if (!window.confirm('이 대화 기록을 삭제하시겠습니까?')) return;

        try {
            // ★ fetch -> fetchWithAuth 로 변경
            await fetchWithAuth(buildApiUrl(`/api/chat/sessions/${sessionId}`), { method: 'DELETE', headers: getHeaders() });
            fetchSessions();
            if (currentSessionId === sessionId) startNewChat();
        } catch (error) {
            console.error('세션 삭제 실패:', error);
        }
    };

    const sendMessage = async () => {
        if (!inputMessage.trim()) return;
        const userMsg = inputMessage;
        setMessages(prev => [...prev, { sender: 'user', text: userMsg }]);
        setInputMessage('');
        setIsLoading(true);

        try {
            const url = buildApiUrl(`/api/agent/chat?message=${encodeURIComponent(userMsg)}${currentSessionId ? `&sessionId=${currentSessionId}` : ''}`);
            // ★ fetch -> fetchWithAuth 로 변경
            const response = await fetchWithAuth(url, { method: 'GET', headers: getHeaders() });
            const data = await response.json();

            if (!currentSessionId && data.sessionId) {
                setCurrentSessionId(data.sessionId);
                fetchSessions();
            }

            setMessages(prev => [...prev, { sender: 'bot', text: data.message }]);

            if (data.intent === 'PLAN_CREATION') {
                setTimeout(() => setIsModalOpen(true), 1000);
            }
        } catch (error) {
            setMessages(prev => [...prev, { sender: 'bot', text: '서버 연결에 실패했습니다.' }]);
        } finally {
            setIsLoading(false);
        }
    };

    const submitPlan = async () => {
        if (!formData.departure || !formData.dest || !formData.startDate || !formData.endDate) {
            alert('출발지, 도착지, 가는 날, 오는 날은 필수입니다!');
            return;
        }

        const finalRequest = `출발지: ${formData.departure}, 목적지: ${formData.dest}, 숙소: ${formData.accommodation || '미정(AI가 동선에 맞춰 최적의 숙소 1곳을 추천해줘)'}, 날짜: ${formData.startDate} ~ ${formData.endDate}, 일행: ${formData.companion}, 스타일: ${formData.style}`;

        setIsModalOpen(false);
        setMessages(prev => [...prev, { sender: 'user', text: '[일정 생성 정보 전달 완료]' }]);
        setIsLoading(true);

        try {
            // ★ fetch -> fetchWithAuth 로 변경
            const response = await fetchWithAuth(buildApiUrl('/api/agent/plan'), {
                method: 'POST',
                headers: getHeaders(true),
                body: JSON.stringify({ request: finalRequest })
            });
            const data = await response.json();

            if (data.success) {
                setMessages(prev => [...prev,
                    { sender: 'bot', text: `🎉 완벽한 일정을 준비했습니다! 확인해 보세요.` },
                    { sender: 'bot', isPlanCard: true, planData: data.data }
                ]);
                fetchSessions();
            } else {
                setMessages(prev => [...prev, { sender: 'bot', text: '일정 생성에 실패했습니다.' }]);
            }
        } catch (error) {
            setMessages(prev => [...prev, { sender: 'bot', text: '일정 생성 중 서버 에러가 발생했습니다.' }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="relative flex h-[calc(100vh-76px)] overflow-hidden bg-[#f7f1e6]">
            {isMobile && isSidebarOpen && (
                <button
                    type="button"
                    aria-label="사이드바 닫기"
                    onClick={() => setIsSidebarOpen(false)}
                    className="absolute inset-0 z-10 bg-black/35"
                />
            )}

            {/* ⬅️ 좌측 사이드바 (채팅 세션 목록) */}
            <div
                className={[
                    'flex flex-col overflow-hidden border-r border-[#ddd2bf] bg-[#fbf6ee] transition-all duration-300',
                    isMobile
                        ? `absolute left-0 top-0 z-20 h-full w-[86%] max-w-[340px] shrink-0 transform ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full'}`
                        : `${isSidebarOpen ? 'w-72' : 'w-0'} relative h-full shrink-0`,
                ].join(' ')}
            >
                <div className="p-4">
                    <button onClick={startNewChat} className="flex w-full items-center gap-2 rounded-2xl border border-[#d8ccbb] bg-[#fffdf8] p-3 font-bold text-[#3f4755] shadow-sm transition hover:border-[#0f7a80] hover:text-[#0f7a80]">
                        <Plus size={18} /> 새 여행 계획하기
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-4 pt-0 space-y-2">
                    <p className="mb-3 ml-2 text-xs font-bold uppercase tracking-[0.12em] text-[#8a7c65]">최근 대화</p>
                    {sessions && sessions.length > 0 ? (
                        sessions.map(session => (
                            <div
                                key={session.id}
                                onClick={() => loadSession(session.id)}
                                className={`group flex cursor-pointer items-center justify-between rounded-xl p-3 transition ${currentSessionId === session.id ? 'border border-[#a8d5d1] bg-[#def3f1] text-[#0f7a80]' : 'text-[#5e6673] hover:bg-[#f1e9db]'}`}
                            >
                                <div className="flex items-center gap-3 overflow-hidden">
                                    <MessageSquare size={16} className={currentSessionId === session.id ? 'text-[#0f7a80]' : 'text-[#9aa3b2]'} />
                                    <span className="text-sm font-semibold truncate">{session.sessionTitle}</span>
                                </div>
                                <button onClick={(e) => deleteSession(e, session.id)} className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition">
                                    <Trash2 size={14} />
                                </button>
                            </div>
                        ))
                    ) : (
                        <p className="mt-10 text-center text-sm text-[#8d95a3]">대화 기록이 없습니다.</p>
                    )}
                </div>
            </div>

            {/* ➡️ 우측 메인 채팅 영역 */}
            <div className="relative flex flex-1 flex-col bg-transparent">

                {/* 사이드바 토글 버튼 */}
                <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="absolute left-4 top-4 z-10 rounded-lg border border-[#d8ccba] bg-[#fffdf8] p-2 text-[#6a7280] shadow-sm transition hover:text-[#1f2430] lg:hidden">
                    {isSidebarOpen ? <PanelLeftClose size={20} /> : <PanelLeftOpen size={20} />}
                </button>

                <div className="flex-1 overflow-y-auto p-4 pt-16 md:p-8 md:pt-8">
                    <div className="max-w-3xl mx-auto space-y-8">
                        {messages.map((msg, idx) => (
                            <div key={idx} className={`flex gap-4 ${msg.sender === 'user' ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2`}>
                                {msg.sender === 'bot' && <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#d6f0ee] text-[#0f7a80] shadow-sm"><Bot size={20} /></div>}

                                <div className={`max-w-[92%] rounded-2xl p-4 sm:max-w-[85%] sm:p-5 ${msg.sender === 'user' ? 'rounded-tr-sm bg-[#1e2430] text-white shadow-md' : 'rounded-tl-sm border border-[#ddd2bf] bg-[#fffdf8] text-[#1f2430]'}`}>

                                    {!msg.isPlanCard && <p className="leading-relaxed whitespace-pre-wrap text-[15px]">{msg.text}</p>}

                                    {/* 일정표 미리보기 카드 UI */}
                                    {msg.isPlanCard && msg.planData && (
                                        <div className="space-y-4">
                                            <div className="mb-4 border-b border-[#ddd2bf] pb-3">
                                                <h3 className="text-xl font-bold text-[#0f7a80]">{msg.planData.title}</h3>
                                                <p className="mt-1 text-sm text-[#5e6673]">{msg.planData.region} | {msg.planData.startDate} ~ {msg.planData.endDate}</p>
                                                {msg.planData.accommodation && <p className="mt-1 flex items-center gap-1 text-sm font-semibold text-[#b8512d]"><BedDouble size={14}/> {msg.planData.accommodation}</p>}
                                            </div>
                                            {msg.planData.itineraries?.map(day => (
                                                <div key={day.dayNumber} className="rounded-xl border border-[#c8e5e2] bg-[#f5fffe] p-4 shadow-sm">
                                                    <h4 className="mb-2 flex items-center gap-2 font-bold text-[#1f2430]">
                                                        <span className="rounded bg-[#d6f0ee] px-2 py-0.5 text-sm text-[#0f7a80]">Day {day.dayNumber}</span>
                                                        {day.theme}
                                                    </h4>
                                                    <ul className="space-y-3 mt-3">
                                                        {day.places?.map((place, pIdx) => (
                                                            <li key={pIdx} className="flex gap-3 text-sm">
                                                                <div className="flex flex-col items-center mt-1">
                                                                    <div className="h-2 w-2 rounded-full bg-[#0f7a80]" />
                                                                    {pIdx !== day.places.length - 1 && <div className="my-1 h-full w-0.5 bg-[#bce0dd]" />}
                                                                </div>
                                                                <div>
                                                                    <p className="font-semibold text-[#1f2430]">{place.placeName || place.name}</p>
                                                                    <p className="mt-0.5 text-xs text-[#5e6673]">{place.memo}</p>
                                                                </div>
                                                            </li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                        {isLoading && (
                            <div className="flex justify-start gap-4 animate-pulse">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#ece5d8] text-[#8d95a3]"><Loader2 className="animate-spin" size={20} /></div>
                                <div className="rounded-2xl rounded-tl-sm border border-[#ddd2bf] bg-[#fffdf8] p-4 font-medium text-[#5e6673]">에이전트가 고민 중입니다...</div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>
                </div>

                {/* 하단 입력창 고정 */}
                <div className="border-t border-[#ddd2bf] bg-[#fffdf8] p-4">
                    <div className="relative mx-auto flex max-w-3xl gap-2 rounded-full border border-[#d8ccba] bg-white shadow-sm transition-all focus-within:border-[#0f7a80] focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                        <input
                            type="text"
                            value={inputMessage} onChange={e => setInputMessage(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && sendMessage()}
                            placeholder="제주도 2박 3일 맛집 투어 일정 짜줘!"
                            className="flex-1 bg-transparent border-none rounded-full px-6 py-4 focus:outline-none text-[15px]"
                        />
                        <button onClick={sendMessage} disabled={isLoading} className="absolute bottom-2 right-2 top-2 flex items-center justify-center rounded-full bg-[#ff6a3d] px-5 text-white shadow-sm transition hover:bg-[#e85a30] disabled:bg-gray-300">
                            <Send size={18} />
                        </button>
                    </div>
                    <p className="mt-3 text-center text-xs text-[#8d95a3]">AI가 생성한 일정은 참고용이며, 실제 예약 및 휴무일은 별도 확인이 필요합니다.</p>
                </div>
            </div>

            {/* 모달 팝업 영역 */}
            {isModalOpen && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                    <div className="max-h-[92vh] w-full max-w-lg overflow-y-auto rounded-[2rem] border border-[#d9cfbf] bg-white shadow-2xl duration-200 animate-in fade-in zoom-in">
                        <div className="relative overflow-hidden bg-gradient-to-r from-[#0f7a80] to-[#ff6a3d] p-6 text-center text-white sm:p-8">
                            <h2 className="text-3xl font-black relative z-10">여행 정보 입력</h2>
                            <p className="relative z-10 mt-2 text-sm font-medium text-white/80">완벽한 동선을 위해 추가 정보를 알려주세요.</p>
                        </div>

                        <div className="space-y-4 bg-[#f7f1e6] p-5 sm:p-8">
                            <div className="flex flex-col gap-4 sm:flex-row">
                                <div className="flex-1 rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                    <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><PlaneTakeoff size={14}/> 출발지</label>
                                    <input type="text" className="w-full bg-transparent p-2 font-semibold text-[#1f2430] outline-none" placeholder="예: 부산" value={formData.departure} onChange={e => setFormData({...formData, departure: e.target.value})} />
                                </div>
                                <div className="flex-1 rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                    <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><PlaneLanding size={14}/> 도착지</label>
                                    <input type="text" className="w-full bg-transparent p-2 font-semibold text-[#1f2430] outline-none" placeholder="예: 오사카" value={formData.dest} onChange={e => setFormData({...formData, dest: e.target.value})} />
                                </div>
                            </div>

                            <div className="w-full rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><BedDouble size={14}/> 예약한 숙소 (선택)</label>
                                <input type="text" className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f2430] outline-none" placeholder="예: 신라호텔 (비워두면 AI가 추천!)" value={formData.accommodation} onChange={e => setFormData({...formData, accommodation: e.target.value})} />
                            </div>

                            <div className="flex flex-col gap-4 sm:flex-row">
                                <div className="flex-1 rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                    <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><Calendar size={14}/> 가는 날</label>
                                    <input type="date" className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f2430] outline-none" value={formData.startDate} onChange={e => setFormData({...formData, startDate: e.target.value})} />
                                </div>
                                <div className="flex-1 rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                    <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><Calendar size={14}/> 오는 날</label>
                                    <input type="date" className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f2430] outline-none" value={formData.endDate} onChange={e => setFormData({...formData, endDate: e.target.value})} />
                                </div>
                            </div>

                            <div className="flex flex-col gap-4 sm:flex-row">
                                <div className="flex-1 rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                    <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><Users size={14}/> 누구와</label>
                                    <select className="w-full cursor-pointer bg-transparent p-2 text-sm font-semibold text-[#1f2430] outline-none" value={formData.companion} onChange={e => setFormData({...formData, companion: e.target.value})}>
                                        <option>혼자</option><option>친구와</option><option>연인과</option><option>가족과</option>
                                    </select>
                                </div>
                                <div className="flex-[2] rounded-2xl border border-[#ddd2bf] bg-white p-2 shadow-sm transition focus-within:ring-2 focus-within:ring-[#d6f0ee]">
                                    <label className="ml-2 mt-1 flex items-center gap-1.5 text-xs font-bold text-[#6e7685]"><Star size={14}/> 여행 테마</label>
                                    <input type="text" className="w-full bg-transparent p-2 text-sm font-semibold text-[#1f2430] outline-none" placeholder="예: 맛집 탐방, 힐링" value={formData.style} onChange={e => setFormData({...formData, style: e.target.value})} />
                                </div>
                            </div>

                            <div className="flex flex-col gap-3 pt-2 sm:flex-row">
                                <button onClick={() => setIsModalOpen(false)} className="flex-1 rounded-2xl bg-[#e7dfd1] py-4 font-bold text-[#5d6573] transition hover:bg-[#ddd3c1]">취소</button>
                                <button onClick={submitPlan} className="flex-[2] rounded-2xl bg-[#0f7a80] py-4 font-black text-white shadow-lg transition hover:-translate-y-0.5 hover:bg-[#0c686d] hover:shadow-xl">
                                    AI 일정 생성하기
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
