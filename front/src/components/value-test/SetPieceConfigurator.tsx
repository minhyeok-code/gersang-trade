'use client';

import {
  applyBundleKindToPieces,
  BUNDLE_KIND_DESCRIPTION,
  BUNDLE_KIND_LABEL,
  generateSetBundleTitle,
  getRitualCountOptions,
  sortSetPiecesBySlot,
  type RitualCountOption,
  type RitualMarkOption,
  type SetBundleKind,
  type SetPieceState,
} from '@/lib/setTitle';

interface SetPieceConfiguratorProps {
  setName: string;
  pieces: SetPieceState[];
  uniqueRituals: RitualMarkOption[];
  bundleKind: SetBundleKind;
  ritualCount: RitualCountOption;
  ritualMark: RitualMarkOption | null;
  onBundleKindChange: (kind: SetBundleKind) => void;
  onRitualCountChange: (count: RitualCountOption) => void;
  onRitualMarkChange: (mark: RitualMarkOption | null) => void;
  onPiecesChange: (pieces: SetPieceState[]) => void;
  /** true면 피스별 수동 조정 허용 */
  allowManualPieces?: boolean;
}

const BUNDLE_KINDS: SetBundleKind[] = ['GAMTU', 'BYEON', 'BANSSANG', 'FULL', 'FULL_BANSSANG'];

const btnBase = 'px-2 py-1.5 text-xs rounded transition-colors';

function btnStyle(active: boolean) {
  return {
    background: active ? 'var(--brown)' : 'var(--bg)',
    border: `1px solid ${active ? 'var(--brown)' : 'var(--border)'}`,
    color: active ? 'var(--beige)' : 'var(--text-muted)',
  };
}

/** 세트 구성·주술 마크→개수·표기 미리보기 UI */
export default function SetPieceConfigurator({
  setName,
  pieces,
  uniqueRituals,
  bundleKind,
  ritualCount,
  ritualMark,
  onBundleKindChange,
  onRitualCountChange,
  onRitualMarkChange,
  onPiecesChange,
  allowManualPieces = false,
}: SetPieceConfiguratorProps) {
  const displayTitle = generateSetBundleTitle(
    setName,
    bundleKind,
    ritualCount,
    ritualCount > 0 ? ritualMark?.label ?? null : null,
  );

  const ritualCountChoices = getRitualCountOptions(bundleKind).filter((c) => c > 0);
  const showRitualMark = ritualCountChoices.length > 0 && uniqueRituals.length > 0;

  function handleKindChange(kind: SetBundleKind) {
    onBundleKindChange(kind);
    onRitualCountChange(0);
    onRitualMarkChange(null);
    onPiecesChange(applyBundleKindToPieces(pieces, kind, 0));
  }

  function handleRitualMarkChange(opt: RitualMarkOption) {
    const deselect = ritualMark?.label === opt.label;
    if (deselect) {
      onRitualMarkChange(null);
      onRitualCountChange(0);
      onPiecesChange(applyBundleKindToPieces(pieces, bundleKind, 0));
      return;
    }
    onRitualMarkChange(opt);
    onRitualCountChange(0);
    onPiecesChange(applyBundleKindToPieces(pieces, bundleKind, 0));
  }

  function handleRitualCountChange(count: RitualCountOption) {
    if (!ritualMark) return;
    onRitualCountChange(count);
    onPiecesChange(applyBundleKindToPieces(pieces, bundleKind, count));
  }

  function handlePieceToggle(idx: number, field: 'included' | 'hasRitual') {
    if (!allowManualPieces) return;
    onPiecesChange(
      pieces.map((p, i) => {
        if (i !== idx) return p;
        if (field === 'included') {
          return { ...p, included: !p.included, hasRitual: p.included ? false : p.hasRitual };
        }
        return { ...p, hasRitual: !p.hasRitual };
      }),
    );
  }

  const sortedPieces = sortSetPiecesBySlot(pieces);

  return (
    <div className="space-y-3">
      {/* 표기 미리보기 */}
      <div
        className="rounded-lg px-3 py-2 text-sm"
        style={{ background: 'var(--bg)', border: '1px dashed var(--brown)' }}
      >
        <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
          표기 미리보기
        </span>
        <p className="font-serif font-semibold mt-0.5" style={{ color: 'var(--brown)' }}>
          {displayTitle}
        </p>
      </div>

      {/* 구성 종류 */}
      <div>
        <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>
          세트 구성
        </label>
        <div className="flex flex-wrap gap-1.5">
          {BUNDLE_KINDS.map((kind) => (
            <button
              key={kind}
              type="button"
              onClick={() => handleKindChange(kind)}
              className={btnBase}
              style={btnStyle(bundleKind === kind)}
              title={BUNDLE_KIND_DESCRIPTION[kind]}
            >
              {BUNDLE_KIND_LABEL[kind]}
            </button>
          ))}
        </div>
        <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>
          {BUNDLE_KIND_DESCRIPTION[bundleKind]}
        </p>
      </div>

      {/* 주술 마크 (먼저 선택) */}
      {showRitualMark && (
        <div>
          <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>
            주술 마크
          </label>
          <div className="flex flex-wrap gap-1.5">
            {uniqueRituals.map((opt, idx) => (
              <button
                key={`${opt.ritualId}-${opt.outcome}-${idx}`}
                type="button"
                onClick={() => handleRitualMarkChange(opt)}
                className={btnBase}
                style={btnStyle(ritualMark?.label === opt.label)}
              >
                {opt.label}
              </button>
            ))}
          </div>
          <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>
            주술 없으면 선택하지 않습니다
          </p>
        </div>
      )}

      {/* 주술 개수 (마크 선택 후) */}
      {ritualMark && ritualCountChoices.length > 0 && (
        <div>
          <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>
            주술 개수
          </label>
          <div className="flex flex-wrap gap-1.5">
            {ritualCountChoices.map((count) => (
              <button
                key={count}
                type="button"
                onClick={() => handleRitualCountChange(count)}
                className={btnBase}
                style={btnStyle(ritualCount === count)}
              >
                {count}개
              </button>
            ))}
          </div>
          {ritualCount === 2 && (
            <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>
              갑옷·투구에만 주술 (2{'{마크}'}{setName}갑투)
            </p>
          )}
          {ritualCount === 3 && bundleKind === 'BYEON' && (
            <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>
              변두리(장갑·요대·신발)에만 주술 (3{'{마크}'}변…)
            </p>
          )}
          {ritualCount === 3 && (bundleKind === 'FULL' || bundleKind === 'FULL_BANSSANG') && (
            <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>
              변두리(장갑·요대·신발)에만 주술 (3{'{마크}'} 풀 …)
            </p>
          )}
          {ritualCount === 5 && (
            <p className="text-xs mt-1" style={{ color: 'var(--text-disabled)' }}>
              주술 가능 5피스 전부 (5{'{마크}'} 풀 …)
            </p>
          )}
        </div>
      )}

      {/* 포함 피스 요약 */}
      <div>
        <label className="block text-xs mb-1.5 font-medium" style={{ color: 'var(--text-muted)' }}>
          포함 피스
        </label>
        <div className="space-y-1">
          {sortedPieces.map((piece) => {
            const idx = pieces.findIndex((p) => p.itemId === piece.itemId);
            const show = piece.included;
            return (
              <div
                key={piece.itemId}
                className="rounded px-2 py-1 text-xs flex items-center justify-between"
                style={{
                  background: 'var(--bg)',
                  border: '1px solid var(--border)',
                  opacity: show ? 1 : 0.35,
                }}
              >
                <span style={{ color: 'var(--text)' }}>
                  {piece.itemName}
                  <span className="ml-1" style={{ color: 'var(--text-muted)' }}>
                    ({piece.slot})
                  </span>
                </span>
                {show && piece.hasRitual && (
                  <span style={{ color: 'var(--brown)' }}>주술</span>
                )}
                {allowManualPieces && (
                  <div className="flex gap-2 ml-2">
                    <input
                      type="checkbox"
                      checked={piece.included}
                      onChange={() => handlePieceToggle(idx, 'included')}
                      title="포함"
                      style={{ accentColor: 'var(--brown)', width: 12, height: 12 }}
                    />
                    {piece.included && ritualCount > 0 && piece.hasRitualOptions && (
                      <input
                        type="checkbox"
                        checked={piece.hasRitual}
                        onChange={() => handlePieceToggle(idx, 'hasRitual')}
                        title="주술"
                        style={{ accentColor: 'var(--brown)', width: 12, height: 12 }}
                      />
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
