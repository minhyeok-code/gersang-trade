'use client';

import { useEffect, useMemo, useState } from 'react';
import { Settings, X } from 'lucide-react';
import type {
  DeckEffectCatalogDto,
  DeckEffectDto,
  DeckEffectSpiritDto,
  DeckEffectUpdateBody,
  DeckBuffSourceDto,
  DeckEffectBuffDto,
} from '@/lib/api';

const ELEMENT_LABELS: Record<string, string> = {
  FIRE: '화', WATER: '수', THUNDER: '뇌', WIND: '풍', EARTH: '토',
  ADAPTIVE: '무',
};

const ELEMENT_COLORS: Record<string, string> = {
  FIRE: '#DC2626',
  WATER: '#2563EB',
  THUNDER: '#D97706',
  WIND: '#16A34A',
  EARTH: '#92400E',
};

const STAT_LABEL: Record<string, string> = {
  ELEMENT_VALUE: '속성값',
  ELEMENT_PIERCE: '속성깎',
  RESIST_PIERCE: '저항깎',
  MIN_POWER: '최소공격력',
  MAX_POWER: '최대공격력',
  ATTACK_POWER: '공격력',
  STRENGTH: '힘',
  VITALITY: '생명력',
  DEXTERITY: '민첩',
  INTELLECT: '지력',
  DEFENSE: '방어',
  SIGHT: '시야',
  HIT_RATE: '명중률',
  CRITICAL_CHANCE: '크리티컬확률',
  MAGIC_RESISTANCE: '마법저항',
  HITTING_RESISTANCE: '타격저항',
  DAMAGE_PERCENT: '데미지증가',
  SKILL_DAMAGE_PERCENT: '스킬데미지증가',
  FIELD_MOVE_SPEED: '필드이동속도',
  ALL_STAT: '모든능력치',
  CRITICAL_RATE: '치명타확률',
  CRITICAL_DAMAGE: '치명타피해',
  MIN_DAMAGE: '최소데미지',
  MAX_DAMAGE: '최대데미지',
  ATTACK_SPEED: '공격속도',
  MOVE_SPEED: '이동속도',
  HP_RECOVERY: '체력회복',
  MP_RECOVERY: '마력회복',
  DAMAGE_PERCENT_GROUND: '지상데미지증가',
  DAMAGE_PERCENT_AIR: '공중데미지증가',
  STUN_DURATION: '기절시간',
  SKILL_RANGE: '사거리',
};

const SPIRIT_NATURE_ORDER = ['FIRE', 'WATER', 'THUNDER', 'WIND', 'EARTH'] as const;
const SPIRIT_GRADE_ORDER = ['LOWER', 'MIDDLE', 'UPPER', 'HIGHEST', 'LEGEND'] as const;
const SPIRIT_GRADE_LABELS: Record<string, string> = {
  LOWER: '하급',
  MIDDLE: '중급',
  UPPER: '상급',
  HIGHEST: '최상급',
  LEGEND: '전설',
};

const JINBEOP_ELEMENT_ORDER = ['FIRE', 'WIND', 'THUNDER', 'WATER'] as const;

type EffectCategory = 'spirit' | 'jinbeop' | 'gaho' | 'gongmyung';

const EFFECT_CATEGORIES: { id: EffectCategory; label: string; ready: boolean }[] = [
  { id: 'spirit', label: '정령', ready: true },
  { id: 'jinbeop', label: '진법', ready: true },
  { id: 'gaho', label: '가호', ready: false },
  { id: 'gongmyung', label: '공명', ready: false },
];

type DraftEffects = {
  spirit1Id: number | null;
  spirit2Id: number | null;
  jinbeopSourceId: number | null;
  cheungjinSourceId: number | null;
};

function formatBuffLine(buff: DeckEffectBuffDto): string {
  const stat = STAT_LABEL[buff.statType] ?? buff.statType;
  const el = buff.element && !['NONE', 'ADAPTIVE'].includes(buff.element)
    ? ELEMENT_LABELS[buff.element] ?? buff.element
    : '';
  const sign = buff.value > 0 ? '+' : '';
  return el ? `${el}${stat} ${sign}${buff.value}` : `${stat} ${sign}${buff.value}`;
}

function jinbeopMetaFromSource(source: DeckBuffSourceDto): { element: string; level: number } | null {
  const sid = source.sourceId;
  if (sid >= 1 && sid <= 10) return { element: 'WIND', level: sid };
  if (sid >= 11 && sid <= 20) return { element: 'THUNDER', level: sid - 10 };
  if (sid >= 21 && sid <= 30) return { element: 'WATER', level: sid - 20 };
  if (sid >= 31 && sid <= 40) return { element: 'FIRE', level: sid - 30 };
  const match = source.name.match(/(\d+)강/);
  const buff = source.buffs[0];
  if (buff?.element && match) {
    return { element: buff.element, level: Number(match[1]) };
  }
  return null;
}

function findJinbeopSource(
  jinbeops: DeckBuffSourceDto[],
  element: string,
  level: number,
): DeckBuffSourceDto | undefined {
  return jinbeops.find((source) => {
    const meta = jinbeopMetaFromSource(source);
    return meta?.element === element && meta.level === level;
  });
}

function spiritsByNatureGrade(spirits: DeckEffectSpiritDto[]) {
  const map = new Map<string, Map<string, DeckEffectSpiritDto[]>>();
  for (const spirit of spirits) {
    const nature = spirit.nature ?? 'NONE';
    const grade = spirit.grade ?? 'LOWER';
    if (!map.has(nature)) map.set(nature, new Map());
    const gradeMap = map.get(nature)!;
    if (!gradeMap.has(grade)) gradeMap.set(grade, []);
    gradeMap.get(grade)!.push(spirit);
  }
  return map;
}

function draftFromEffects(effects?: DeckEffectDto | null, cheungjinId?: number | null): DraftEffects {
  return {
    spirit1Id: effects?.spirits?.[0]?.id ?? null,
    spirit2Id: effects?.spirits?.[1]?.id ?? null,
    jinbeopSourceId: effects?.jinbeop?.id ?? null,
    cheungjinSourceId: cheungjinId ?? effects?.cheungjin?.id ?? null,
  };
}

function SpiritPortrait({ spirit, emptyLabel }: { spirit?: DeckEffectSpiritDto | null; emptyLabel: string }) {
  const nature = spirit?.nature;
  const color = nature ? (ELEMENT_COLORS[nature] ?? 'var(--brown)') : 'var(--border)';
  return (
    <div className="flex flex-col items-center gap-1 min-w-0 flex-1">
      <p className="text-[10px] font-medium truncate w-full text-center" style={{ color: 'var(--text-muted)' }}>
        {emptyLabel}
      </p>
      <div
        className="w-full aspect-square max-w-[72px] rounded-lg flex flex-col items-center justify-center p-1 text-center"
        style={{
          background: spirit ? `${color}22` : 'var(--bg)',
          border: `1px solid ${spirit ? color : 'var(--border)'}`,
        }}
      >
        {spirit ? (
          <>
            <span className="text-lg font-bold leading-none" style={{ color }}>
              {ELEMENT_LABELS[nature ?? ''] ?? '?'}
            </span>
            <span className="text-[9px] mt-1 leading-tight line-clamp-2" style={{ color: 'var(--text)' }}>
              {spirit.displayLabel || spirit.name}
            </span>
          </>
        ) : (
          <span className="text-[10px]" style={{ color: 'var(--text-disabled)' }}>미설정</span>
        )}
      </div>
    </div>
  );
}

function DeckEffectSettingsModal({
  open,
  catalog,
  initialDraft,
  saving,
  onClose,
  onSave,
}: {
  open: boolean;
  catalog: DeckEffectCatalogDto;
  initialDraft: DraftEffects;
  saving: boolean;
  onClose: () => void;
  onSave: (body: DeckEffectUpdateBody) => void;
}) {
  const [category, setCategory] = useState<EffectCategory>('spirit');
  const [draft, setDraft] = useState<DraftEffects>(initialDraft);
  const [activeSpiritSlot, setActiveSpiritSlot] = useState<0 | 1>(0);
  const [jinbeopElement, setJinbeopElement] = useState<string>('FIRE');
  const [jinbeopLevel, setJinbeopLevel] = useState(1);

  useEffect(() => {
    if (!open) return;
    setDraft(initialDraft);
    setCategory('spirit');
    setActiveSpiritSlot(0);
    const current = initialDraft.jinbeopSourceId
      ? catalog.jinbeops.find((j) => j.id === initialDraft.jinbeopSourceId)
      : null;
    if (current) {
      const meta = jinbeopMetaFromSource(current);
      if (meta) {
        setJinbeopElement(meta.element);
        setJinbeopLevel(meta.level);
      }
    } else {
      setJinbeopElement('FIRE');
      setJinbeopLevel(1);
    }
  }, [open, initialDraft, catalog.jinbeops]);

  const spiritMap = useMemo(() => spiritsByNatureGrade(catalog.spirits), [catalog.spirits]);
  const spiritById = useMemo(
    () => new Map(catalog.spirits.map((s) => [s.id, s])),
    [catalog.spirits],
  );

  const draftSpirit1 = draft.spirit1Id ? spiritById.get(draft.spirit1Id) : null;
  const draftSpirit2 = draft.spirit2Id ? spiritById.get(draft.spirit2Id) : null;

  function otherSpiritNature(slot: 0 | 1): string | null {
    const other = slot === 0 ? draftSpirit2 : draftSpirit1;
    return other?.nature ?? null;
  }

  function selectSpirit(spirit: DeckEffectSpiritDto) {
    const slot = activeSpiritSlot;
    const currentId = slot === 0 ? draft.spirit1Id : draft.spirit2Id;
    if (currentId === spirit.id) {
      setDraft((prev) => ({
        ...prev,
        [slot === 0 ? 'spirit1Id' : 'spirit2Id']: null,
      }));
      return;
    }
    const blockedNature = otherSpiritNature(slot);
    if (blockedNature && blockedNature === spirit.nature) {
      alert('같은 속성 정령은 1개만 선택할 수 있습니다.');
      return;
    }
    setDraft((prev) => ({
      ...prev,
      [slot === 0 ? 'spirit1Id' : 'spirit2Id']: spirit.id,
    }));
  }

  function applyJinbeop(element: string, level: number) {
    setJinbeopElement(element);
    setJinbeopLevel(level);
    const source = findJinbeopSource(catalog.jinbeops, element, level);
    setDraft((prev) => ({ ...prev, jinbeopSourceId: source?.id ?? null }));
  }

  function handleConfirm() {
    if (draft.spirit1Id && draft.spirit2Id && draft.spirit1Id === draft.spirit2Id) {
      alert('같은 정령은 중복 적용할 수 없습니다.');
      return;
    }
    const s1 = draft.spirit1Id ? spiritById.get(draft.spirit1Id) : null;
    const s2 = draft.spirit2Id ? spiritById.get(draft.spirit2Id) : null;
    if (s1 && s2 && s1.nature === s2.nature) {
      alert('같은 속성 정령은 1개만 선택할 수 있습니다.');
      return;
    }
    onSave({
      spirit1Id: draft.spirit1Id,
      spirit2Id: draft.spirit2Id,
      jinbeopSourceId: draft.jinbeopSourceId,
      cheungjinSourceId: draft.cheungjinSourceId,
    });
  }

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-[500] p-4"
      style={{ background: 'rgba(0,0,0,0.65)' }}
      onClick={onClose}
    >
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)', width: 'min(640px, 100%)', maxHeight: '86vh' }}
        className="rounded-xl flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-3 border-b" style={{ borderColor: 'var(--border)' }}>
          <h2 className="font-semibold" style={{ color: 'var(--text)' }}>덱 효과 설정</h2>
          <button type="button" onClick={onClose} style={{ color: 'var(--text-muted)' }} className="hover:text-[var(--text)]">
            <X style={{ width: 18, height: 18 }} />
          </button>
        </div>

        <div className="px-5 pt-3 flex flex-wrap gap-1.5">
          {EFFECT_CATEGORIES.map((cat) => {
            const active = category === cat.id;
            return (
              <button
                key={cat.id}
                type="button"
                onClick={() => setCategory(cat.id)}
                style={{
                  background: active ? 'var(--brown)' : 'var(--bg)',
                  color: active ? 'var(--beige)' : 'var(--text-muted)',
                  border: `1px solid ${active ? 'var(--brown)' : 'var(--border)'}`,
                }}
                className="rounded-lg px-3 py-1.5 text-sm font-medium transition-colors"
              >
                {cat.label}
                {!cat.ready && (
                  <span className="text-[10px] ml-1 opacity-70">(준비중)</span>
                )}
              </button>
            );
          })}
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4 min-h-0">
          {category === 'spirit' && (
            <div className="space-y-4">
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                속성별 1개씩, 최대 2개까지 선택할 수 있습니다.
              </p>
              <div className="flex gap-2">
                {([0, 1] as const).map((slot) => {
                  const active = activeSpiritSlot === slot;
                  const spirit = slot === 0 ? draftSpirit1 : draftSpirit2;
                  return (
                    <button
                      key={slot}
                      type="button"
                      onClick={() => setActiveSpiritSlot(slot)}
                      style={{
                        border: `1px solid ${active ? 'var(--brown)' : 'var(--border)'}`,
                        background: active ? 'var(--beige)' : 'var(--bg)',
                      }}
                      className="flex-1 rounded-lg p-2 text-left transition-colors"
                    >
                      <p className="text-[10px] mb-1" style={{ color: 'var(--text-muted)' }}>
                        {slot === 0 ? '주정령' : '보조정령'}
                      </p>
                      <p className="text-xs font-medium truncate" style={{ color: 'var(--text)' }}>
                        {spirit?.displayLabel ?? '선택 안 함'}
                      </p>
                    </button>
                  );
                })}
              </div>

              {SPIRIT_NATURE_ORDER.map((nature) => {
                const gradeMap = spiritMap.get(nature);
                if (!gradeMap || gradeMap.size === 0) return null;
                return (
                  <div key={nature}>
                    <p className="text-xs font-semibold mb-2" style={{ color: ELEMENT_COLORS[nature] ?? 'var(--text)' }}>
                      {ELEMENT_LABELS[nature]} 속성
                    </p>
                    <div className="space-y-2">
                      {SPIRIT_GRADE_ORDER.map((grade) => {
                        const list = gradeMap.get(grade);
                        if (!list?.length) return null;
                        return (
                          <div key={grade}>
                            <p className="text-[10px] mb-1" style={{ color: 'var(--text-muted)' }}>
                              {SPIRIT_GRADE_LABELS[grade] ?? grade}
                            </p>
                            <div className="flex flex-wrap gap-1.5">
                              {list.map((spirit) => {
                                const selected = draft.spirit1Id === spirit.id || draft.spirit2Id === spirit.id;
                                const blockedNature = otherSpiritNature(activeSpiritSlot);
                                const blocked = !selected && blockedNature === spirit.nature;
                                return (
                                  <button
                                    key={spirit.id}
                                    type="button"
                                    disabled={blocked}
                                    onClick={() => selectSpirit(spirit)}
                                    style={{
                                      background: selected ? 'var(--brown)' : 'var(--bg)',
                                      color: selected ? 'var(--beige)' : 'var(--text)',
                                      border: `1px solid ${selected ? 'var(--brown)' : 'var(--border)'}`,
                                      opacity: blocked ? 0.4 : 1,
                                    }}
                                    className="rounded px-2 py-1 text-[11px] hover:border-[var(--brown)] transition-colors disabled:cursor-not-allowed"
                                    title={spirit.acquireCondition ?? undefined}
                                  >
                                    {spirit.displayLabel || spirit.name}
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {category === 'jinbeop' && (
            <div className="space-y-4">
              <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                속성 1개와 1~10강 중 1개만 선택할 수 있습니다.
              </p>
              <div>
                <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>속성</p>
                <div className="flex flex-wrap gap-1.5">
                  {JINBEOP_ELEMENT_ORDER.map((el) => {
                    const active = jinbeopElement === el;
                    return (
                      <button
                        key={el}
                        type="button"
                        onClick={() => applyJinbeop(el, jinbeopLevel)}
                        style={{
                          background: active ? 'var(--brown)' : 'var(--bg)',
                          color: active ? 'var(--beige)' : 'var(--text)',
                          border: `1px solid ${active ? 'var(--brown)' : 'var(--border)'}`,
                        }}
                        className="rounded-lg px-4 py-1.5 text-sm font-medium min-w-[52px]"
                      >
                        {ELEMENT_LABELS[el]}
                      </button>
                    );
                  })}
                </div>
              </div>
              <div>
                <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>강화</p>
                <div className="grid grid-cols-5 gap-1.5">
                  {Array.from({ length: 10 }, (_, i) => i + 1).map((level) => {
                    const active = jinbeopLevel === level;
                    return (
                      <button
                        key={level}
                        type="button"
                        onClick={() => applyJinbeop(jinbeopElement, level)}
                        style={{
                          background: active ? 'var(--brown)' : 'var(--bg)',
                          color: active ? 'var(--beige)' : 'var(--text-muted)',
                          border: `1px solid ${active ? 'var(--brown)' : 'var(--border)'}`,
                        }}
                        className="rounded py-1.5 text-xs font-medium"
                      >
                        {level}강
                      </button>
                    );
                  })}
                </div>
              </div>
              {draft.jinbeopSourceId && (
                <div style={{ background: 'var(--bg)', border: '1px solid var(--border)' }} className="rounded-lg p-3">
                  <p className="text-xs font-medium mb-1" style={{ color: 'var(--text)' }}>
                    {catalog.jinbeops.find((j) => j.id === draft.jinbeopSourceId)?.name ?? '진법'}
                  </p>
                  <ul className="space-y-0.5">
                    {(catalog.jinbeops.find((j) => j.id === draft.jinbeopSourceId)?.buffs ?? []).map((buff, i) => (
                      <li key={i} className="text-[11px]" style={{ color: 'var(--text-muted)' }}>
                        · {formatBuffLine(buff)}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {(category === 'gaho' || category === 'gongmyung') && (
            <div className="text-center py-12">
              <p className="text-sm font-medium mb-1" style={{ color: 'var(--text)' }}>
                {EFFECT_CATEGORIES.find((c) => c.id === category)?.label} 설정
              </p>
              <p className="text-xs" style={{ color: 'var(--text-disabled)' }}>
                추후 업데이트 예정입니다.
              </p>
            </div>
          )}
        </div>

        <div className="flex gap-2 px-5 py-3 border-t" style={{ borderColor: 'var(--border)' }}>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
            className="flex-1 py-2 rounded text-sm disabled:opacity-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={saving}
            style={{ background: 'var(--brown)', color: 'var(--beige)' }}
            className="flex-1 py-2 rounded text-sm font-semibold hover:bg-[var(--brown-dark)] disabled:opacity-50"
          >
            {saving ? '저장 중...' : '확인'}
          </button>
        </div>
      </div>
    </div>
  );
}

export function DeckEffectPanel({
  selectedDeckId,
  effects,
  catalog,
  loading,
  saving,
  onSave,
}: {
  selectedDeckId: number | null;
  effects?: DeckEffectDto | null;
  catalog: DeckEffectCatalogDto | null;
  loading: boolean;
  saving: boolean;
  onSave: (body: DeckEffectUpdateBody) => Promise<void>;
}) {
  const [modalOpen, setModalOpen] = useState(false);

  const spirit1 = effects?.spirits?.[0] ?? null;
  const spirit2 = effects?.spirits?.[1] ?? null;
  const jinbeop = effects?.jinbeop ?? null;

  const spiritBuffLines = useMemo(() => {
    const lines: string[] = [];
    for (const spirit of [spirit1, spirit2]) {
      if (!spirit) continue;
      for (const buff of spirit.buffs) {
        lines.push(formatBuffLine(buff));
      }
    }
    return lines;
  }, [spirit1, spirit2]);

  const jinbeopBuffLines = useMemo(
    () => (jinbeop?.buffs ?? []).map(formatBuffLine),
    [jinbeop],
  );

  async function handleSave(body: DeckEffectUpdateBody) {
    await onSave(body);
    setModalOpen(false);
  }

  if (!selectedDeckId) {
    return (
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4">
        <p className="text-sm text-center py-6" style={{ color: 'var(--text-disabled)' }}>덱을 먼저 선택해 주세요</p>
      </div>
    );
  }

  return (
    <>
      <div style={{ background: 'var(--card)', border: '1px solid var(--border)' }} className="rounded-xl p-4 space-y-4">
        <div className="flex items-center justify-between gap-2">
          <h2 className="font-semibold text-sm" style={{ color: 'var(--text)' }}>덱효과</h2>
          <button
            type="button"
            onClick={() => setModalOpen(true)}
            disabled={!catalog || loading}
            style={{ border: '1px solid var(--border)', color: 'var(--brown)' }}
            className="flex items-center gap-1 rounded px-2 py-1 text-xs font-medium hover:border-[var(--brown)] disabled:opacity-50"
          >
            <Settings style={{ width: 12, height: 12 }} />
            설정
          </button>
        </div>

        {loading || !catalog ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} style={{ background: 'var(--bg)' }} className="h-12 rounded animate-pulse" />
            ))}
          </div>
        ) : (
          <>
            {/* 정령 */}
            <section>
              <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text)' }}>정령</p>
              <div className="flex gap-3 mb-2">
                <SpiritPortrait spirit={spirit1} emptyLabel="주정령" />
                <SpiritPortrait spirit={spirit2} emptyLabel="보조정령" />
              </div>
              <p className="text-[10px] font-medium mb-1" style={{ color: 'var(--text-muted)' }}>정령 효과</p>
              {spiritBuffLines.length > 0 ? (
                <ul className="space-y-0.5">
                  {spiritBuffLines.map((line, i) => (
                    <li key={i} className="text-[11px]" style={{ color: 'var(--text-muted)' }}>- {line}</li>
                  ))}
                </ul>
              ) : (
                <p className="text-[11px]" style={{ color: 'var(--text-disabled)' }}>- 미적용</p>
              )}
            </section>

            {/* 진법 */}
            <section style={{ borderTop: '1px solid var(--border)' }} className="pt-3">
              {jinbeop ? (
                <>
                  <p className="text-xs font-semibold mb-1" style={{ color: 'var(--text)' }}>{jinbeop.name}</p>
                  <p className="text-[10px] font-medium mb-1" style={{ color: 'var(--text-muted)' }}>진법 효과</p>
                  <ul className="space-y-0.5">
                    {jinbeopBuffLines.map((line, i) => (
                      <li key={i} className="text-[11px]" style={{ color: 'var(--text-muted)' }}>- {line}</li>
                    ))}
                  </ul>
                </>
              ) : (
                <>
                  <p className="text-xs font-semibold mb-1" style={{ color: 'var(--text-disabled)' }}>진법 미적용</p>
                  <p className="text-[11px]" style={{ color: 'var(--text-disabled)' }}>-</p>
                </>
              )}
            </section>

            {/* 가호 / 공명 — 확장용 플레이스홀더 */}
            <section style={{ borderTop: '1px solid var(--border)' }} className="pt-3">
              <p className="text-xs font-semibold mb-1" style={{ color: 'var(--text-disabled)' }}>가호</p>
              <p className="text-[11px] mb-2" style={{ color: 'var(--text-disabled)' }}>-</p>
              <p className="text-xs font-semibold mb-1" style={{ color: 'var(--text-disabled)' }}>공명</p>
              <p className="text-[11px]" style={{ color: 'var(--text-disabled)' }}>-</p>
            </section>
          </>
        )}
      </div>

      {catalog && (
        <DeckEffectSettingsModal
          open={modalOpen}
          catalog={catalog}
          initialDraft={draftFromEffects(effects)}
          saving={saving}
          onClose={() => !saving && setModalOpen(false)}
          onSave={handleSave}
        />
      )}
    </>
  );
}
