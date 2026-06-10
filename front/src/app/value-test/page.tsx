'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  AlertTriangle,
  Calculator,
  History,
  Search,
  Skull,
  Sparkles,
  Trash2,
  X,
} from 'lucide-react';
import MercenaryCharacteristicPicker from '@/components/value-test/MercenaryCharacteristicPicker';
import SetPieceConfigurator from '@/components/value-test/SetPieceConfigurator';
import ValueTestResultPanel from '@/components/value-test/ValueTestResultPanel';
import {
  api,
  getToken,
  type DeckDetailDto,
  type DpsEvaluationSummaryDto,
  type DpsValueEvaluationResponseDto,
  type ItemSearchResult,
  type MercenaryCharacteristicSetupDto,
  type MercenaryDto,
  type MonsterDto,
  type ResistanceTypeDto,
  type RitualDto,
  type ScenarioItemTypeDto,
  type SetDetailDto,
  type SetSummaryDto,
  type UserDto,
} from '@/lib/api';
import { parseApiError } from '@/lib/parseApiError';
import {
  applyBundleKindToPieces,
  buildRitualMarkOptions,
  buildSetScenarioLines,
  initSetPieces,
  type RitualCountOption,
  type RitualMarkOption,
  type SetBundleKind,
  type SetPieceState,
  shouldSendSetLines,
  validateSetBundleSelection,
} from '@/lib/setTitle';
import {
  buildEvaluationRequest,
  CANDIDATE_TYPE_LABEL,
  formatEfficiencyPerEok,
  parseEokPrice,
  resolveResistanceType,
} from '@/lib/valueTest';
import {
  toCharacteristicPayload,
  validateCharacteristicSelections,
} from '@/lib/mercenaryCharacteristicUi';

type DeckSummary = { id: number; name: string; isActive: boolean };
type CandidateTab = ScenarioItemTypeDto;

const cardStyle = { background: 'var(--card)', border: '1px solid var(--border)' };
const inputStyle = {
  border: '1px solid var(--border)',
  background: 'white',
  color: 'var(--text)',
};

export default function ValueTestPage() {
  const router = useRouter();

  // ── 인증·프로필 ──
  const [me, setMe] = useState<UserDto | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

  // ── Step 1: 덱·몬스터 ──
  const [decks, setDecks] = useState<DeckSummary[]>([]);
  const [selectedDeckId, setSelectedDeckId] = useState<number | null>(null);
  const [deckDetail, setDeckDetail] = useState<DeckDetailDto | null>(null);
  const [deckLoading, setDeckLoading] = useState(false);
  const [monsters, setMonsters] = useState<MonsterDto[]>([]);
  const [monster, setMonster] = useState<MonsterDto | null>(null);
  const [monsterQuery, setMonsterQuery] = useState('');
  const [showMonsterSuggest, setShowMonsterSuggest] = useState(false);
  const [resistanceType, setResistanceType] = useState<ResistanceTypeDto>('MAGIC');
  const [allMercenaries, setAllMercenaries] = useState<MercenaryDto[]>([]);

  // ── Step 2: 후보 ──
  const [candidateTab, setCandidateTab] = useState<CandidateTab>('ITEM_SET');
  const [itemQuery, setItemQuery] = useState('');
  const [itemResults, setItemResults] = useState<ItemSearchResult[]>([]);
  const [selectedItem, setSelectedItem] = useState<ItemSearchResult | null>(null);
  const [itemDropOpen, setItemDropOpen] = useState(false);
  const itemSearchRef = useRef<HTMLDivElement>(null);

  const [setQuery, setSetQuery] = useState('');
  const [setResults, setSetResults] = useState<SetSummaryDto[]>([]);
  const [selectedSet, setSelectedSet] = useState<SetDetailDto | null>(null);
  const [setDropOpen, setSetDropOpen] = useState(false);
  const setSearchRef = useRef<HTMLDivElement>(null);
  const [setPieces, setSetPieces] = useState<SetPieceState[]>([]);
  const [bundleKind, setBundleKind] = useState<SetBundleKind>('FULL');
  const [ritualCount, setRitualCount] = useState<RitualCountOption>(0);
  const [ritualMark, setRitualMark] = useState<RitualMarkOption | null>(null);
  const [uniqueRituals, setUniqueRituals] = useState<RitualMarkOption[]>([]);

  const [mercQuery, setMercQuery] = useState('');
  const [mercResults, setMercResults] = useState<MercenaryDto[]>([]);
  const [selectedMerc, setSelectedMerc] = useState<MercenaryDto | null>(null);
  const [mercPriceEok, setMercPriceEok] = useState('');
  const [mercLevel, setMercLevel] = useState<250 | 260>(250);
  const [mercBonusTarget, setMercBonusTarget] = useState<'MAIN_STAT' | 'VITALITY'>('MAIN_STAT');
  const [mercBonusAmount, setMercBonusAmount] = useState('0');
  const [mercCharSetup, setMercCharSetup] = useState<MercenaryCharacteristicSetupDto | null>(null);
  const [mercCharSetupLoading, setMercCharSetupLoading] = useState(false);
  const [mercCharSelections, setMercCharSelections] = useState<Record<number, number>>({});

  // ── Step 3: 대상 용병·가격 override ──
  const [affectedMemberId, setAffectedMemberId] = useState<number | null>(null);
  const [priceOverrideEok, setPriceOverrideEok] = useState('');
  const [mercenaryModeOverride, setMercenaryModeOverride] = useState<'AUTO' | 'REPLACE' | 'APPEND'>('AUTO');

  // ── 실행·결과 ──
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<DpsValueEvaluationResponseDto | null>(null);

  // ── 평가 기록 ──
  const [showHistory, setShowHistory] = useState(false);
  const [history, setHistory] = useState<DpsEvaluationSummaryDto[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const deckFull = (deckDetail?.members.length ?? 0) >= 12;
  const autoMercMode = deckFull ? 'REPLACE' : 'APPEND';
  const effectiveMercMode =
    mercenaryModeOverride === 'AUTO'
      ? autoMercMode
      : mercenaryModeOverride;

  const needsServer = candidateTab !== 'MERCENARY';
  const serverMissing = needsServer && me != null && me.serverId == null && !me.server && !me.serverName;

  const filteredMonsters = useMemo(() => {
    const q = monsterQuery.trim().toLowerCase();
    if (!q) return monsters.slice(0, 12);
    return monsters.filter((m) => m.name.toLowerCase().includes(q)).slice(0, 12);
  }, [monsters, monsterQuery]);

  const calculateBlockers = useMemo(() => {
    const blockers: string[] = [];
    if (!selectedDeckId || !deckDetail?.members.length) {
      blockers.push('기준 덱을 선택해주세요');
    }
    if (!monster) {
      blockers.push('대상 몬스터를 선택해주세요');
    }
    if (serverMissing) {
      blockers.push('프로필에서 서버를 설정해주세요');
    }
    if (candidateTab === 'ITEM_SINGLE') {
      if (!selectedItem) blockers.push('단품 아이템을 선택해주세요');
      if (affectedMemberId == null) blockers.push('장착할 덱 용병을 선택해주세요');
    } else if (candidateTab === 'ITEM_SET') {
      if (!selectedSet) blockers.push('세트를 선택해주세요');
      if (affectedMemberId == null) blockers.push('장착할 덱 용병을 선택해주세요');
      const setErr = validateSetBundleSelection(bundleKind, ritualCount, ritualMark, setPieces);
      if (setErr) blockers.push(setErr);
    } else {
      if (!selectedMerc) blockers.push('후보 용병을 선택해주세요');
      if (parseEokPrice(mercPriceEok) <= 0) blockers.push('용병 가격을 입력해주세요');
      if (effectiveMercMode === 'REPLACE' && affectedMemberId == null) {
        blockers.push('교체할 덱 용병을 선택해주세요');
      }
      const charErr = validateCharacteristicSelections(mercCharSetup, mercCharSelections);
      if (charErr) blockers.push(charErr);
    }
    return blockers;
  }, [
    selectedDeckId, deckDetail, monster, serverMissing, candidateTab,
    selectedItem, selectedSet, affectedMemberId, setPieces, bundleKind, ritualCount, ritualMark,
    selectedMerc, mercPriceEok, effectiveMercMode, mercCharSetup, mercCharSelections,
  ]);

  const canCalculate = calculateBlockers.length === 0;

  // ── 초기 로드 ──
  useEffect(() => {
    if (!getToken()) {
      router.replace('/login');
      return;
    }
    setAuthChecked(true);
    Promise.all([
      api.getMe(),
      api.getDecks() as Promise<DeckSummary[]>,
      api.getMonsters(),
      api.getMercenaries({ limit: 500 }),
    ])
      .then(([user, deckList, monsterList, mercList]) => {
        setMe(user);
        setDecks(deckList);
        setMonsters(monsterList);
        setAllMercenaries(mercList);
        const active = deckList.find((d) => d.isActive) ?? deckList[0];
        if (active) setSelectedDeckId(active.id);
      })
      .catch(() => {});
  }, [router]);

  // 덱 상세 로드
  useEffect(() => {
    if (!selectedDeckId) {
      setDeckDetail(null);
      return;
    }
    setDeckLoading(true);
    api.getDeck(selectedDeckId)
      .then((d) => {
        const detail = d as DeckDetailDto;
        setDeckDetail(detail);
        setAffectedMemberId(null);
        const mercMap = new Map(allMercenaries.map((m) => [m.id, m]));
        setResistanceType(resolveResistanceType(detail, mercMap));
      })
      .catch(() => setDeckDetail(null))
      .finally(() => setDeckLoading(false));
  }, [selectedDeckId, allMercenaries]);

  // 단품 검색
  useEffect(() => {
    if (!itemQuery.trim() || selectedItem) { setItemResults([]); return; }
    const t = setTimeout(() => {
      api.searchItems(itemQuery, { type: 'EQUIPMENT', limit: 8 })
        .then(setItemResults)
        .catch(() => setItemResults([]));
    }, 250);
    return () => clearTimeout(t);
  }, [itemQuery, selectedItem]);

  // 세트 검색
  useEffect(() => {
    if (!setQuery.trim() || selectedSet) { setSetResults([]); return; }
    const t = setTimeout(() => {
      api.getSets(setQuery)
        .then((res) => setSetResults(res.content ?? []))
        .catch(() => setSetResults([]));
    }, 250);
    return () => clearTimeout(t);
  }, [setQuery, selectedSet]);

  // 용병 검색
  useEffect(() => {
    if (!mercQuery.trim() || selectedMerc) { setMercResults([]); return; }
    const t = setTimeout(() => {
      api.getMercenaries({ q: mercQuery, limit: 12 })
        .then(setMercResults)
        .catch(() => setMercResults([]));
    }, 250);
    return () => clearTimeout(t);
  }, [mercQuery, selectedMerc]);

  // 후보 용병 특성 정의 로드
  useEffect(() => {
    if (!selectedMerc) {
      setMercCharSetup(null);
      setMercCharSelections({});
      return;
    }
    setMercCharSelections({});
    setMercCharSetupLoading(true);
    api.getMercenaryCharacteristicSetup(selectedMerc.id)
      .then(setMercCharSetup)
      .catch(() => setMercCharSetup(null))
      .finally(() => setMercCharSetupLoading(false));
  }, [selectedMerc]);

  // 세트 선택 시 피스·주술 로드
  useEffect(() => {
    if (!selectedSet) {
      setSetPieces([]);
      setUniqueRituals([]);
      setRitualMark(null);
      setBundleKind('FULL');
      setRitualCount(0);
      return;
    }
    setRitualMark(null);
    setRitualCount(0);
    setBundleKind('FULL');
    Promise.all(
      selectedSet.pieces.map((p) =>
        api.getItemRituals(p.itemId).catch(() => [] as RitualDto[])
      )
    ).then((perPiece) => {
      const ritualMap = new Map(
        selectedSet.pieces.map((p, i) => [p.itemId, perPiece[i].length > 0])
      );
      const initial = initSetPieces(selectedSet.pieces, ritualMap);
      setSetPieces(applyBundleKindToPieces(initial, 'FULL', 0));
      setUniqueRituals(buildRitualMarkOptions(perPiece));
    });
  }, [selectedSet]);

  // 드롭다운 외부 클릭
  useEffect(() => {
    function close(e: MouseEvent) {
      if (itemSearchRef.current && !itemSearchRef.current.contains(e.target as Node)) setItemDropOpen(false);
      if (setSearchRef.current && !setSearchRef.current.contains(e.target as Node)) setSetDropOpen(false);
    }
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const loadHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      const page = await api.getMyDpsEvaluations(0, 20);
      setHistory(page.content ?? []);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    if (showHistory) loadHistory();
  }, [showHistory, loadHistory]);

  async function handleDeleteEvaluation(evaluationId: number) {
    if (!window.confirm('이 평가 기록을 삭제할까요?')) return;
    setDeletingId(evaluationId);
    try {
      await api.deleteMyDpsEvaluation(evaluationId);
      setHistory((prev) => prev.filter((h) => h.evaluationId !== evaluationId));
      if (result?.evaluationId === evaluationId) {
        setResult(null);
      }
    } catch (e) {
      setError(parseApiError(e));
    } finally {
      setDeletingId(null);
    }
  }

  async function handleCalculate() {
    if (!canCalculate || !selectedDeckId || !deckDetail || !monster) return;
    setError('');
    setCalculating(true);
    try {
      const priceOverrides: Record<string, number> = {};
      const overrideJeon = parseEokPrice(priceOverrideEok);
      if (overrideJeon > 0) {
        if (candidateTab === 'ITEM_SINGLE' && selectedItem) {
          priceOverrides[`ITEM:${selectedItem.id}`] = overrideJeon;
        } else if (candidateTab === 'ITEM_SET' && selectedSet) {
          priceOverrides[`SET:${selectedSet.id}`] = overrideJeon;
        }
      }

      let setLines: ReturnType<typeof buildSetScenarioLines> | undefined;
      if (candidateTab === 'ITEM_SET') {
        const pieceError = validateSetBundleSelection(bundleKind, ritualCount, ritualMark, setPieces);
        if (pieceError) {
          setError(pieceError);
          setCalculating(false);
          return;
        }
        if (shouldSendSetLines(setPieces, ritualMark)) {
          setLines = buildSetScenarioLines(setPieces, ritualMark);
        }
      }

      const body = buildEvaluationRequest({
        deckId: selectedDeckId,
        deck: deckDetail,
        monsterId: monster.id,
        resistanceType,
        candidateType: candidateTab,
        affectedMemberId,
        singleItemId: selectedItem?.id,
        setId: selectedSet?.id,
        setLines,
        priceOverrides: Object.keys(priceOverrides).length > 0 ? priceOverrides : undefined,
        mercenaryId: selectedMerc?.id,
        mercenaryMode: effectiveMercMode as 'REPLACE' | 'APPEND',
        mercenaryPrice: parseEokPrice(mercPriceEok),
        mercenaryLevel: mercLevel,
        mercenaryBonusTarget: mercBonusTarget,
        mercenaryBonusAmount: Number(mercBonusAmount) || 0,
        mercenaryCharacteristics: toCharacteristicPayload(mercCharSelections),
      });

      const res = await api.evaluateDpsValue(body);
      setResult(res);
      if (showHistory) loadHistory();
    } catch (e) {
      setError(parseApiError(e));
      setResult(null);
    } finally {
      setCalculating(false);
    }
  }

  if (!authChecked) {
    return (
      <div className="py-16 text-center text-sm" style={{ color: 'var(--text-muted)' }}>
        로딩 중…
      </div>
    );
  }

  return (
    <div className="py-8">
      {/* 헤더 */}
      <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <h1 className="font-serif text-2xl font-bold flex items-center gap-2" style={{ color: 'var(--text)' }}>
            <Calculator style={{ color: 'var(--brown)', width: 24, height: 24 }} />
            가성비테스트
          </h1>
          <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>
            저장된 덱을 기준으로 아이템·용병 후보의 DPS 증가분과 가격 대비 효율을 시뮬레이션합니다.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowHistory((v) => !v)}
          className="flex items-center gap-1.5 text-sm px-3 py-2 rounded"
          style={{ ...cardStyle, color: 'var(--text-muted)' }}
        >
          <History size={16} />
          내 평가 기록
        </button>
      </div>

      {/* 시뮬레이션 안내 */}
      <div
        className="flex items-start gap-2 px-4 py-3 rounded-lg mb-6 text-sm"
        style={{ background: 'var(--bg)', border: '1px dashed var(--brown)', color: 'var(--text-muted)' }}
      >
        <Sparkles size={16} style={{ color: 'var(--brown)', flexShrink: 0, marginTop: 2 }} />
        <span>
          <strong style={{ color: 'var(--brown)' }}>덱은 변경되지 않습니다.</strong>{' '}
          서버가 메모리 overlay로 장착·교체를 시뮬레이션한 뒤 before/after DPS를 계산합니다.
        </span>
      </div>

      {serverMissing && (
        <div
          className="flex items-start gap-2 px-4 py-3 rounded-lg mb-6 text-sm"
          style={{ background: '#FFF8F0', border: '1px solid var(--brown)', color: 'var(--text)' }}
        >
          <AlertTriangle size={16} style={{ color: 'var(--brown)', flexShrink: 0, marginTop: 2 }} />
          <span>
            아이템 시세 조회를 위해 프로필에서 서버를 설정해주세요.{' '}
            <Link href="/profile" style={{ color: 'var(--brown)' }} className="underline">
              프로필로 이동
            </Link>
          </span>
        </div>
      )}

      <div className="grid lg:grid-cols-5 gap-6">
        {/* 왼쪽: 입력 스텝 */}
        <div className="lg:col-span-3 space-y-5">

          {/* Step 1: 기준 덱 */}
          <section className="rounded-xl p-5" style={cardStyle}>
            <StepHeader step={1} title="기준 덱" />
            <div className="space-y-4">
              <div>
                <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>덱 선택</label>
                {decks.length === 0 ? (
                  <p className="text-sm">
                    저장된 덱이 없습니다.{' '}
                    <Link href="/deck" style={{ color: 'var(--brown)' }} className="underline">덱 설정</Link>
                    에서 덱을 만드세요.
                  </p>
                ) : (
                  <select
                    value={selectedDeckId ?? ''}
                    onChange={(e) => setSelectedDeckId(Number(e.target.value))}
                    className="w-full rounded px-3 py-2 text-sm"
                    style={inputStyle}
                  >
                    {decks.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}{d.isActive ? ' (활성)' : ''}
                      </option>
                    ))}
                  </select>
                )}
              </div>

              {deckLoading && (
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>덱 불러오는 중…</p>
              )}

              {deckDetail && deckDetail.members.length > 0 && (
                <div>
                  <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>
                    덱 구성 ({deckDetail.members.length}명) — 읽기 전용
                  </label>
                  <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                    {deckDetail.members.map((m) => (
                      <div
                        key={m.id}
                        className="rounded px-2 py-1.5 text-xs truncate"
                        style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                        title={m.mercenaryName}
                      >
                        {m.mercenaryName}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* Step 2: 대상 몬스터 */}
          <section
            className="rounded-xl p-5"
            style={{
              background: monster ? 'var(--card)' : 'var(--bg)',
              border: monster ? '1px solid var(--border)' : '2px dashed var(--brown)',
            }}
          >
            <div className="flex items-start justify-between gap-3 mb-4">
              <StepHeader step={2} title="대상 몬스터" className="mb-0" />
              {!monster && (
                <span
                  className="shrink-0 text-[10px] font-semibold px-2 py-0.5 rounded"
                  style={{ background: 'var(--brown)', color: 'var(--beige)' }}
                >
                  필수
                </span>
              )}
            </div>

            <p className="text-xs mb-4 -mt-2" style={{ color: 'var(--text-muted)' }}>
              DPS·가성비는 이 몬스터를 기준으로 계산됩니다. 몬스터를 선택해야 계산 버튼이 활성화됩니다.
            </p>

            {monster ? (
              <div
                className="rounded-lg px-4 py-3 mb-4 flex items-start gap-3"
                style={{ background: 'var(--bg)', border: '1px solid var(--brown)' }}
              >
                <Skull size={20} style={{ color: 'var(--brown)', flexShrink: 0, marginTop: 2 }} />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-semibold text-sm truncate" style={{ color: 'var(--text)' }}>
                      {monster.name}
                    </p>
                    <button
                      type="button"
                      onClick={() => { setMonster(null); setMonsterQuery(''); }}
                      className="text-xs shrink-0"
                      style={{ color: 'var(--text-muted)' }}
                    >
                      변경
                    </button>
                  </div>
                  <div className="flex flex-wrap gap-x-3 gap-y-1 mt-1.5 text-xs" style={{ color: 'var(--text-muted)' }}>
                    {monster.magicResistance != null && (
                      <span>마법저항 {monster.magicResistance}</span>
                    )}
                    {monster.hittingResistance != null && (
                      <span>타격저항 {monster.hittingResistance}</span>
                    )}
                    {monster.elementValue != null && (
                      <span>속성값 {monster.elementValue}</span>
                    )}
                    {monster.element && <span>속성 {monster.element}</span>}
                  </div>
                </div>
              </div>
            ) : (
              <div className="relative mb-4">
                <div className="relative">
                  <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--brown)' }} />
                  <input
                    value={monsterQuery}
                    onChange={(e) => {
                      setMonsterQuery(e.target.value);
                      setShowMonsterSuggest(true);
                    }}
                    onFocus={() => setShowMonsterSuggest(true)}
                    placeholder="몬스터 이름을 검색하세요 (예: 보스명)"
                    className="w-full rounded-lg pl-10 pr-3 py-3 text-sm"
                    style={{ ...inputStyle, border: '2px solid var(--brown)' }}
                  />
                </div>
                {showMonsterSuggest && filteredMonsters.length > 0 && (
                  <div
                    className="absolute z-20 w-full mt-1 rounded-lg shadow-lg max-h-52 overflow-y-auto"
                    style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
                  >
                    {filteredMonsters.map((m) => (
                      <button
                        key={m.id}
                        type="button"
                        onClick={() => { setMonster(m); setMonsterQuery(''); setShowMonsterSuggest(false); }}
                        className="block w-full text-left px-4 py-2.5 text-sm hover:bg-black/5 border-b last:border-b-0"
                        style={{ borderColor: 'var(--border)' }}
                      >
                        <span style={{ color: 'var(--text)' }}>{m.name}</span>
                        {m.magicResistance != null && (
                          <span className="ml-2 text-xs" style={{ color: 'var(--text-muted)' }}>
                            마저 {m.magicResistance}
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                )}
                {!showMonsterSuggest && !monsterQuery && monsters.length > 0 && (
                  <p className="text-xs mt-2" style={{ color: 'var(--text-disabled)' }}>
                    검색창을 눌러 몬스터 목록에서 선택하세요
                  </p>
                )}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-3 pt-1" style={{ borderTop: '1px solid var(--border)' }}>
              <label className="text-xs font-medium" style={{ color: 'var(--text-muted)' }}>저항 기준</label>
              <select
                value={resistanceType}
                onChange={(e) => setResistanceType(e.target.value as ResistanceTypeDto)}
                className="rounded px-2 py-1.5 text-sm"
                style={inputStyle}
              >
                <option value="MAGIC">마법저항</option>
                <option value="HITTING">타격저항</option>
              </select>
              <span className="text-xs" style={{ color: 'var(--text-disabled)' }}>
                덱 구성 기준 자동 추천됨
              </span>
            </div>
          </section>

          {/* Step 3 */}
          <section className="rounded-xl p-5" style={cardStyle}>
            <StepHeader step={3} title="후보 선택" />
            <div className="flex gap-1 mb-4">
              {(['ITEM_SET', 'ITEM_SINGLE', 'MERCENARY'] as CandidateTab[]).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => {
                    setCandidateTab(tab);
                    setResult(null);
                    setError('');
                    setAffectedMemberId(null);
                    if (tab !== 'MERCENARY') {
                      setMercCharSelections({});
                      setMercCharSetup(null);
                    }
                  }}
                  className="flex-1 py-2 text-xs rounded transition-colors"
                  style={{
                    background: candidateTab === tab ? 'var(--brown)' : 'var(--bg)',
                    color: candidateTab === tab ? 'var(--beige)' : 'var(--text-muted)',
                    border: `1px solid ${candidateTab === tab ? 'var(--brown)' : 'var(--border)'}`,
                  }}
                >
                  {CANDIDATE_TYPE_LABEL[tab]}
                </button>
              ))}
            </div>

            {candidateTab === 'ITEM_SINGLE' && (
              <div ref={itemSearchRef} className="relative">
                <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>장비 아이템 검색</label>
                <input
                  value={selectedItem ? selectedItem.name : itemQuery}
                  onChange={(e) => { setItemQuery(e.target.value); setSelectedItem(null); setItemDropOpen(true); }}
                  onFocus={() => setItemDropOpen(true)}
                  placeholder="아이템명 입력"
                  className="w-full rounded px-3 py-2 text-sm"
                  style={inputStyle}
                />
                {itemDropOpen && !selectedItem && itemResults.length > 0 && (
                  <div className="absolute z-20 w-full mt-1 rounded shadow-lg" style={{ background: 'var(--card)', border: '1px solid var(--border)' }}>
                    {itemResults.map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => { setSelectedItem(item); setItemQuery(''); setItemDropOpen(false); }}
                        className="block w-full text-left px-3 py-2 text-sm hover:bg-black/5"
                      >
                        {item.name}
                        {item.setName && <span className="text-xs ml-1" style={{ color: 'var(--text-muted)' }}>({item.setName})</span>}
                      </button>
                    ))}
                  </div>
                )}
                {selectedItem && (
                  <p className="text-xs mt-1" style={{ color: 'var(--brown)' }}>선택: {selectedItem.name}</p>
                )}
              </div>
            )}

            {candidateTab === 'ITEM_SET' && (
              <div className="space-y-3">
                <div ref={setSearchRef} className="relative">
                  <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>세트 검색</label>
                  <input
                    value={selectedSet ? selectedSet.name : setQuery}
                    onChange={(e) => { setSetQuery(e.target.value); setSelectedSet(null); setSetDropOpen(true); }}
                    onFocus={() => setSetDropOpen(true)}
                    placeholder="세트명 입력"
                    className="w-full rounded px-3 py-2 text-sm"
                    style={inputStyle}
                  />
                  {setDropOpen && !selectedSet && setResults.length > 0 && (
                    <div className="absolute z-20 w-full mt-1 rounded shadow-lg" style={{ background: 'var(--card)', border: '1px solid var(--border)' }}>
                      {setResults.map((s) => (
                        <button
                          key={s.id}
                          type="button"
                          onClick={() => {
                            api.getSet(s.id).then(setSelectedSet).catch(() => {});
                            setSetQuery('');
                            setSetDropOpen(false);
                          }}
                          className="block w-full text-left px-3 py-2 text-sm hover:bg-black/5"
                        >
                          {s.name} ({s.totalPieces}피스)
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                {selectedSet && setPieces.length > 0 && (
                  <SetPieceConfigurator
                    setName={selectedSet.name}
                    pieces={setPieces}
                    uniqueRituals={uniqueRituals}
                    bundleKind={bundleKind}
                    ritualCount={ritualCount}
                    ritualMark={ritualMark}
                    onBundleKindChange={setBundleKind}
                    onRitualCountChange={setRitualCount}
                    onRitualMarkChange={setRitualMark}
                    onPiecesChange={setSetPieces}
                  />
                )}
              </div>
            )}

            {candidateTab === 'MERCENARY' && (
              <div className="space-y-3">
                <div className="relative">
                  <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>용병 검색</label>
                  <input
                    value={selectedMerc ? selectedMerc.name : mercQuery}
                    onChange={(e) => { setMercQuery(e.target.value); setSelectedMerc(null); }}
                    placeholder="용병명 입력"
                    className="w-full rounded px-3 py-2 text-sm"
                    style={inputStyle}
                  />
                  {!selectedMerc && mercResults.length > 0 && mercQuery && (
                    <div className="absolute z-20 w-full mt-1 rounded shadow-lg" style={{ background: 'var(--card)', border: '1px solid var(--border)' }}>
                      {mercResults.map((m) => (
                        <button
                          key={m.id}
                          type="button"
                          onClick={() => { setSelectedMerc(m); setMercQuery(''); }}
                          className="block w-full text-left px-3 py-2 text-sm hover:bg-black/5"
                        >
                          {m.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                <div>
                  <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>용병 가격 (억, 필수)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.1"
                    value={mercPriceEok}
                    onChange={(e) => setMercPriceEok(e.target.value)}
                    placeholder="예: 30"
                    className="w-full rounded px-3 py-2 text-sm"
                    style={inputStyle}
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs mb-1" style={{ color: 'var(--text-muted)' }}>레벨</label>
                    <select value={mercLevel} onChange={(e) => setMercLevel(Number(e.target.value) as 250 | 260)} className="w-full rounded px-2 py-1.5 text-sm" style={inputStyle}>
                      <option value={250}>250</option>
                      <option value={260}>260</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs mb-1" style={{ color: 'var(--text-muted)' }}>보너스 스탯</label>
                    <select value={mercBonusTarget} onChange={(e) => setMercBonusTarget(e.target.value as 'MAIN_STAT' | 'VITALITY')} className="w-full rounded px-2 py-1.5 text-sm" style={inputStyle}>
                      <option value="MAIN_STAT">주스탯</option>
                      <option value="VITALITY">생명력</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-xs mb-1" style={{ color: 'var(--text-muted)' }}>보너스 스탯량</label>
                  <input type="number" value={mercBonusAmount} onChange={(e) => setMercBonusAmount(e.target.value)} className="w-full rounded px-3 py-2 text-sm" style={inputStyle} />
                </div>
                <div>
                  <label className="block text-xs mb-1" style={{ color: 'var(--text-muted)' }}>편성 방식</label>
                  <select
                    value={mercenaryModeOverride}
                    onChange={(e) => setMercenaryModeOverride(e.target.value as 'AUTO' | 'REPLACE' | 'APPEND')}
                    className="w-full rounded px-2 py-1.5 text-sm"
                    style={inputStyle}
                  >
                    <option value="AUTO">자동 ({deckFull ? '교체 (덱 12명)' : '추가 (빈 슬롯)'})</option>
                    <option value="REPLACE">교체 (REPLACE)</option>
                    <option value="APPEND">추가 (APPEND)</option>
                  </select>
                </div>

                {selectedMerc && (
                  <div
                    className="rounded-lg p-4"
                    style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}
                  >
                    <MercenaryCharacteristicPicker
                      setup={mercCharSetup}
                      loading={mercCharSetupLoading}
                      selections={mercCharSelections}
                      onSelectionsChange={setMercCharSelections}
                    />
                  </div>
                )}
              </div>
            )}
          </section>

          {/* Step 4 */}
          <section className="rounded-xl p-5" style={cardStyle}>
            <StepHeader step={4} title="대상 용병 · 옵션" />
            {candidateTab !== 'MERCENARY' || effectiveMercMode === 'REPLACE' ? (
              <div>
                <label className="block text-xs mb-2" style={{ color: 'var(--text-muted)' }}>
                  {candidateTab === 'MERCENARY' ? '교체할 덱 용병 선택' : '아이템을 장착할 용병 선택'}
                </label>
                {!deckDetail?.members.length ? (
                  <p className="text-sm" style={{ color: 'var(--text-muted)' }}>덱에 용병이 없습니다.</p>
                ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                    {deckDetail.members.map((m) => {
                      const selected = affectedMemberId === m.id;
                      return (
                        <button
                          key={m.id}
                          type="button"
                          onClick={() => setAffectedMemberId(m.id)}
                          className="rounded-lg p-3 text-left text-sm transition-all"
                          style={{
                            background: selected ? 'var(--bg)' : 'transparent',
                            border: selected ? '2px solid var(--brown)' : '1px dashed var(--border)',
                          }}
                        >
                          <div className="font-medium truncate">{m.mercenaryName}</div>
                          <div className="text-xs mt-0.5 truncate" style={{ color: 'var(--text-muted)' }}>
                            {m.slots.length > 0 ? `${m.slots.length}개 장비` : '장비 없음'}
                          </div>
                          {selected && (
                            <span className="text-[10px] mt-1 inline-block px-1.5 py-0.5 rounded" style={{ background: 'var(--brown)', color: 'var(--beige)' }}>
                              SIM
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            ) : (
              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
                빈 슬롯에 용병이 자동 추가됩니다 (APPEND).
              </p>
            )}

            {candidateTab !== 'MERCENARY' && (
              <div className="mt-4">
                <label className="block text-xs mb-1.5" style={{ color: 'var(--text-muted)' }}>
                  가격 직접 입력 (억, 선택) — 미입력 시 서버 시세 사용
                </label>
                <input
                  type="number"
                  min="0"
                  step="0.1"
                  value={priceOverrideEok}
                  onChange={(e) => setPriceOverrideEok(e.target.value)}
                  placeholder="예: 15"
                  className="w-full rounded px-3 py-2 text-sm"
                  style={inputStyle}
                />
              </div>
            )}
          </section>

          {/* 실행 */}
          <div className="rounded-xl p-4 space-y-3" style={cardStyle}>
            <div className="flex flex-wrap items-center gap-4">
              <button
                type="button"
                disabled={!canCalculate || calculating}
                onClick={handleCalculate}
                className="flex items-center gap-2 px-6 py-3 rounded-lg text-sm font-medium transition-opacity disabled:opacity-40"
                style={{ background: 'var(--brown)', color: 'var(--beige)' }}
              >
                <Calculator size={18} />
                {calculating ? '계산 중…' : '가성비 계산'}
              </button>
            </div>
            {!canCalculate && !calculating && calculateBlockers.length > 0 && (
              <div className="text-xs space-y-1" style={{ color: 'var(--text-muted)' }}>
                <p className="font-medium" style={{ color: 'var(--brown)' }}>아직 선택이 필요합니다</p>
                <ul className="list-disc list-inside space-y-0.5">
                  {calculateBlockers.map((msg) => (
                    <li key={msg}>{msg}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          {error && (
            <div className="px-4 py-3 rounded-lg text-sm" style={{ background: '#FFF0F0', color: 'var(--danger)', border: '1px solid var(--danger)' }}>
              {error}
            </div>
          )}
        </div>

        {/* 오른쪽: 결과 패널 (sticky) */}
        <div className="lg:col-span-2">
          <div className="lg:sticky lg:top-24 space-y-4">
            {result ? (
              <ValueTestResultPanel result={result} loading={calculating} />
            ) : (
              <div
                className="rounded-xl p-8 text-center"
                style={{ ...cardStyle, color: 'var(--text-muted)' }}
              >
                <Calculator size={32} className="mx-auto mb-3 opacity-30" />
                <p className="text-sm">덱 · <strong>몬스터</strong> · 후보 · 대상 용병을</p>
                <p className="text-sm">선택한 뒤</p>
                <p className="text-sm font-medium" style={{ color: 'var(--brown)' }}>가성비 계산</p>
                <p className="text-sm">버튼을 눌러주세요.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 평가 기록 드로어 */}
      {showHistory && (
        <div className="fixed inset-0 z-[300] flex justify-end">
          <div className="absolute inset-0 bg-black/40" onClick={() => setShowHistory(false)} />
          <div
            className="relative w-full max-w-md h-full overflow-y-auto p-5"
            style={{ background: 'var(--card)' }}
          >
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-serif text-lg font-semibold">내 평가 기록</h2>
              <button type="button" onClick={() => setShowHistory(false)} style={{ color: 'var(--text-muted)' }}>
                <X size={20} />
              </button>
            </div>
            {historyLoading ? (
              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>불러오는 중…</p>
            ) : history.length === 0 ? (
              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>저장된 평가가 없습니다.</p>
            ) : (
              <div className="space-y-2">
                {history.map((h) => (
                  <div key={h.evaluationId} className="rounded-lg p-3 text-sm" style={{ background: 'var(--bg)', border: '1px solid var(--border)' }}>
                    <div className="flex justify-between items-start gap-2">
                      <span className="font-medium">{h.candidateLabel || CANDIDATE_TYPE_LABEL[h.candidateType]}</span>
                      <div className="flex items-center gap-2 shrink-0">
                        <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
                          {new Date(h.createdAt).toLocaleDateString('ko-KR')}
                        </span>
                        <button
                          type="button"
                          title="삭제"
                          disabled={deletingId === h.evaluationId}
                          onClick={() => handleDeleteEvaluation(h.evaluationId)}
                          className="p-1 rounded transition-opacity disabled:opacity-40"
                          style={{ color: 'var(--text-muted)' }}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                    <div className="text-xs mt-1" style={{ color: 'var(--text-muted)' }}>
                      {h.monsterName} · +{h.finalDpsIncreaseRate.toFixed(2)}%
                    </div>
                    <div className="flex justify-between mt-1">
                      <span className="font-serif font-bold" style={{ color: 'var(--brown)' }}>
                        {h.formattedPrice ?? '—'}
                      </span>
                      <span style={{ color: 'var(--brown)' }}>
                        {formatEfficiencyPerEok(h.efficiencyPerEokFinal)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function StepHeader({ step, title, className = 'mb-4' }: { step: number; title: string; className?: string }) {
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <span
        className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold"
        style={{ background: 'var(--brown)', color: 'var(--beige)' }}
      >
        {step}
      </span>
      <h2 className="font-semibold text-sm" style={{ color: 'var(--text)' }}>{title}</h2>
    </div>
  );
}
