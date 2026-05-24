import { useState } from "react";
import { useNavigate } from "react-router";
import { User, Shield, Clock, TrendingUp } from "lucide-react";

export function ProfilePage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState({
    username: '장사꾼홍길동',
    gameName: '홍길동',
    email: 'honggildong@example.com',
    server: '한양',
    availability: '18시~24시',
  });

  const [isEditing, setIsEditing] = useState(false);

  const handleSave = () => {
    setIsEditing(false);
    alert('프로필이 저장되었습니다!');
  };

  return (
    <div className="max-w-[1200px] mx-auto px-6 py-8">
      <div className="mb-8">
        <h1 className="font-serif text-3xl font-bold text-[#3E3A36] mb-2">내 정보</h1>
        <p className="text-[#7A7368] text-sm">프로필을 관리하고 거래 내역을 확인하세요</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile Card */}
        <div className="lg:col-span-1">
          <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6">
            <div className="flex flex-col items-center mb-6">
              <div className="w-24 h-24 rounded-full bg-gradient-to-br from-[#E8DCCB] to-[#D8D0C5] border-4 border-[#DDD6CA] flex items-center justify-center font-serif text-4xl text-[#8B6B4A] font-bold mb-4">
                {profile.username[0]}
              </div>
              <h2 className="font-serif text-xl font-bold text-[#3E3A36] mb-1">{profile.username}</h2>
              <div className="bg-[#E8DCCB] text-[#8B6B4A] border border-[#C9A87A] text-xs px-3 py-1 rounded font-medium tracking-wide">
                대상
              </div>
            </div>

            <div className="space-y-4 mb-6">
              <div className="flex items-center gap-3 text-sm">
                <Shield className="w-4 h-4 text-[#8B6B4A]" />
                <span className="text-[#7A7368]">신뢰도:</span>
                <span className="font-semibold text-[#3E3A36]">98%</span>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <TrendingUp className="w-4 h-4 text-[#8B6B4A]" />
                <span className="text-[#7A7368]">거래 횟수:</span>
                <span className="font-semibold text-[#3E3A36]">142건</span>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <Clock className="w-4 h-4 text-[#8B6B4A]" />
                <span className="text-[#7A7368]">가입일:</span>
                <span className="font-semibold text-[#3E3A36]">2024.01.15</span>
              </div>
            </div>

            <button
              onClick={() => navigate('/deck')}
              className="w-full bg-[#8B6B4A] text-[#E8DCCB] py-2.5 rounded font-serif text-sm tracking-wide hover:bg-[#6A4F35] transition-colors"
            >
              내 덱 구성하기
            </button>
          </div>
        </div>

        {/* Profile Details */}
        <div className="lg:col-span-2">
          <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6 mb-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="font-serif text-xl font-semibold text-[#3E3A36] flex items-center gap-2">
                <User className="w-5 h-5 text-[#8B6B4A]" />
                프로필 정보
              </h3>
              {!isEditing ? (
                <button
                  onClick={() => setIsEditing(true)}
                  className="text-sm text-[#8B6B4A] hover:text-[#6A4F35] font-medium"
                >
                  수정
                </button>
              ) : (
                <div className="flex gap-2">
                  <button
                    onClick={() => setIsEditing(false)}
                    className="text-sm text-[#7A7368] hover:text-[#3E3A36]"
                  >
                    취소
                  </button>
                  <button
                    onClick={handleSave}
                    className="text-sm text-[#8B6B4A] hover:text-[#6A4F35] font-medium"
                  >
                    저장
                  </button>
                </div>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#7A7368] mb-2">닉네임</label>
                <input
                  type="text"
                  value={profile.username}
                  onChange={(e) => setProfile({ ...profile, username: e.target.value })}
                  disabled={!isEditing}
                  className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] disabled:bg-[#F5F1E8] disabled:text-[#7A7368]"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#7A7368] mb-2">게임 닉네임</label>
                <input
                  type="text"
                  value={profile.gameName}
                  onChange={(e) => setProfile({ ...profile, gameName: e.target.value })}
                  disabled={!isEditing}
                  className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] disabled:bg-[#F5F1E8] disabled:text-[#7A7368]"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#7A7368] mb-2">이메일</label>
                <input
                  type="email"
                  value={profile.email}
                  onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                  disabled={!isEditing}
                  className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] disabled:bg-[#F5F1E8] disabled:text-[#7A7368]"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#7A7368] mb-2">서버</label>
                <select
                  value={profile.server}
                  onChange={(e) => setProfile({ ...profile, server: e.target.value })}
                  disabled={!isEditing}
                  className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] disabled:bg-[#F5F1E8] disabled:text-[#7A7368]"
                >
                  <option>한양</option>
                  <option>금강</option>
                  <option>압록</option>
                  <option>두만</option>
                  <option>낙동</option>
                  <option>대동</option>
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-[#7A7368] mb-2">접속 가능 시간</label>
                <input
                  type="text"
                  value={profile.availability}
                  onChange={(e) => setProfile({ ...profile, availability: e.target.value })}
                  disabled={!isEditing}
                  className="w-full border border-[#DDD6CA] bg-white px-4 py-2.5 rounded text-sm outline-none focus:border-[#8B6B4A] disabled:bg-[#F5F1E8] disabled:text-[#7A7368]"
                  placeholder="예: 18시~24시, 종일, 주말"
                />
              </div>
            </div>
          </div>

          {/* Trading History */}
          <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6">
            <h3 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">최근 거래 내역</h3>
            <div className="space-y-3">
              {[
                { type: 'sell', item: '5북두 풀지국', price: 680, date: '2026.05.15', buyer: '대상주' },
                { type: 'buy', item: '화염의 검', price: 85, date: '2026.05.12', seller: '무기상인' },
                { type: 'sell', item: '3북두 풀지국반쌍', price: 390, date: '2026.05.10', buyer: '황금손길드' },
              ].map((trade, idx) => (
                <div key={idx} className="flex items-center justify-between p-3 bg-[#F5F1E8] border border-[#DDD6CA] rounded">
                  <div className="flex items-center gap-3">
                    <div className={`px-2 py-1 rounded text-[10px] font-semibold ${
                      trade.type === 'sell'
                        ? 'bg-[#EBF2E8] text-[#4A6B3A] border border-[#A8C49A]'
                        : 'bg-[#E8EEF5] text-[#3A4F6B] border border-[#9AAAC4]'
                    }`}>
                      {trade.type === 'sell' ? '판매' : '구매'}
                    </div>
                    <div>
                      <div className="text-sm font-medium text-[#3E3A36]">{trade.item}</div>
                      <div className="text-xs text-[#7A7368]">
                        {trade.type === 'sell' ? `구매자: ${trade.buyer}` : `판매자: ${trade.seller}`} • {trade.date}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-serif text-base font-bold text-[#8B6B4A]">{trade.price}억</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
