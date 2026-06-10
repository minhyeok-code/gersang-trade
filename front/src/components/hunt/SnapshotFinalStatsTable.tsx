'use client';

import { useMemo } from 'react';
import type { MemberStatsDto } from '@/lib/api';

const STAT_LABEL: Record<string, string> = {
  ELEMENT_VALUE: '속성값',
  DAMAGE_PERCENT: '데미지증가',
  CRITICAL_CHANCE: '크리티컬확률',
  STRENGTH: '힘',
  VITALITY: '생명력',
  DEXTERITY: '민첩',
  INTELLECT: '지력',
  ATTACK_POWER: '공격력',
  MIN_POWER: '최소공격력',
  MAX_POWER: '최대공격력',
};

const ALL_STAT_TARGETS = ['STRENGTH', 'DEXTERITY', 'VITALITY', 'INTELLECT'] as const;
const LEFT_STAT_COLS = ['STRENGTH', 'DEXTERITY', 'VITALITY', 'INTELLECT'] as const;
const RIGHT_STAT_COLS = ['ATTACK_POWER', 'ELEMENT_VALUE', 'DAMAGE_PERCENT', 'CRITICAL_CHANCE'] as const;
const FIXED_STAT_ROWS = Math.max(LEFT_STAT_COLS.length, RIGHT_STAT_COLS.length);

const COMPOUND_STAT_LABEL: Record<string, string> = {
  DAMAGE_PERCENT_FIRE: '화속성 데미지증가',
  DAMAGE_PERCENT_WIND: '풍속성 데미지증가',
  DAMAGE_PERCENT_WATER: '수속성 데미지증가',
  DAMAGE_PERCENT_THUNDER: '뇌속성 데미지증가',
  DAMAGE_PERCENT_EARTH: '지속성 데미지증가',
};

function formatStatType(statType: string) {
  return STAT_LABEL[statType] ?? statType;
}

/** 덱 설정 모달 최종 스탯 표와 동일 — totalStats + ALL_STAT 분배 */
function computeDisplayTotalMap(stats: MemberStatsDto | null): Map<string, number> {
  const map = new Map<string, number>();
  stats?.totalStats?.forEach((s) => {
    map.set(s.statType, (map.get(s.statType) ?? 0) + s.value);
  });
  const allStatVal = map.get('ALL_STAT') ?? 0;
  if (allStatVal !== 0) {
    map.delete('ALL_STAT');
    for (const t of ALL_STAT_TARGETS) {
      map.set(t, (map.get(t) ?? 0) + allStatVal);
    }
  }
  return map;
}

export function SnapshotFinalStatsTable({
  stats,
  loading,
  error,
}: {
  stats: MemberStatsDto | null;
  loading: boolean;
  error?: string | null;
}) {
  const displayTotalMap = useMemo(() => computeDisplayTotalMap(stats), [stats]);

  if (loading) {
    return <div style={{ background: 'var(--card)' }} className="h-40 rounded animate-pulse" />;
  }

  if (error) {
    return <p className="text-sm" style={{ color: 'var(--text-muted)' }}>{error}</p>;
  }

  if (!stats) {
    return <p className="text-sm" style={{ color: 'var(--text-muted)' }}>스탯 정보가 없습니다.</p>;
  }

  return (
    <div>
      <p className="text-sm font-semibold mb-1" style={{ color: 'var(--text)' }}>최종 스탯</p>
      <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-sm">
        <div
          className="grid grid-cols-4 text-xs font-semibold px-3 py-2"
          style={{ background: 'var(--bg)', color: 'var(--text-muted)', borderBottom: '1px solid var(--border)' }}
        >
          <span>스탯</span>
          <span className="text-right pr-4">값</span>
          <span>스탯</span>
          <span className="text-right pr-2">값</span>
        </div>
        {Array.from({ length: FIXED_STAT_ROWS }).map((_, i) => {
          const leftType = LEFT_STAT_COLS[i];
          const rightType = RIGHT_STAT_COLS[i];
          const leftVal = leftType ? (displayTotalMap.get(leftType) ?? 0) : null;
          const isAtkRow = rightType === 'ATTACK_POWER';
          const minPow = displayTotalMap.get('MIN_POWER') ?? 0;
          const maxPow = displayTotalMap.get('MAX_POWER') ?? 0;
          const rightVal = isAtkRow ? null : (rightType ? (displayTotalMap.get(rightType) ?? 0) : null);

          return (
            <div
              key={i}
              className="grid grid-cols-4 items-center px-3 py-2.5"
              style={{ borderTop: '1px solid var(--border)' }}
            >
              <span style={{ color: 'var(--text-muted)' }}>
                {leftType && formatStatType(leftType)}
              </span>
              <span className="text-right pr-4 font-semibold" style={{ color: 'var(--brown)' }}>
                {leftVal !== null ? leftVal : ''}
              </span>
              <span style={{ color: 'var(--text-muted)' }}>
                {isAtkRow ? '공격력' : (rightType && formatStatType(rightType))}
              </span>
              <span className="text-right pr-2 font-semibold" style={{ color: 'var(--brown)' }}>
                {isAtkRow
                  ? (minPow === 0 && maxPow === 0 ? '-' : `${minPow} - ${maxPow}`)
                  : (rightVal !== null ? rightVal : '')}
              </span>
            </div>
          );
        })}
      </div>

      {(stats.lgAllyElementalStats?.length ?? 0) > 0 && (
        <div className="mt-4">
          <p className="text-xs font-semibold mb-2" style={{ color: 'var(--text-muted)' }}>전설장수(아군) 속성 버프</p>
          <div style={{ border: '1px solid var(--border)' }} className="rounded-lg overflow-hidden text-xs">
            {stats.lgAllyElementalStats!.map((e, idx) => (
              <div
                key={e.statKey}
                className="flex justify-between items-center px-3 py-2"
                style={{ borderTop: idx > 0 ? '1px solid var(--border)' : undefined }}
              >
                <span style={{ color: 'var(--text-muted)' }}>
                  {COMPOUND_STAT_LABEL[e.statKey] ?? e.statKey}
                </span>
                <span className="font-semibold" style={{ color: 'var(--brown)' }}>
                  +{e.value}%
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
