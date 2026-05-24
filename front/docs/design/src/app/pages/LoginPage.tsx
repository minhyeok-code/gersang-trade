import { useState } from "react";
import { useNavigate } from "react-router";

export function LoginPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'findId' | 'register'>('login');
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    email: '',
    confirmPassword: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === 'login') {
      // Mock login
      navigate('/profile');
    } else if (mode === 'register') {
      // Mock registration
      alert('회원가입이 완료되었습니다!');
      setMode('login');
    }
  };

  return (
    <div className="min-h-[calc(100vh-52px)] flex items-center justify-center px-6 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="font-serif text-3xl font-bold text-[#3E3A36] mb-2">거상인</h1>
          <p className="text-[#7A7368] text-sm">게임 아이템 거래 플랫폼</p>
        </div>

        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg shadow-lg p-8">
          {/* Tab Navigation */}
          <div className="flex gap-2 mb-6 border-b border-[#DDD6CA]">
            <button
              onClick={() => setMode('login')}
              className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
                mode === 'login' ? 'border-[#8B6B4A] text-[#3E3A36]' : 'border-transparent text-[#7A7368] hover:text-[#3E3A36]'
              }`}
            >
              로그인
            </button>
            <button
              onClick={() => setMode('findId')}
              className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
                mode === 'findId' ? 'border-[#8B6B4A] text-[#3E3A36]' : 'border-transparent text-[#7A7368] hover:text-[#3E3A36]'
              }`}
            >
              아이디찾기
            </button>
            <button
              onClick={() => setMode('register')}
              className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
                mode === 'register' ? 'border-[#8B6B4A] text-[#3E3A36]' : 'border-transparent text-[#7A7368] hover:text-[#3E3A36]'
              }`}
            >
              회원가입
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Login Mode */}
            {mode === 'login' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">아이디</label>
                  <input
                    type="text"
                    value={formData.username}
                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="아이디를 입력하세요"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">비밀번호</label>
                  <input
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="비밀번호를 입력하세요"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full bg-[#8B6B4A] text-[#E8DCCB] py-3 rounded font-serif text-sm tracking-wide hover:bg-[#6A4F35] transition-colors"
                >
                  로그인
                </button>
              </>
            )}

            {/* Find ID Mode */}
            {mode === 'findId' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">이메일</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="등록한 이메일을 입력하세요"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full bg-[#8B6B4A] text-[#E8DCCB] py-3 rounded font-serif text-sm tracking-wide hover:bg-[#6A4F35] transition-colors"
                >
                  아이디 찾기
                </button>
              </>
            )}

            {/* Register Mode */}
            {mode === 'register' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">아이디</label>
                  <input
                    type="text"
                    value={formData.username}
                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="사용할 아이디를 입력하세요"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">이메일</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="이메일을 입력하세요"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">비밀번호</label>
                  <input
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="비밀번호를 입력하세요"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3E3A36] mb-2">비밀번호 확인</label>
                  <input
                    type="password"
                    value={formData.confirmPassword}
                    onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                    className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] transition-colors"
                    placeholder="비밀번호를 다시 입력하세요"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full bg-[#8B6B4A] text-[#E8DCCB] py-3 rounded font-serif text-sm tracking-wide hover:bg-[#6A4F35] transition-colors"
                >
                  회원가입
                </button>
              </>
            )}
          </form>

          <div className="mt-6 text-center">
            <p className="text-xs text-[#7A7368]">
              문제가 있으신가요?{' '}
              <a href="#" className="text-[#8B6B4A] hover:underline">
                고객센터
              </a>
            </p>
          </div>
        </div>

        <div className="mt-6 text-center text-xs text-[#7A7368]">
          <p>거상인은 안전한 거래를 위해 최선을 다하고 있습니다.</p>
          <p className="mt-1">회원가입 시 이용약관 및 개인정보 처리방침에 동의한 것으로 간주됩니다.</p>
        </div>
      </div>
    </div>
  );
}
