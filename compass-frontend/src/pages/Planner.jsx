import { useState, useRef, useEffect } from 'react';
import { Send, Bot, Calendar, Users, Star, Loader2, PlaneTakeoff, PlaneLanding, BedDouble, MessageSquare, Plus, Trash2, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
// ★ 토큰 자동 갱신 마법 함수 가져오기!
import { fetchWithAuth } from '../utils/api';

export default function Planner() {
    const [sessions, setSessions] = useState([]);
    const [currentSessionId, setCurrentSessionId] = useState(null);
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    const [messages, setMessages] = useState([{ sender: 'bot', text: '안녕하세요! 어디로 여행을 떠나고 싶으신가요? ✈️' }]);
    const [inputMessage, setInputMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({
        departure: '', dest: '', accommodation: '', startDate: '', endDate: '', companion: '혼자', style: ''
    });

    const messagesEndRef = useRef(null);
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
            const response = await fetchWithAuth('http://localhost:8080/api/chat/sessions', { headers: getHeaders() });
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
        setIsLoading(true);
        try {
            // ★ fetch -> fetchWithAuth 로 변경
            const response = await fetchWithAuth(`http://localhost:8080/api/chat/sessions/${sessionId}/messages`, { headers: getHeaders() });
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
    };

    const deleteSession = async (e, sessionId) => {
        e.stopPropagation();
        if (!window.confirm('이 대화 기록을 삭제하시겠습니까?')) return;

        try {
            // ★ fetch -> fetchWithAuth 로 변경
            await fetchWithAuth(`http://localhost:8080/api/chat/sessions/${sessionId}`, { method: 'DELETE', headers: getHeaders() });
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
            const url = `http://localhost:8080/api/agent/chat?message=${encodeURIComponent(userMsg)}${currentSessionId ? `&sessionId=${currentSessionId}` : ''}`;
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
            const response = await fetchWithAuth('http://localhost:8080/api/agent/plan', {
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
        <div className="flex h-[calc(100vh-80px)] bg-white overflow-hidden">

            {/* ⬅️ 좌측 사이드바 (채팅 세션 목록) */}
            <div className={`${isSidebarOpen ? 'w-72' : 'w-0'} bg-gray-50 border-r border-gray-200 transition-all duration-300 flex flex-col overflow-hidden shrink-0`}>
                <div className="p-4">
                    <button onClick={startNewChat} className="w-full flex items-center gap-2 bg-white border border-gray-200 shadow-sm hover:border-blue-400 hover:text-blue-600 transition p-3 rounded-xl font-bold text-gray-700">
                        <Plus size={18} /> 새 여행 계획하기
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-4 pt-0 space-y-2">
                    <p className="text-xs font-bold text-gray-400 mb-3 ml-2">최근 대화</p>
                    {sessions && sessions.length > 0 ? (
                        sessions.map(session => (
                            <div
                                key={session.id}
                                onClick={() => loadSession(session.id)}
                                className={`group flex items-center justify-between p-3 rounded-xl cursor-pointer transition ${currentSessionId === session.id ? 'bg-blue-50 border border-blue-100 text-blue-700' : 'hover:bg-gray-100 text-gray-600'}`}
                            >
                                <div className="flex items-center gap-3 overflow-hidden">
                                    <MessageSquare size={16} className={currentSessionId === session.id ? 'text-blue-500' : 'text-gray-400'} />
                                    <span className="text-sm font-semibold truncate">{session.sessionTitle}</span>
                                </div>
                                <button onClick={(e) => deleteSession(e, session.id)} className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition">
                                    <Trash2 size={14} />
                                </button>
                            </div>
                        ))
                    ) : (
                        <p className="text-center text-sm text-gray-400 mt-10">대화 기록이 없습니다.</p>
                    )}
                </div>
            </div>

            {/* ➡️ 우측 메인 채팅 영역 */}
            <div className="flex-1 flex flex-col relative bg-white">

                {/* 사이드바 토글 버튼 */}
                <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="absolute top-4 left-4 z-10 p-2 bg-white border border-gray-200 rounded-lg shadow-sm text-gray-500 hover:text-gray-800 transition">
                    {isSidebarOpen ? <PanelLeftClose size={20} /> : <PanelLeftOpen size={20} />}
                </button>

                <div className="flex-1 overflow-y-auto p-4 md:p-8 pt-16">
                    <div className="max-w-3xl mx-auto space-y-8">
                        {messages.map((msg, idx) => (
                            <div key={idx} className={`flex gap-4 ${msg.sender === 'user' ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2`}>
                                {msg.sender === 'bot' && <div className="w-10 h-10 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center shrink-0 shadow-sm"><Bot size={20} /></div>}

                                <div className={`max-w-[85%] rounded-2xl p-5 ${msg.sender === 'user' ? 'bg-gray-800 text-white rounded-tr-sm shadow-md' : 'bg-gray-50 text-gray-800 rounded-tl-sm border border-gray-100'}`}>

                                    {!msg.isPlanCard && <p className="leading-relaxed whitespace-pre-wrap text-[15px]">{msg.text}</p>}

                                    {/* 일정표 미리보기 카드 UI */}
                                    {msg.isPlanCard && msg.planData && (
                                        <div className="space-y-4">
                                            <div className="border-b border-gray-200 pb-3 mb-4">
                                                <h3 className="text-xl font-bold text-blue-700">{msg.planData.title}</h3>
                                                <p className="text-sm text-gray-500 mt-1">{msg.planData.region} | {msg.planData.startDate} ~ {msg.planData.endDate}</p>
                                                {msg.planData.accommodation && <p className="text-sm text-amber-600 mt-1 font-semibold flex items-center gap-1"><BedDouble size={14}/> {msg.planData.accommodation}</p>}
                                            </div>
                                            {msg.planData.itineraries?.map(day => (
                                                <div key={day.dayNumber} className="bg-white p-4 rounded-xl border border-blue-100 shadow-sm">
                                                    <h4 className="font-bold text-gray-800 mb-2 flex items-center gap-2">
                                                        <span className="bg-blue-100 text-blue-700 px-2 py-0.5 rounded text-sm">Day {day.dayNumber}</span>
                                                        {day.theme}
                                                    </h4>
                                                    <ul className="space-y-3 mt-3">
                                                        {day.places?.map((place, pIdx) => (
                                                            <li key={pIdx} className="flex gap-3 text-sm">
                                                                <div className="flex flex-col items-center mt-1">
                                                                    <div className="w-2 h-2 rounded-full bg-blue-400"></div>
                                                                    {pIdx !== day.places.length - 1 && <div className="w-0.5 h-full bg-blue-100 my-1"></div>}
                                                                </div>
                                                                <div>
                                                                    <p className="font-semibold text-gray-800">{place.placeName || place.name}</p>
                                                                    <p className="text-gray-500 text-xs mt-0.5">{place.memo}</p>
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
                                <div className="w-10 h-10 bg-gray-100 text-gray-400 rounded-full flex items-center justify-center"><Loader2 className="animate-spin" size={20} /></div>
                                <div className="bg-gray-50 rounded-2xl p-4 rounded-tl-sm border border-gray-100 text-gray-500 font-medium">에이전트가 고민 중입니다...</div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>
                </div>

                {/* 하단 입력창 고정 */}
                <div className="p-4 bg-white border-t border-gray-100">
                    <div className="max-w-3xl mx-auto flex gap-2 relative shadow-sm rounded-full bg-white border border-gray-200 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 transition-all">
                        <input
                            type="text"
                            value={inputMessage} onChange={e => setInputMessage(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && sendMessage()}
                            placeholder="제주도 2박 3일 맛집 투어 일정 짜줘!"
                            className="flex-1 bg-transparent border-none rounded-full px-6 py-4 focus:outline-none text-[15px]"
                        />
                        <button onClick={sendMessage} disabled={isLoading} className="absolute right-2 top-2 bottom-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white rounded-full px-5 flex items-center justify-center transition shadow-sm">
                            <Send size={18} />
                        </button>
                    </div>
                    <p className="text-center text-xs text-gray-400 mt-3">AI가 생성한 일정은 참고용이며, 실제 예약 및 휴무일은 별도 확인이 필요합니다.</p>
                </div>
            </div>

            {/* 모달 팝업 영역 */}
            {isModalOpen && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-[2rem] w-full max-w-lg overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 border border-gray-100">
                        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-8 text-white text-center relative overflow-hidden">
                            <h2 className="text-3xl font-black relative z-10">여행 정보 입력</h2>
                            <p className="text-blue-100 text-sm mt-2 relative z-10 font-medium">완벽한 동선을 위해 추가 정보를 알려주세요!</p>
                        </div>

                        <div className="p-8 space-y-4 bg-gray-50">
                            <div className="flex gap-4">
                                <div className="flex-1 bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                    <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><PlaneTakeoff size={14}/> 출발지</label>
                                    <input type="text" className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold" placeholder="예: 부산" value={formData.departure} onChange={e => setFormData({...formData, departure: e.target.value})} />
                                </div>
                                <div className="flex-1 bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                    <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><PlaneLanding size={14}/> 도착지</label>
                                    <input type="text" className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold" placeholder="예: 오사카" value={formData.dest} onChange={e => setFormData({...formData, dest: e.target.value})} />
                                </div>
                            </div>

                            <div className="w-full bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><BedDouble size={14}/> 예약한 숙소 (선택)</label>
                                <input type="text" className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold text-sm" placeholder="예: 신라호텔 (비워두면 AI가 추천!)" value={formData.accommodation} onChange={e => setFormData({...formData, accommodation: e.target.value})} />
                            </div>

                            <div className="flex gap-4">
                                <div className="flex-1 bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                    <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><Calendar size={14}/> 가는 날</label>
                                    <input type="date" className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold text-sm" value={formData.startDate} onChange={e => setFormData({...formData, startDate: e.target.value})} />
                                </div>
                                <div className="flex-1 bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                    <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><Calendar size={14}/> 오는 날</label>
                                    <input type="date" className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold text-sm" value={formData.endDate} onChange={e => setFormData({...formData, endDate: e.target.value})} />
                                </div>
                            </div>

                            <div className="flex gap-4">
                                <div className="flex-1 bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                    <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><Users size={14}/> 누구와</label>
                                    <select className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold text-sm cursor-pointer" value={formData.companion} onChange={e => setFormData({...formData, companion: e.target.value})}>
                                        <option>혼자</option><option>친구와</option><option>연인과</option><option>가족과</option>
                                    </select>
                                </div>
                                <div className="flex-[2] bg-white p-2 rounded-2xl shadow-sm border border-gray-100 focus-within:ring-2 focus-within:ring-blue-500 transition">
                                    <label className="flex items-center gap-1.5 text-xs font-bold text-gray-500 ml-2 mt-1"><Star size={14}/> 여행 테마</label>
                                    <input type="text" className="w-full p-2 bg-transparent outline-none text-gray-800 font-semibold text-sm" placeholder="예: 맛집 탐방, 힐링" value={formData.style} onChange={e => setFormData({...formData, style: e.target.value})} />
                                </div>
                            </div>

                            <div className="pt-2 flex gap-3">
                                <button onClick={() => setIsModalOpen(false)} className="flex-1 py-4 rounded-2xl font-bold text-gray-600 bg-gray-200 hover:bg-gray-300 transition">취소</button>
                                <button onClick={submitPlan} className="flex-[2] py-4 rounded-2xl font-black text-white bg-blue-600 hover:bg-blue-700 shadow-lg hover:shadow-xl transition transform hover:-translate-y-0.5">
                                    AI 일정 생성하기 🚀
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}