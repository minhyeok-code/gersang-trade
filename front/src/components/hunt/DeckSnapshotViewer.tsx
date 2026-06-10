'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  api,
  type DeckSnapshotContentDto,
  type MercenaryCharacteristicCatalogDto,
  type MonsterDto,
} from '@/lib/api';
import { SnapshotDpsPanel } from '@/components/hunt/SnapshotDpsPanel';
import { SnapshotMemberModal } from '@/components/hunt/SnapshotMemberModal';

export interface SnapshotDpsMeta {
  rawDps?: number | null;
  adjustDps?: number | null;
  finalDps?: number | null;
  totalResistPierce?: number | null;
  totalElementPierce?: number | null;
  monster?: MonsterDto | null;
  monsterName?: string | null;
  resistAfterDebuff?: number | null;
  effectiveMonsterElement?: number | null;
  resistPassRate?: number | null;
}

export function DeckSnapshotViewer({
  content,
  headerExtra,
  dpsMeta,
}: {
  content: DeckSnapshotContentDto;
  headerExtra?: React.ReactNode;
  dpsMeta?: SnapshotDpsMeta | null;
}) {
  const mercenaryIds = useMemo(
    () => [...new Set(content.members.map((m) => m.member.mercenaryId))],
    [content.members],
  );

  const [catalogs, setCatalogs] = useState<Record<number, MercenaryCharacteristicCatalogDto>>({});
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [selectedMemberEntry, setSelectedMemberEntry] = useState<
    DeckSnapshotContentDto['members'][number] | null
  >(null);

  useEffect(() => {
    let cancelled = false;
    setCatalogLoading(true);
    Promise.all(
      mercenaryIds.map((id) =>
        api.getMercenaryCharacteristics(id)
          .then((catalog) => ({ id, catalog }))
          .catch(() => null),
      ),
    )
      .then((results) => {
        if (cancelled) return;
        const next: Record<number, MercenaryCharacteristicCatalogDto> = {};
        for (const row of results) {
          if (row) next[row.id] = row.catalog;
        }
        setCatalogs(next);
      })
      .finally(() => {
        if (!cancelled) setCatalogLoading(false);
      });
    return () => { cancelled = true; };
  }, [mercenaryIds]);

  const dpsInputByMemberId = useMemo(() => {
    const map = new Map<number, (typeof content.dpsContext.memberInputs)[number]>();
    for (const input of content.dpsContext.memberInputs) {
      map.set(input.memberId, input);
    }
    return map;
  }, [content.dpsContext.memberInputs]);

  const elementValueByMemberId = useMemo(() => {
    const map = new Map<number, number>();
    for (const row of content.dpsContext.memberElementValues ?? []) {
      map.set(row.memberId, row.elementValue);
    }
    return map;
  }, [content.dpsContext.memberElementValues]);

  const resistPierce = dpsMeta?.totalResistPierce ?? content.totalResDown;
  const deckMeta = [
    content.attrXValue != null ? `속성값 ${content.attrXValue}` : null,
    resistPierce != null ? `저항깎 ${resistPierce.toLocaleString()}` : null,
    dpsMeta?.totalElementPierce != null ? `속성깎 ${dpsMeta.totalElementPierce.toLocaleString()}` : null,
  ].filter(Boolean).join(' · ');

  return (
    <div className="space-y-4">
      <div
        style={{ background: 'var(--card)', border: '1px solid var(--border)' }}
        className="rounded-xl p-4"
      >
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="font-semibold text-lg" style={{ color: 'var(--text)' }}>
              {content.deckName}
            </h2>
            {deckMeta && (
              <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>{deckMeta}</p>
            )}
          </div>
          {headerExtra}
        </div>

        {content.effects && (
          <div className="mt-3 flex flex-wrap gap-2 text-[13px]" style={{ color: 'var(--text-muted)' }}>
            {content.effects.gonmyeongLevel != null && (
              <span className="px-2 py-0.5 rounded" style={{ background: 'var(--bg)' }}>
                공명 Lv.{content.effects.gonmyeongLevel}
              </span>
            )}
            {content.effects.gahoLevel != null && (
              <span className="px-2 py-0.5 rounded" style={{ background: 'var(--bg)' }}>
                가호 Lv.{content.effects.gahoLevel}
              </span>
            )}
            {content.effects.jinbeop?.name && (
              <span className="px-2 py-0.5 rounded" style={{ background: 'var(--bg)' }}>
                진법: {content.effects.jinbeop.name}
              </span>
            )}
            {content.effects.cheungjin?.name && (
              <span className="px-2 py-0.5 rounded" style={{ background: 'var(--bg)' }}>
                청진: {content.effects.cheungjin.name}
              </span>
            )}
            {content.effects.spirits?.map((spirit, idx) => (
              <span key={idx} className="px-2 py-0.5 rounded" style={{ background: 'var(--bg)' }}>
                정령: {spirit.name}
              </span>
            ))}
          </div>
        )}
      </div>

      {dpsMeta && (
        <SnapshotDpsPanel
          rawDps={dpsMeta.rawDps}
          adjustDps={dpsMeta.adjustDps}
          finalDps={dpsMeta.finalDps}
          monster={dpsMeta.monster}
          monsterName={dpsMeta.monsterName}
          resistanceType={content.dpsContext.resistanceType}
          resistAfterDebuff={dpsMeta.resistAfterDebuff}
          effectiveMonsterElement={dpsMeta.effectiveMonsterElement}
          resistPassRate={dpsMeta.resistPassRate}
        />
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 10 }}>
        {content.members.map(({ member, characteristics }) => {
          const dpsInput = dpsInputByMemberId.get(member.id);
          const elementValue = elementValueByMemberId.get(member.id);
          const imageUrl = member.mercenaryImageUrl ?? member.imageUrl;
          const level = dpsInput?.level ?? member.level;

          return (
            <button
              key={member.id}
              type="button"
              onClick={() => setSelectedMemberEntry({ member, characteristics })}
              style={{
                background: 'var(--card)',
                border: '1px solid var(--border)',
                minHeight: 170,
              }}
              className="rounded-xl overflow-hidden flex flex-col text-left hover:border-[var(--brown)] transition-colors cursor-pointer"
            >
              <div style={{ background: 'var(--border)', aspectRatio: '1 / 1' }} className="relative overflow-hidden">
                {imageUrl ? (
                  <img src={imageUrl} alt={member.mercenaryName} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <span className="font-serif text-3xl font-bold" style={{ color: 'var(--text-muted)' }}>
                      {member.mercenaryName.charAt(0)}
                    </span>
                  </div>
                )}
              </div>

              <div className="p-1.5 flex-1 flex flex-col justify-end">
                <p
                  className="font-medium text-[11px] leading-tight truncate"
                  style={{ color: 'var(--text)' }}
                  title={member.mercenaryName}
                >
                  {member.mercenaryName}
                </p>
                <p className="text-[10px] leading-tight mt-0.5" style={{ color: 'var(--text-muted)' }}>
                  Lv.{level ?? '-'}
                </p>
                {elementValue != null && (
                  <p className="text-[10px] leading-tight mt-0.5" style={{ color: 'var(--text-muted)' }}>
                    속성값 {elementValue.toLocaleString()}
                  </p>
                )}
              </div>
            </button>
          );
        })}
      </div>

      {selectedMemberEntry && (
        <SnapshotMemberModal
          entry={selectedMemberEntry}
          deckId={content.deckId}
          catalog={catalogs[selectedMemberEntry.member.mercenaryId]}
          catalogLoading={catalogLoading}
          level={dpsInputByMemberId.get(selectedMemberEntry.member.id)?.level ?? selectedMemberEntry.member.level}
          elementValue={elementValueByMemberId.get(selectedMemberEntry.member.id)}
          onClose={() => setSelectedMemberEntry(null)}
        />
      )}
    </div>
  );
}
