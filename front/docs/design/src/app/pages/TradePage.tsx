import { useState } from "react";
import { Drawer } from "../components/ui/drawer";

interface TradeCard {
  id: string;
  type: 'sell' | 'buy';
  nickname: string;
  gameName: string;
  title: string;
  price: number;
  time: string;
  availability: string;
}

export function TradePage() {
  const [selectedTab, setSelectedTab] = useState<'all' | 'sell' | 'buy'>('all');
  const [selectedCard, setSelectedCard] = useState<TradeCard | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const trades: TradeCard[] = [
    { id: '1', type: 'sell', nickname: '장사꾼홍길동', gameName: '홍길동', title: '5북두 풀지국', price: 680, time: '13분 전', availability: '18시~24시' },
    { id: '2', type: 'sell', nickname: '천하제일상인', gameName: '제일상인', title: '3북두 풀지국반쌍', price: 390, time: '42분 전', availability: '20시~24시' },
    { id: '3', type: 'sell', nickname: '압록강상단', gameName: '강상단장', title: '풀지국', price: 115, time: '2시간 전', availability: '종일' },
    { id: '4', type: 'sell', nickname: '묵향상회', gameName: '묵향주인', title: '5천추 변지국', price: 88, time: '4시간 전', availability: '저녁' },
    { id: '5', type: 'sell', nickname: '비단상인', gameName: '비단이', title: '갑투지국', price: 32, time: '어제', availability: '22시~02시' },
    { id: '6', type: 'buy', nickname: '대상주', gameName: '대상주각하', title: '5북두 풀지국', price: 650, time: '5분 전', availability: '21시~24시' },
    { id: '7', type: 'buy', nickname: '황금손길드', gameName: '황금손', title: '북두 갑투광목', price: 220, time: '1시간 전', availability: '종일' },
    { id: '8', type: 'buy', nickname: '청룡상단', gameName: '청룡단주', title: '3북두 풀증장', price: 180, time: '3시간 전', availability: '20시~23시' },
    { id: '9', type: 'buy', nickname: '한양부자', gameName: '부자님', title: '5개양 변군다리', price: 95, time: '5시간 전', availability: '저녁~새벽' },
    { id: '10', type: 'buy', nickname: '초보상인', gameName: '초보냠냠', title: '풀광목', price: 28, time: '어제', availability: '주말' },
  ];

  const filteredTrades = selectedTab === 'all' ? trades : trades.filter(t => t.type === selectedTab);
  const sellTrades = filteredTrades.filter(t => t.type === 'sell');
  const buyTrades = filteredTrades.filter(t => t.type === 'buy');

  return (
    <div className="flex min-h-[calc(100vh-52px)]">
      {/* Sidebar */}
      <aside className="w-[210px] flex-shrink-0 border-r border-[#DDD6CA] bg-[#FAF8F2] p-5 sticky top-[52px] h-[calc(100vh-52px)] overflow-y-auto">
        <div className="pb-4 border-b border-[#DDD6CA] mb-4">
          <label className="font-serif text-[10px] font-semibold tracking-[0.18em] text-[#8B6B4A] uppercase block mb-2.5">구 분</label>
          <div className="flex gap-1 flex-wrap">
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-[#8B6B4A] text-[#E8DCCB] rounded text-[11px]">전체</button>
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-transparent text-[#7A7368] rounded text-[11px] hover:border-[#8B6B4A] hover:text-[#8B6B4A]">단품</button>
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-transparent text-[#7A7368] rounded text-[11px] hover:border-[#8B6B4A] hover:text-[#8B6B4A]">세트</button>
          </div>
        </div>

        <div className="pb-4 border-b border-[#DDD6CA] mb-4">
          <label className="font-serif text-[10px] font-semibold tracking-[0.18em] text-[#8B6B4A] uppercase block mb-2.5">세트명</label>
          <input
            type="text"
            placeholder="세트명 검색..."
            className="w-full border border-[#DDD6CA] bg-[#F5F1E8] px-2.5 py-1.5 rounded text-xs text-[#3E3A36] outline-none focus:border-[#8B6B4A] placeholder:text-[#D8D0C5]"
          />
        </div>

        <div className="pb-4 border-b border-[#DDD6CA] mb-4">
          <label className="font-serif text-[10px] font-semibold tracking-[0.18em] text-[#8B6B4A] uppercase block mb-2.5">구 성</label>
          <div className="flex gap-1 flex-wrap">
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-[#8B6B4A] text-[#E8DCCB] rounded text-[11px]">전체</button>
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-transparent text-[#7A7368] rounded text-[11px] hover:border-[#8B6B4A] hover:text-[#8B6B4A]">갑투</button>
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-transparent text-[#7A7368] rounded text-[11px] hover:border-[#8B6B4A] hover:text-[#8B6B4A]">변</button>
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-transparent text-[#7A7368] rounded text-[11px] hover:border-[#8B6B4A] hover:text-[#8B6B4A]">풀</button>
            <button className="px-2.5 py-1 border border-[#DDD6CA] bg-transparent text-[#7A7368] rounded text-[11px] hover:border-[#8B6B4A] hover:text-[#8B6B4A]">풀반쌍</button>
          </div>
        </div>

        <div className="pb-4">
          <label className="font-serif text-[10px] font-semibold tracking-[0.18em] text-[#8B6B4A] uppercase block mb-2.5">주 술</label>
          <div className="flex flex-col gap-0.5">
            {['전체', '없음', '북두칠성', '천추', '천기', '천권', '개양', '요광'].map((item, idx) => (
              <div
                key={item}
                className={`flex items-center gap-2 px-1.5 py-1 rounded cursor-pointer text-xs transition-colors ${
                  idx === 0 ? 'text-[#8B6B4A] font-medium' : 'text-[#7A7368] hover:bg-[#F5F1E8] hover:text-[#3E3A36]'
                }`}
              >
                <div className={`w-3 h-3 border border-[#DDD6CA] rounded flex items-center justify-center text-[9px] ${idx === 0 ? 'bg-[#8B6B4A] border-[#8B6B4A] text-white' : ''}`}>
                  {idx === 0 ? '✓' : ''}
                </div>
                {item}
              </div>
            ))}
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 min-w-0 flex flex-col">
        {/* Tab Bar */}
        <div className="bg-[#FAF8F2] border-b border-[#DDD6CA] px-5 flex items-center gap-0 h-11 sticky top-[52px] z-[100]">
          <button
            onClick={() => setSelectedTab('all')}
            className={`h-11 px-4 border-b-2 -mb-px text-sm transition-colors ${
              selectedTab === 'all' ? 'text-[#3E3A36] border-[#8B6B4A] font-medium' : 'text-[#7A7368] border-transparent hover:text-[#3E3A36]'
            }`}
          >
            전체
          </button>
          <button
            onClick={() => setSelectedTab('sell')}
            className={`h-11 px-4 border-b-2 -mb-px text-sm transition-colors ${
              selectedTab === 'sell' ? 'text-[#3E3A36] border-[#8B6B4A] font-medium' : 'text-[#7A7368] border-transparent hover:text-[#3E3A36]'
            }`}
          >
            팝니다
          </button>
          <button
            onClick={() => setSelectedTab('buy')}
            className={`h-11 px-4 border-b-2 -mb-px text-sm transition-colors ${
              selectedTab === 'buy' ? 'text-[#3E3A36] border-[#8B6B4A] font-medium' : 'text-[#7A7368] border-transparent hover:text-[#3E3A36]'
            }`}
          >
            삽니다
          </button>
          <div className="flex-1"></div>
          <button
            onClick={() => setDrawerOpen(true)}
            className="bg-[#8B6B4A] text-[#E8DCCB] px-4 py-1.5 rounded text-xs font-serif tracking-wide hover:bg-[#6A4F35] transition-colors mr-2"
          >
            시세 검색
          </button>
          <button className="bg-[#8B6B4A] text-[#E8DCCB] px-4 py-1.5 rounded text-xs font-serif tracking-wide hover:bg-[#6A4F35] transition-colors">
            + 등록하기
          </button>
        </div>

        {/* List Area */}
        <div className="flex-1 grid grid-cols-2 gap-0">
          {/* Sell Column */}
          <div className="border-r border-[#DDD6CA] flex flex-col">
            <div className="px-4 py-3 border-b border-[#DDD6CA] flex items-center justify-between bg-[#FAF8F2] sticky top-[96px] z-[50]">
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-[#EBF2E8] text-[#4A6B3A] border border-[#A8C49A]">팝니다</span>
                <span className="text-[11px] text-[#7A7368]">{sellTrades.length}건</span>
              </div>
              <div className="flex gap-1">
                <button className="border border-[#DDD6CA] bg-[#8B6B4A] text-[#E8DCCB] px-2 py-0.5 text-[10px] rounded">최신</button>
                <button className="border border-[#DDD6CA] bg-transparent text-[#7A7368] px-2 py-0.5 text-[10px] rounded hover:border-[#8B6B4A] hover:text-[#8B6B4A]">낮은가</button>
                <button className="border border-[#DDD6CA] bg-transparent text-[#7A7368] px-2 py-0.5 text-[10px] rounded hover:border-[#8B6B4A] hover:text-[#8B6B4A]">높은가</button>
              </div>
            </div>
            <div className="p-3 flex flex-col gap-1.5">
              {sellTrades.map((card) => (
                <div
                  key={card.id}
                  onClick={() => setSelectedCard(card)}
                  className="bg-[#FAF8F2] border border-[#DDD6CA] rounded px-3.5 py-3 cursor-pointer transition-all hover:border-[#8B6B4A] hover:shadow-md relative before:absolute before:left-0 before:top-1 before:bottom-1 before:w-0.5 before:rounded-r before:bg-[#A8C49A]/60"
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-[11px] text-[#7A7368]">{card.nickname}</span>
                  </div>
                  <div className="h-px bg-[#DDD6CA] mb-2"></div>
                  <div className="font-serif text-[15px] font-semibold text-[#3E3A36] mb-1">{card.title}</div>
                  <div className="font-serif text-base font-bold text-[#8B6B4A] mb-1.5">{card.price}억 전</div>
                  <div className="text-[11px] text-[#7A7368] flex items-center justify-between">
                    <div className="flex gap-2">
                      <span>게임닉: {card.gameName}</span>
                      <span>{card.availability}</span>
                    </div>
                    <span className="text-[#D8D0C5]">{card.time}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Buy Column */}
          <div className="flex flex-col">
            <div className="px-4 py-3 border-b border-[#DDD6CA] flex items-center justify-between bg-[#FAF8F2] sticky top-[96px] z-[50]">
              <div className="flex items-center gap-2">
                <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-[#E8EEF5] text-[#3A4F6B] border border-[#9AAAC4]">삽니다</span>
                <span className="text-[11px] text-[#7A7368]">{buyTrades.length}건</span>
              </div>
              <div className="flex gap-1">
                <button className="border border-[#DDD6CA] bg-[#8B6B4A] text-[#E8DCCB] px-2 py-0.5 text-[10px] rounded">최신</button>
                <button className="border border-[#DDD6CA] bg-transparent text-[#7A7368] px-2 py-0.5 text-[10px] rounded hover:border-[#8B6B4A] hover:text-[#8B6B4A]">낮은가</button>
                <button className="border border-[#DDD6CA] bg-transparent text-[#7A7368] px-2 py-0.5 text-[10px] rounded hover:border-[#8B6B4A] hover:text-[#8B6B4A]">높은가</button>
              </div>
            </div>
            <div className="p-3 flex flex-col gap-1.5">
              {buyTrades.map((card) => (
                <div
                  key={card.id}
                  onClick={() => setSelectedCard(card)}
                  className="bg-[#FAF8F2] border border-[#DDD6CA] rounded px-3.5 py-3 cursor-pointer transition-all hover:border-[#8B6B4A] hover:shadow-md relative before:absolute before:left-0 before:top-1 before:bottom-1 before:w-0.5 before:rounded-r before:bg-[#9AAAC4]/60"
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-[11px] text-[#7A7368]">{card.nickname}</span>
                  </div>
                  <div className="h-px bg-[#DDD6CA] mb-2"></div>
                  <div className="font-serif text-[15px] font-semibold text-[#3E3A36] mb-1">{card.title}</div>
                  <div className="font-serif text-base font-bold text-[#8B6B4A] mb-1.5">{card.price}억 전</div>
                  <div className="text-[11px] text-[#7A7368] flex items-center justify-between">
                    <div className="flex gap-2">
                      <span>게임닉: {card.gameName}</span>
                      <span>{card.availability}</span>
                    </div>
                    <span className="text-[#D8D0C5]">{card.time}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedCard && (
        <div
          className="fixed inset-0 bg-black/65 z-[500] flex items-center justify-center"
          onClick={() => setSelectedCard(null)}
        >
          <div
            className="bg-[#FAF8F2] border border-[#DDD6CA] rounded w-[660px] max-w-[90vw] shadow-[0_24px_60px_rgba(0,0,0,0.3)] overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-5 py-3.5 border-b border-[#DDD6CA] bg-[#F5F1E8]">
              <span className="font-serif text-sm font-semibold text-[#3E3A36]">거래 상세</span>
              <button
                onClick={() => setSelectedCard(null)}
                className="text-[#7A7368] text-lg hover:text-[#3E3A36] transition-colors px-1.5"
              >
                ✕
              </button>
            </div>
            <div className="grid grid-cols-[170px_1fr]">
              {/* Profile Panel */}
              <div className="border-r border-[#DDD6CA] p-5 bg-[#F5F1E8] flex flex-col items-center gap-2.5">
                <div className="w-14 h-14 rounded-full bg-gradient-to-br from-[#E8DCCB] to-[#D8D0C5] border-2 border-[#DDD6CA] flex items-center justify-center font-serif text-xl text-[#8B6B4A] font-semibold">
                  {selectedCard.nickname[0]}
                </div>
                <div className="font-serif text-sm font-semibold text-[#3E3A36]">{selectedCard.nickname}</div>
                <div className="bg-[#E8DCCB] text-[#8B6B4A] border border-[#C9A87A] text-[10px] px-2 py-0.5 rounded font-medium tracking-wide">
                  대상
                </div>
                <div className="w-full flex flex-col gap-1.5 mt-1">
                  <div className="flex flex-col gap-0.5">
                    <span className="text-[9px] text-[#D8D0C5] tracking-widest uppercase">서버</span>
                    <span className="text-xs text-[#3E3A36]">한양</span>
                  </div>
                  <div className="flex flex-col gap-0.5">
                    <span className="text-[9px] text-[#D8D0C5] tracking-widest uppercase">게임 닉네임</span>
                    <span className="text-xs text-[#3E3A36]">{selectedCard.gameName}</span>
                  </div>
                  <div className="flex flex-col gap-0.5">
                    <span className="text-[9px] text-[#D8D0C5] tracking-widest uppercase">접속 가능</span>
                    <span className="text-xs text-[#3E3A36]">{selectedCard.availability}</span>
                  </div>
                </div>
              </div>

              {/* Trade Info */}
              <div className="p-5">
                <div className="w-16 h-16 bg-gradient-to-br from-[#E8DCCB] to-[#D8D0C5] border border-[#DDD6CA] rounded flex items-center justify-center text-[28px] mb-3.5">
                  ⚔️
                </div>
                <div className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold mb-2 ${
                  selectedCard.type === 'sell' 
                    ? 'bg-[#EBF2E8] text-[#4A6B3A] border border-[#A8C49A]' 
                    : 'bg-[#E8EEF5] text-[#3A4F6B] border border-[#9AAAC4]'
                }`}>
                  {selectedCard.type === 'sell' ? '팝니다' : '삽니다'}
                </div>
                <div className="font-serif text-xl font-bold text-[#3E3A36] mb-1">{selectedCard.title}</div>
                <div className="font-serif text-[22px] font-bold text-[#8B6B4A] mb-4">{selectedCard.price}억 전</div>
                <table className="w-full mb-3.5">
                  <tbody>
                    <tr className="border-b border-[#DDD6CA]">
                      <td className="py-1.5 px-1 text-xs text-[#7A7368] w-[70px]">구성</td>
                      <td className="py-1.5 px-1 text-xs text-[#3E3A36]">풀세트 (갑옷·투구·요대·신발·장갑) — 반지 미포함</td>
                    </tr>
                    <tr className="border-b border-[#DDD6CA]">
                      <td className="py-1.5 px-1 text-xs text-[#7A7368]">주술</td>
                      <td className="py-1.5 px-1 text-xs text-[#3E3A36]">북두칠성(대성공) 5셋 — 전 피스</td>
                    </tr>
                    <tr>
                      <td className="py-1.5 px-1 text-xs text-[#7A7368]">등록</td>
                      <td className="py-1.5 px-1 text-xs text-[#3E3A36]">{selectedCard.time}</td>
                    </tr>
                  </tbody>
                </table>
                <div className="bg-[#F5F1E8] border border-[#DDD6CA] rounded px-3 py-2 text-xs text-[#7A7368] italic">
                  협의 가능. 현금 거래 X
                </div>
              </div>
            </div>
            <div className="px-5 py-3 border-t border-[#DDD6CA] bg-[#F5F1E8] flex justify-end gap-2">
              <button className="border border-[#DDD6CA] bg-transparent text-[#7A7368] px-4 py-1.5 rounded text-xs hover:border-[#C24A4A] hover:text-[#C24A4A] transition-colors">
                신고
              </button>
              <button className="bg-[#8B6B4A] text-[#E8DCCB] px-5 py-1.5 rounded text-xs font-serif tracking-wide hover:bg-[#6A4F35] transition-colors">
                채팅하기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Price Search Drawer */}
      <Drawer open={drawerOpen} onOpenChange={setDrawerOpen}>
        <div className="p-6">
          <h2 className="font-serif text-2xl font-bold text-[#3E3A36] mb-4">시세 검색</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-[#3E3A36] mb-2">아이템명</label>
              <input
                type="text"
                placeholder="아이템명을 입력하세요"
                className="w-full border border-[#DDD6CA] bg-white px-3 py-2 rounded text-sm outline-none focus:border-[#8B6B4A]"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-[#3E3A36] mb-2">세트 구성</label>
              <select className="w-full border border-[#DDD6CA] bg-white px-3 py-2 rounded text-sm outline-none focus:border-[#8B6B4A]">
                <option>전체</option>
                <option>단품</option>
                <option>세트</option>
              </select>
            </div>
            <button className="w-full bg-[#8B6B4A] text-[#E8DCCB] py-2.5 rounded font-serif hover:bg-[#6A4F35] transition-colors">
              검색
            </button>
          </div>
        </div>
      </Drawer>
    </div>
  );
}
