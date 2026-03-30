import { Link } from 'react-router-dom';
import { Mail, Lock, User } from 'lucide-react';

export default function Signup() {
    return (
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-4 py-10">
            <div className="bg-white p-8 rounded-3xl shadow-lg border border-gray-100 w-full max-w-md animate-in fade-in zoom-in duration-300">
                <h2 className="text-3xl font-black text-center text-gray-800 mb-2">시작하기 🚀</h2>
                <p className="text-center text-gray-500 mb-8">무료로 가입하고 AI 일정을 무제한으로 짜보세요</p>

                <form className="space-y-5">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">이름</label>
                        <div className="relative">
                            <User className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                            <input type="text" placeholder="홍길동" className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none transition" />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">이메일</label>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                            <input type="email" placeholder="example@compass.com" className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none transition" />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">비밀번호</label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                            <input type="password" placeholder="8자리 이상 입력해주세요" className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none transition" />
                        </div>
                    </div>

                    <button type="button" className="w-full py-3.5 mt-4 bg-gray-800 text-white font-bold rounded-xl hover:bg-gray-900 transition shadow-md">
                        이메일로 회원가입
                    </button>
                </form>

                <p className="mt-8 text-center text-sm text-gray-600">
                    이미 계정이 있으신가요? <Link to="/login" className="text-blue-600 font-bold hover:underline">로그인</Link>
                </p>
            </div>
        </div>
    );
}