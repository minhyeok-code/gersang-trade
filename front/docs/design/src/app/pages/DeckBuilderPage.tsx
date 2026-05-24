import { useState } from "react";
import { Search, X } from "lucide-react";

type Nation = '한국' | '중국' | '일본' | '대만' | '인도';
type Gender = '남' | '여';

interface Hero {
  nation: Nation;
  gender: Gender;
  name: string;
}

interface Mercenary {
  id: string;
  name: string;
  category: string;
  element: string;
}

interface Equipment {
  id: string;
  name: string;
  type: string;
}

interface DeckSlot {
  mercenary: Mercenary | null;
  equipment: {
    charm?: Equipment;
    helmet?: Equipment;
    gloves?: Equipment;
    weapon?: Equipment;
    armor?: Equipment;
    belt?: Equipment;
    ring1?: Equipment;
    shoes?: Equipment;
    ring2?: Equipment;
    energy?: Equipment;
    weaponAcc?: Equipment;
    top?: Equipment;
    bottom?: Equipment;
    necklace?: Equipment;
    earring?: Equipment;
    legging?: Equipment;
    bracelet?: Equipment;
  };
}

interface Monster {
  id: string;
  name: string;
  element: string;
  resistance: number;
}

const heroes: Hero[] = [
  { nation: '한국', gender: '남', name: '한국 남자 주인공' },
  { nation: '한국', gender: '여', name: '한국 여자 주인공' },
  { nation: '중국', gender: '남', name: '중국 남자 주인공' },
  { nation: '중국', gender: '여', name: '중국 여자 주인공' },
  { nation: '일본', gender: '남', name: '일본 남자 주인공' },
  { nation: '일본', gender: '여', name: '일본 여자 주인공' },
  { nation: '대만', gender: '남', name: '대만 남자 주인공' },
  { nation: '대만', gender: '여', name: '대만 여자 주인공' },
  { nation: '인도', gender: '남', name: '인도 남자 주인공' },
  { nation: '인도', gender: '여', name: '인도 여자 주인공' },
];

const sampleMercenaries: Mercenary[] = [
  { id: 'm1', name: '무사', category: '전사', element: '화' },
  { id: 'm2', name: '도사', category: '마법사', element: '수' },
  { id: 'm3', name: '상인', category: '지원', element: '토' },
  { id: 'm4', name: '자객', category: '암살자', element: '풍' },
  { id: 'm5', name: '승려', category: '힐러', element: '화' },
];

const sampleMonsters: Monster[] = [
  { id: 'mon1', name: '화염 드래곤', element: '화', resistance: 30 },
  { id: 'mon2', name: '얼음 골렘', element: '수', resistance: 40 },
  { id: 'mon3', name: '바람의 정령', element: '풍', resistance: 25 },
  { id: 'mon4', name: '대지의 거인', element: '토', resistance: 50 },
];

export function DeckBuilderPage() {
  const [selectedHero, setSelectedHero] = useState<Hero>(heroes[0]);
  const [showHeroSelector, setShowHeroSelector] = useState(false);
  const [deck, setDeck] = useState<DeckSlot[]>(Array(12).fill(null).map(() => ({ mercenary: null, equipment: {} })));
  const [selectedSlotIndex, setSelectedSlotIndex] = useState<number | null>(null);
  const [showMercenarySelector, setShowMercenarySelector] = useState(false);
  const [showEquipmentSelector, setShowEquipmentSelector] = useState<string | null>(null);
  const [mercenarySearch, setMercenarySearch] = useState('');
  const [selectedMonster, setSelectedMonster] = useState<Monster | null>(sampleMonsters[0]);
  const [showMonsterSelector, setShowMonsterSelector] = useState(false);

  const handleSelectMercenary = (mercenary: Mercenary) => {
    if (selectedSlotIndex !== null) {
      const newDeck = [...deck];
      newDeck[selectedSlotIndex].mercenary = mercenary;
      setDeck(newDeck);
      setShowMercenarySelector(false);
      setSelectedSlotIndex(null);
      setMercenarySearch('');
    }
  };

  const handleEquipmentSlotClick = (slotIndex: number, equipType: string) => {
    setSelectedSlotIndex(slotIndex);
    setShowEquipmentSelector(equipType);
  };

  const calculateDPSShare = (index: number): number => {
    if (!deck[index].mercenary) return 0;
    // Mock calculation - in real app would be based on equipment and stats
    return Math.random() * 2;
  };

  const filteredMercenaries = sampleMercenaries.filter(m =>
    m.name.toLowerCase().includes(mercenarySearch.toLowerCase()) ||
    m.category.toLowerCase().includes(mercenarySearch.toLowerCase())
  );

  return (
    <div className="max-w-[1600px] mx-auto px-6 py-8">
      <div className="mb-8">
        <h1 className="font-serif text-3xl font-bold text-[#3E3A36] mb-2">내 덱 구성하기</h1>
        <p className="text-[#7A7368] text-sm">용병과 장비를 선택하여 최적의 전투력을 구성하세요</p>
      </div>

      {/* Hero Selection */}
      <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6 mb-6">
        <h2 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">주인공 선택</h2>
        <div className="flex items-center gap-4">
          <div className="w-20 h-20 rounded-lg bg-gradient-to-br from-[#E8DCCB] to-[#8B6B4A]/30 border-2 border-[#8B6B4A] flex items-center justify-center text-3xl">
            👤
          </div>
          <div className="flex-1">
            <div className="font-medium text-[#3E3A36] mb-1">{selectedHero.name}</div>
            <div className="text-sm text-[#7A7368]">국적: {selectedHero.nation} • 성별: {selectedHero.gender}</div>
          </div>
          <button
            onClick={() => setShowHeroSelector(true)}
            className="bg-[#8B6B4A] text-[#E8DCCB] px-4 py-2 rounded text-sm hover:bg-[#6A4F35] transition-colors"
          >
            변경
          </button>
        </div>
      </div>

      {/* Deck Slots */}
      <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6 mb-6">
        <h2 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">용병 구성 (12명)</h2>
        <div className="grid grid-cols-4 gap-4">
          {deck.map((slot, index) => {
            const dpsShare = calculateDPSShare(index);
            return (
              <div
                key={index}
                className={`border-2 rounded-lg p-4 transition-all ${
                  index === 0
                    ? 'border-[#8B6B4A] bg-[#8B6B4A]/5'
                    : slot.mercenary
                    ? 'border-[#DDD6CA] bg-white hover:border-[#8B6B4A] cursor-pointer'
                    : 'border-dashed border-[#DDD6CA] bg-[#F5F1E8] hover:border-[#8B6B4A] cursor-pointer'
                }`}
                onClick={() => {
                  if (index !== 0) {
                    setSelectedSlotIndex(index);
                    setShowMercenarySelector(true);
                  }
                }}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-semibold text-[#7A7368]">
                    {index === 0 ? '주인공' : `용병 ${index}`}
                  </span>
                  {slot.mercenary && dpsShare > 0 && (
                    <span className="text-xs font-bold text-[#8B6B4A]">
                      {dpsShare.toFixed(1)}
                    </span>
                  )}
                </div>
                {index === 0 ? (
                  <div className="text-center py-6">
                    <div className="text-3xl mb-2">👤</div>
                    <div className="text-sm font-medium text-[#3E3A36]">{selectedHero.nation}</div>
                  </div>
                ) : slot.mercenary ? (
                  <div className="text-center py-4">
                    <div className="text-2xl mb-2">⚔️</div>
                    <div className="text-sm font-medium text-[#3E3A36] mb-1">{slot.mercenary.name}</div>
                    <div className="text-xs text-[#7A7368]">{slot.mercenary.category}</div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedSlotIndex(index);
                      }}
                      className="mt-3 w-full bg-[#8B6B4A] text-[#E8DCCB] px-2 py-1 rounded text-xs hover:bg-[#6A4F35] transition-colors"
                    >
                      장비 설정
                    </button>
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <div className="text-4xl text-[#D8D0C5] mb-2">+</div>
                    <div className="text-xs text-[#7A7368]">용병 선택</div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Equipment Configuration */}
      {selectedSlotIndex !== null && deck[selectedSlotIndex].mercenary && !showMercenarySelector && (
        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-serif text-xl font-semibold text-[#3E3A36]">
              장비 설정 - {deck[selectedSlotIndex].mercenary?.name}
            </h2>
            <button
              onClick={() => setSelectedSlotIndex(null)}
              className="text-[#7A7368] hover:text-[#3E3A36]"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          
          <div className="grid grid-cols-2 gap-8">
            {/* 3x3 Equipment Grid */}
            <div>
              <h3 className="text-sm font-semibold text-[#7A7368] mb-3 uppercase tracking-wide">주 장비</h3>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { key: 'charm', label: '부적' },
                  { key: 'helmet', label: '투구' },
                  { key: 'gloves', label: '장갑' },
                  { key: 'weapon', label: '무기' },
                  { key: 'armor', label: '갑옷' },
                  { key: 'belt', label: '요대' },
                  { key: 'ring1', label: '반지1' },
                  { key: 'shoes', label: '신발' },
                  { key: 'ring2', label: '반지2' },
                ].map((item) => (
                  <button
                    key={item.key}
                    onClick={() => handleEquipmentSlotClick(selectedSlotIndex, item.key)}
                    className={`aspect-[3/4] border-2 border-dashed border-[#DDD6CA] rounded-lg bg-white hover:border-[#8B6B4A] transition-colors flex flex-col items-center justify-center ${
                      item.key === 'shoes' ? 'row-span-1' : ''
                    }`}
                  >
                    <div className="text-2xl mb-1">📦</div>
                    <div className="text-[10px] text-[#7A7368] text-center">{item.label}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* 2x4 Accessory Grid */}
            <div>
              <h3 className="text-sm font-semibold text-[#7A7368] mb-3 uppercase tracking-wide">액세서리</h3>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { key: 'energy', label: '기운' },
                  { key: 'necklace', label: '목걸이' },
                  { key: 'weaponAcc', label: '무기' },
                  { key: 'earring', label: '귀걸이' },
                  { key: 'top', label: '상의' },
                  { key: 'legging', label: '각반' },
                  { key: 'bottom', label: '하의' },
                  { key: 'bracelet', label: '팔찌' },
                ].map((item) => (
                  <button
                    key={item.key}
                    onClick={() => handleEquipmentSlotClick(selectedSlotIndex, item.key)}
                    className="aspect-square border-2 border-dashed border-[#DDD6CA] rounded-lg bg-white hover:border-[#8B6B4A] transition-colors flex flex-col items-center justify-center"
                  >
                    <div className="text-2xl mb-1">💎</div>
                    <div className="text-[10px] text-[#7A7368] text-center">{item.label}</div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Monster Selection & Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6">
          <h2 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">대상 몬스터</h2>
          {selectedMonster ? (
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-lg bg-gradient-to-br from-[#D94F3D]/30 to-[#8B6B4A]/20 border-2 border-[#D94F3D]/40 flex items-center justify-center text-3xl">
                🐉
              </div>
              <div className="flex-1">
                <div className="font-medium text-[#3E3A36] mb-1">{selectedMonster.name}</div>
                <div className="text-sm text-[#7A7368]">
                  속성: {selectedMonster.element} • 저항: {selectedMonster.resistance}%
                </div>
              </div>
              <button
                onClick={() => setShowMonsterSelector(true)}
                className="bg-[#8B6B4A] text-[#E8DCCB] px-4 py-2 rounded text-sm hover:bg-[#6A4F35] transition-colors"
              >
                변경
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowMonsterSelector(true)}
              className="w-full border-2 border-dashed border-[#DDD6CA] rounded-lg py-8 hover:border-[#8B6B4A] transition-colors"
            >
              <div className="text-center">
                <div className="text-4xl mb-2">+</div>
                <div className="text-sm text-[#7A7368]">몬스터 선택</div>
              </div>
            </button>
          )}
        </div>

        <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg p-6">
          <h2 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">전투력 분석</h2>
          <div className="space-y-3">
            <div className="flex items-center justify-between p-3 bg-[#F5F1E8] rounded">
              <span className="text-sm text-[#7A7368]">총 DPS</span>
              <span className="font-serif text-xl font-bold text-[#8B6B4A]">12,450</span>
            </div>
            <div className="flex items-center justify-between p-3 bg-[#F5F1E8] rounded">
              <span className="text-sm text-[#7A7368]">저항 감소</span>
              <span className="font-serif text-xl font-bold text-[#8B6B4A]">-25%</span>
            </div>
            {selectedMonster && (
              <>
                <div className="flex items-center justify-between p-3 bg-[#F5F1E8] rounded">
                  <span className="text-sm text-[#7A7368]">적 속성</span>
                  <span className="font-serif text-lg font-bold text-[#3E3A36]">{selectedMonster.element}</span>
                </div>
                <div className="flex items-center justify-between p-3 bg-[#F5F1E8] rounded">
                  <span className="text-sm text-[#7A7368]">적 저항</span>
                  <span className="font-serif text-lg font-bold text-[#3E3A36]">{selectedMonster.resistance}%</span>
                </div>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Hero Selector Modal */}
      {showHeroSelector && (
        <div className="fixed inset-0 bg-black/65 z-[500] flex items-center justify-center" onClick={() => setShowHeroSelector(false)}>
          <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg w-[600px] max-w-[90vw] p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">주인공 선택</h3>
            <div className="grid grid-cols-2 gap-3 max-h-[60vh] overflow-y-auto">
              {heroes.map((hero, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    setSelectedHero(hero);
                    setShowHeroSelector(false);
                  }}
                  className={`p-4 border-2 rounded-lg text-left transition-all ${
                    selectedHero.nation === hero.nation && selectedHero.gender === hero.gender
                      ? 'border-[#8B6B4A] bg-[#8B6B4A]/10'
                      : 'border-[#DDD6CA] hover:border-[#8B6B4A]'
                  }`}
                >
                  <div className="text-2xl mb-2">👤</div>
                  <div className="text-sm font-medium text-[#3E3A36]">{hero.name}</div>
                  <div className="text-xs text-[#7A7368]">{hero.nation} • {hero.gender}</div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Mercenary Selector Modal */}
      {showMercenarySelector && (
        <div className="fixed inset-0 bg-black/65 z-[500] flex items-center justify-center" onClick={() => setShowMercenarySelector(false)}>
          <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg w-[700px] max-w-[90vw] p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">용병 선택</h3>
            
            <div className="mb-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#7A7368]" />
                <input
                  type="text"
                  placeholder="용병 이름 또는 카테고리 검색..."
                  value={mercenarySearch}
                  onChange={(e) => setMercenarySearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 border border-[#DDD6CA] rounded outline-none focus:border-[#8B6B4A]"
                />
              </div>
            </div>

            <div className="flex gap-2 mb-4 flex-wrap">
              {['전체', '전사', '마법사', '암살자', '지원', '힐러'].map((cat) => (
                <button
                  key={cat}
                  className="px-3 py-1 border border-[#DDD6CA] rounded text-sm hover:border-[#8B6B4A] hover:bg-[#8B6B4A] hover:text-[#E8DCCB] transition-colors"
                >
                  {cat}
                </button>
              ))}
            </div>

            <div className="grid grid-cols-2 gap-3 max-h-[50vh] overflow-y-auto">
              {filteredMercenaries.map((merc) => (
                <button
                  key={merc.id}
                  onClick={() => handleSelectMercenary(merc)}
                  className="p-4 border-2 border-[#DDD6CA] rounded-lg text-left hover:border-[#8B6B4A] transition-all"
                >
                  <div className="text-2xl mb-2">⚔️</div>
                  <div className="text-sm font-medium text-[#3E3A36] mb-1">{merc.name}</div>
                  <div className="text-xs text-[#7A7368]">{merc.category} • {merc.element}속성</div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Monster Selector Modal */}
      {showMonsterSelector && (
        <div className="fixed inset-0 bg-black/65 z-[500] flex items-center justify-center" onClick={() => setShowMonsterSelector(false)}>
          <div className="bg-[#FAF8F2] border border-[#DDD6CA] rounded-lg w-[600px] max-w-[90vw] p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="font-serif text-xl font-semibold text-[#3E3A36] mb-4">몬스터 선택</h3>
            <div className="grid grid-cols-2 gap-3 max-h-[60vh] overflow-y-auto">
              {sampleMonsters.map((monster) => (
                <button
                  key={monster.id}
                  onClick={() => {
                    setSelectedMonster(monster);
                    setShowMonsterSelector(false);
                  }}
                  className={`p-4 border-2 rounded-lg text-left transition-all ${
                    selectedMonster?.id === monster.id
                      ? 'border-[#8B6B4A] bg-[#8B6B4A]/10'
                      : 'border-[#DDD6CA] hover:border-[#8B6B4A]'
                  }`}
                >
                  <div className="text-2xl mb-2">🐉</div>
                  <div className="text-sm font-medium text-[#3E3A36] mb-1">{monster.name}</div>
                  <div className="text-xs text-[#7A7368]">{monster.element}속성 • 저항 {monster.resistance}%</div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
