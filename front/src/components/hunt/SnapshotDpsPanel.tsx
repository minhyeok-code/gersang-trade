'use client';

import { Target } from 'lucide-react';
import type { MonsterDto } from '@/lib/api';
import { DpsBreakdown } from '@/components/hunt/DpsBreakdown';
import { StatWithDebuff } from '@/components/hunt/StatWithDebuff';

const ELEMENT_LABELS: Record<string, string> = {
  FIRE: '화', WATER: '수', WOOD: '목', METAL: '금', EARTH: '토', NONE: '무',
};

function monsterHittingResist(monster: MonsterDto) {
  return monster.hittingResistance ?? monster.resistance;
}

export interface SnapshotDpsPanelProps {
  rawDps?: number | null;
  adjustDps?: number | null;
  finalDps?: number | null;
  monster?: MonsterDto | null;
  monsterName?: string | null;
  resistanceType?: 'HITTING' | 'MAGIC';
  resistAfterDebuff?: number | null;
  effectiveMonsterElement?: number | null;
  resistPassRate?: number | null;
}

export function SnapshotDpsPanel({
  rawDps,
  adjustDps,
  finalDps,
  monster,
  monsterName,
  resistanceType = 'HITTING',
  resistAfterDebuff,
  effectiveMonsterElement,
  resistPassRate,
}: SnapshotDpsPanelProps) {
  const displayName = monster?.name ?? monsterName;
  const baseResist = monster
    ? (resistanceType === 'MAGIC'
      ? monster.magicResistance
      : monsterHittingResist(monster))
    : null;
  const resistLabel = resistanceType === 'MAGIC' ? '마법저항' : '타격저항';

  return (
    <div
      style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
      className="rounded-xl px-4 py-3"
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-0">
        {/* 좌측: DPS (기존 컴팩트 박스) */}
        <div
          className="sm:pr-4 border-b sm:border-b-0 sm:border-r pb-3 sm:pb-0"
          style={{ borderColor: 'var(--border)' }}
        >
          <p className="text-[12px] font-medium mb-2 flex items-center gap-1" style={{ color: 'var(--text-muted)' }}>
            <Target style={{ width: 12, height: 12 }} />
            DPS
          </p>
          <DpsBreakdown
            layout="stack"
            stackTextClass="text-[13px]"
            rawDps={rawDps}
            adjustDps={adjustDps}
            finalDps={finalDps}
          />
        </div>

        {/* 우측: 몬스터 */}
        <div className="sm:pl-4">
          {displayName ? (
            <>
              <p className="text-[12px] font-medium mb-2" style={{ color: 'var(--text-muted)' }}>
                {displayName}
              </p>
              <p className="text-[12px] mb-0.5" style={{ color: 'var(--text-muted)' }}>
                속성:{' '}
                <span style={{ color: 'var(--text)' }}>
                  {monster?.element ? (ELEMENT_LABELS[monster.element] ?? monster.element) : '-'}
                </span>
              </p>
              <StatWithDebuff
                className="text-[12px]"
                label={resistLabel}
                base={baseResist}
                debuffed={resistAfterDebuff}
              />
              {resistanceType === 'HITTING' && monster?.magicResistance != null && (
                <p className="text-[12px] leading-snug" style={{ color: 'var(--text-muted)' }}>
                  마법저항: <span style={{ color: 'var(--text)' }}>{monster.magicResistance}</span>
                </p>
              )}
              <StatWithDebuff
                className="text-[12px]"
                label="속성값"
                base={monster?.elementValue}
                debuffed={effectiveMonsterElement}
              />
              {resistPassRate != null && (
                <p className="text-[12px] leading-snug mt-0.5" style={{ color: 'var(--text-muted)' }}>
                  저항통과: <span style={{ color: 'var(--brown)' }}>{resistPassRate.toFixed(1)}%</span>
                </p>
              )}
            </>
          ) : (
            <p className="text-[13px]" style={{ color: 'var(--text-disabled)' }}>
              몬스터 정보 없음
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
