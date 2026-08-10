'use client';

import { useState, useEffect, useCallback, type ChangeEvent } from 'react';

const BASE = '';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

function setToken(token: string) {
  localStorage.setItem('accessToken', token);
}

/** AT 만료 시 RT 쿠키로 새 AT를 발급받는다. 실패 시 null. */
async function tryRefresh(): Promise<string | null> {
  try {
    const res = await fetch(`${BASE}/auth/refresh`, { method: 'POST', credentials: 'include' });
    if (!res.ok) return null;
    const data = (await res.json()) as { accessToken?: string };
    if (data.accessToken) {
      setToken(data.accessToken);
      return data.accessToken;
    }
    return null;
  } catch {
    return null;
  }
}

async function req<T>(path: string, options: RequestInit = {}): Promise<T> {
  const doFetch = (token: string | null) => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return fetch(`${BASE}${path}`, { ...options, headers, credentials: 'include' });
  };

  let res = await doFetch(getToken());
  if (res.status === 401 || res.status === 403) {
    const newToken = await tryRefresh();
    if (newToken) res = await doFetch(newToken);
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${res.status}: ${text}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ───────────────────────── 타입 ─────────────────────────

interface StepIssue {
  order: number;
  label: string;
  kind: string;
  matchName: string;
  status: string; // UNMATCHED | AMBIGUOUS
}
interface ImportResult {
  guideId: number;
  title: string;
  targetMercenaryMatched: boolean;
  stepCount: number;
  linkedCount: number;
  unmatchedCount: number;
  ambiguousCount: number;
  issues: StepIssue[];
}
interface ImportReport {
  guidesCreated: number;
  results: ImportResult[];
}
interface AdminGuide {
  id: number;
  title: string;
  phase: string;
  version: string;
  author: string | null;
  published: boolean;
  targetMercenaryName: string | null;
  stepCount: number;
}
interface GuideStepResp {
  id: number;
  stepOrder: number;
  stepType: string;
  label: string;
  note: string | null;
  iconUrl: string | null;
  linkedItemId: number | null; linkedItemName: string | null;
  linkedSetId: number | null; linkedSetName: string | null;
  linkedMercenaryId: number | null; linkedMercenaryName: string | null;
}
interface CatalogHit { id: number; name: string; }

const LINKABLE_TYPES = ['GET_ITEM', 'GET_SET', 'SELL', 'HIRE_MERCENARY'];
const STEP_TYPE_OPTIONS = [
  { value: 'GET_ITEM', label: '아이템 획득' },
  { value: 'GET_SET', label: '세트 획득' },
  { value: 'HIRE_MERCENARY', label: '용병 고용' },
  { value: 'SELL', label: '판매' },
  { value: 'HUNT', label: '사냥터' },
  { value: 'TRAIT', label: '특성' },
  { value: 'JOB_CHANGE', label: '전직' },
  { value: 'CRAFT', label: '제작' },
  { value: 'QUEST', label: '퀘스트' },
  { value: 'MISC', label: '기타' },
];

// ───────────────────────── 화면 ─────────────────────────

export default function GuideImportPage() {
  const [jsonText, setJsonText] = useState('');
  const [fileName, setFileName] = useState('');
  const [report, setReport] = useState<ImportReport | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [guides, setGuides] = useState<AdminGuide[]>([]);
  const [reviewId, setReviewId] = useState<number | null>(null);
  const [steps, setSteps] = useState<GuideStepResp[]>([]);
  const [editStep, setEditStep] = useState<number | null>(null);
  const [sKind, setSKind] = useState<'ITEM' | 'SET' | 'MERCENARY'>('ITEM');
  const [sQuery, setSQuery] = useState('');
  const [sHits, setSHits] = useState<CatalogHit[]>([]);
  const [editType, setEditType] = useState('MISC');
  const [editLabel, setEditLabel] = useState('');
  const [editNote, setEditNote] = useState('');

  const loadGuides = useCallback(async () => {
    try {
      const list = await req<AdminGuide[]>('/admin/guides');
      setGuides(list ?? []);
    } catch {
      /* 목록 로드 실패는 조용히 무시 */
    }
  }, []);

  useEffect(() => {
    loadGuides();
  }, [loadGuides]);

  async function togglePublish(g: AdminGuide) {
    await req(`/admin/guides/${g.id}/published?published=${!g.published}`, { method: 'PATCH' });
    loadGuides();
  }

  async function deleteGuide(g: AdminGuide) {
    if (!confirm(`"${g.title}" 가이드를 삭제할까요? (스텝 ${g.stepCount}개 포함)`)) return;
    await req(`/admin/guides/${g.id}`, { method: 'DELETE' });
    loadGuides();
  }

  async function openReview(g: AdminGuide) {
    setReviewId(g.id);
    setEditStep(null); setSHits([]); setSQuery('');
    try {
      setSteps(await req<GuideStepResp[]>(`/api/guides/${g.id}/steps`));
    } catch (e) { setError(e instanceof Error ? e.message : '스텝 로드 실패'); }
  }

  async function reloadSteps() {
    if (reviewId == null) return;
    setSteps(await req<GuideStepResp[]>(`/api/guides/${reviewId}/steps`));
  }

  function openLink(step: GuideStepResp) {
    setEditStep(step.id);
    setEditType(step.stepType);
    setEditLabel(step.label);
    setEditNote(step.note ?? '');
    setSKind(step.stepType === 'GET_SET' ? 'SET' : step.stepType === 'HIRE_MERCENARY' ? 'MERCENARY' : 'ITEM');
    setSQuery(''); setSHits([]);
  }

  /** 유형·라벨·노트 저장 (현재 연결 유지). */
  async function saveContent(step: GuideStepResp) {
    if (!editLabel.trim()) { setError('라벨을 입력하세요'); return; }
    await req(`/admin/guides/steps/${step.id}`, {
      method: 'PATCH',
      body: JSON.stringify({
        stepType: editType,
        label: editLabel.trim(),
        note: editNote.trim() || null,
        iconUrl: step.iconUrl,
        linkedItemId: step.linkedItemId,
        linkedSetId: step.linkedSetId,
        linkedMercenaryId: step.linkedMercenaryId,
      }),
    });
    reloadSteps();
  }

  async function searchCatalog() {
    if (!sQuery.trim()) return;
    const q = encodeURIComponent(sQuery.trim());
    try {
      if (sKind === 'ITEM') {
        const hits = await req<CatalogHit[]>(`/api/items/search?q=${q}`);
        setSHits((hits ?? []).map((h) => ({ id: h.id, name: h.name })));
      } else {
        const path = sKind === 'SET' ? 'sets' : 'mercenaries';
        const page = await req<{ content: CatalogHit[] }>(`/admin/${path}?name=${q}&size=20`);
        setSHits((page.content ?? []).map((h) => ({ id: h.id, name: h.name })));
      }
    } catch (e) { setError(e instanceof Error ? e.message : '검색 실패'); }
  }

  async function linkStep(step: GuideStepResp, kind: string, targetId: number | null) {
    await req(`/admin/guides/steps/${step.id}`, {
      method: 'PATCH',
      body: JSON.stringify({
        stepType: editType,
        label: (editLabel.trim() || step.label),
        note: editNote.trim() || null,
        iconUrl: step.iconUrl,
        linkedItemId: kind === 'ITEM' ? targetId : null,
        linkedSetId: kind === 'SET' ? targetId : null,
        linkedMercenaryId: kind === 'MERCENARY' ? targetId : null,
      }),
    });
    setEditStep(null); setSHits([]); setSQuery('');
    reloadSteps();
  }

  function onFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = () => setJsonText(String(reader.result ?? ''));
    reader.readAsText(file);
  }

  async function onImport() {
    setError('');
    setReport(null);

    let payload: unknown;
    try {
      payload = JSON.parse(jsonText);
    } catch {
      setError('JSON 형식이 올바르지 않습니다. 파일 내용을 다시 확인하세요.');
      return;
    }

    setLoading(true);
    try {
      const result = await req<ImportReport>('/admin/guides/import', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      setReport(result);
      loadGuides();
    } catch (e) {
      setError(e instanceof Error ? e.message : '주입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100 p-6">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-2xl font-bold text-yellow-400 mb-1">가이드 일괄 주입</h1>
        <p className="text-sm text-gray-400 mb-6">
          이미지 추출 JSON을 붙여넣거나 파일로 불러온 뒤 주입하세요. 주입된 가이드는 비공개(published=false)로
          저장되며, 아래 리포트에서 매칭 실패 스텝을 확인해 검수합니다.
        </p>

        <div className="mb-3 flex items-center gap-3">
          <label className="inline-block cursor-pointer bg-gray-700 hover:bg-gray-600 text-sm px-3 py-2 rounded">
            <input type="file" accept="application/json,.json" onChange={onFile} className="hidden" />
            JSON 파일 선택
          </label>
          {fileName && <span className="text-xs text-gray-400">{fileName}</span>}
        </div>

        <textarea
          value={jsonText}
          onChange={(e) => setJsonText(e.target.value)}
          placeholder='{ "guides": [ ... ] }'
          spellCheck={false}
          className="w-full h-64 bg-gray-800 border border-gray-700 rounded p-3 font-mono text-xs text-gray-100 mb-3"
        />

        <div className="flex items-center gap-3 mb-6">
          <button
            onClick={onImport}
            disabled={loading || !jsonText.trim()}
            className="bg-yellow-500 hover:bg-yellow-400 disabled:opacity-40 text-gray-900 font-semibold text-sm px-5 py-2 rounded"
          >
            {loading ? '주입 중…' : '주입하기'}
          </button>
          {error && <span className="text-sm text-red-400">{error}</span>}
        </div>

        {guides.length > 0 && (
          <div className="bg-gray-800 rounded p-4 mb-6">
            <h2 className="text-lg font-bold text-yellow-400 mb-3">주입된 가이드</h2>
            <table className="w-full text-sm">
              <thead className="text-gray-400 text-left">
                <tr>
                  <th className="py-1 pr-3">#</th>
                  <th className="py-1 pr-3">제목</th>
                  <th className="py-1 pr-3">단계</th>
                  <th className="py-1 pr-3">스텝</th>
                  <th className="py-1 pr-3">공개</th>
                  <th className="py-1">관리</th>
                </tr>
              </thead>
              <tbody>
                {guides.map((g) => (
                  <tr key={g.id} className="border-t border-gray-700">
                    <td className="py-2 pr-3 text-gray-400">{g.id}</td>
                    <td className="py-2 pr-3">
                      {g.title}
                      {g.targetMercenaryName && (
                        <span className="text-xs text-gray-500 ml-2">→ {g.targetMercenaryName}</span>
                      )}
                    </td>
                    <td className="py-2 pr-3 text-gray-400">{g.phase === 'AWAKENED' ? '각성' : '일반'}</td>
                    <td className="py-2 pr-3 text-gray-400">{g.stepCount}</td>
                    <td className="py-2 pr-3">
                      {g.published ? (
                        <span className="text-green-400 text-xs">공개</span>
                      ) : (
                        <span className="text-gray-500 text-xs">비공개</span>
                      )}
                    </td>
                    <td className="py-2 whitespace-nowrap">
                      <button
                        onClick={() => openReview(g)}
                        className="text-xs bg-blue-800 hover:bg-blue-700 px-2 py-1 rounded mr-2"
                      >
                        검수
                      </button>
                      <button
                        onClick={() => togglePublish(g)}
                        className="text-xs bg-gray-700 hover:bg-gray-600 px-2 py-1 rounded mr-2"
                      >
                        {g.published ? '비공개로' : '공개하기'}
                      </button>
                      <button
                        onClick={() => deleteGuide(g)}
                        className="text-xs bg-red-800 hover:bg-red-700 px-2 py-1 rounded"
                      >
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {reviewId != null && (
          <div className="bg-gray-800 rounded p-4 mb-6">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg font-bold text-yellow-400">스텝 검수 — 가이드 #{reviewId}</h2>
              <button onClick={() => setReviewId(null)} className="text-xs text-gray-400 hover:text-gray-200">닫기</button>
            </div>
            <p className="text-xs text-gray-500 mb-3">각 스텝의 "연결/유형"에서 유형(예: 기타→세트)을 바꾸거나, 아이템/세트/용병을 검색해 연결하세요.</p>
            <table className="w-full text-sm">
              <thead className="text-gray-400 text-left">
                <tr><th className="py-1 pr-3">#</th><th className="py-1 pr-3">라벨</th><th className="py-1 pr-3">유형</th><th className="py-1 pr-3">연결</th><th className="py-1"></th></tr>
              </thead>
              <tbody>
                {steps.map((s) => {
                  const linked = s.linkedItemName ?? s.linkedSetName ?? s.linkedMercenaryName;
                  const linkable = LINKABLE_TYPES.includes(s.stepType);
                  return (
                    <tr key={s.id} className="border-t border-gray-700 align-top">
                      <td className="py-2 pr-3 text-gray-400">{s.stepOrder}</td>
                      <td className="py-2 pr-3">
                        {s.label}
                        {s.note && <span className="block text-xs text-gray-500">{s.note}</span>}
                      </td>
                      <td className="py-2 pr-3 text-gray-500 text-xs">{s.stepType}</td>
                      <td className="py-2 pr-3">
                        {linked ? <span className="text-green-400">{linked}</span> : <span className="text-gray-600">{linkable ? '미연결' : '—'}</span>}
                      </td>
                      <td className="py-2">
                        {editStep === s.id ? (
                          <div className="bg-gray-900 rounded p-2 w-72">
                            <div className="flex gap-1 mb-1">
                              <select value={editType} onChange={(e) => setEditType(e.target.value)} className="flex-1 bg-gray-700 border border-gray-600 rounded px-1 text-xs">
                                {STEP_TYPE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                              </select>
                              <button onClick={() => saveContent(s)} className="text-xs bg-gray-700 hover:bg-gray-600 px-2 rounded whitespace-nowrap">저장</button>
                            </div>
                            <input value={editLabel} onChange={(e) => setEditLabel(e.target.value)} placeholder="라벨" className="w-full mb-1 bg-gray-700 border border-gray-600 rounded px-2 py-1 text-xs" />
                            <input value={editNote} onChange={(e) => setEditNote(e.target.value)} placeholder="노트(설명, 선택)" className="w-full mb-1 bg-gray-700 border border-gray-600 rounded px-2 py-1 text-xs" />
                            <div className="flex gap-1 mb-1">
                              <select
                                value={sKind}
                                onChange={(e) => { setSKind(e.target.value as 'ITEM' | 'SET' | 'MERCENARY'); setSHits([]); }}
                                className="bg-gray-700 border border-gray-600 rounded px-1 text-xs"
                              >
                                <option value="ITEM">아이템</option>
                                <option value="SET">세트</option>
                                <option value="MERCENARY">용병</option>
                              </select>
                              <input
                                value={sQuery}
                                onChange={(e) => setSQuery(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && searchCatalog()}
                                placeholder="이름 검색"
                                className="flex-1 min-w-0 bg-gray-700 border border-gray-600 rounded px-2 text-xs"
                              />
                              <button onClick={searchCatalog} className="text-xs bg-gray-700 hover:bg-gray-600 px-2 rounded">검색</button>
                            </div>
                            {sHits.length > 0 && (
                              <ul className="max-h-40 overflow-y-auto border border-gray-700 rounded divide-y divide-gray-700 mb-1">
                                {sHits.map((h) => (
                                  <li key={h.id} onClick={() => linkStep(s, sKind, h.id)} className="px-2 py-1 text-xs cursor-pointer hover:bg-gray-700">{h.name}</li>
                                ))}
                              </ul>
                            )}
                            <div className="flex gap-3">
                              <button onClick={() => setEditStep(null)} className="text-xs text-gray-400 hover:text-gray-200">취소</button>
                              {linked && <button onClick={() => linkStep(s, '', null)} className="text-xs text-red-400 hover:text-red-300">연결 해제</button>}
                            </div>
                          </div>
                        ) : (
                          <button onClick={() => openLink(s)} className="text-xs bg-blue-800 hover:bg-blue-700 px-2 py-1 rounded">{linked ? '변경' : '연결/유형'}</button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {report && (
          <div className="bg-gray-800 rounded p-4">
            <h2 className="text-lg font-bold text-yellow-400 mb-3">
              주입 완료 — 가이드 {report.guidesCreated}건
            </h2>
            {report.results.map((r) => (
              <div key={r.guideId} className="mb-5 border-b border-gray-700 pb-4 last:border-0">
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mb-2">
                  <span className="font-semibold">{r.title}</span>
                  <span className="text-xs text-gray-400">#{r.guideId}</span>
                  {!r.targetMercenaryMatched && (
                    <span className="text-xs text-red-400">대상 용병 매칭 실패</span>
                  )}
                </div>
                <div className="flex flex-wrap gap-2 text-xs mb-3">
                  <span className="bg-gray-700 px-2 py-1 rounded">스텝 {r.stepCount}</span>
                  <span className="bg-green-900 text-green-200 px-2 py-1 rounded">연결 {r.linkedCount}</span>
                  <span className="bg-yellow-900 text-yellow-200 px-2 py-1 rounded">미매칭 {r.unmatchedCount}</span>
                  <span className="bg-orange-900 text-orange-200 px-2 py-1 rounded">중복 {r.ambiguousCount}</span>
                </div>
                {r.issues.length > 0 && (
                  <table className="w-full text-xs">
                    <thead className="text-gray-400 text-left">
                      <tr>
                        <th className="py-1 pr-3">#</th>
                        <th className="py-1 pr-3">라벨</th>
                        <th className="py-1 pr-3">종류</th>
                        <th className="py-1 pr-3">검색어</th>
                        <th className="py-1">상태</th>
                      </tr>
                    </thead>
                    <tbody>
                      {r.issues.map((it) => (
                        <tr key={`${r.guideId}-${it.order}`} className="border-t border-gray-700">
                          <td className="py-1 pr-3 text-gray-400">{it.order}</td>
                          <td className="py-1 pr-3">{it.label}</td>
                          <td className="py-1 pr-3 text-gray-400">{it.kind}</td>
                          <td className="py-1 pr-3">{it.matchName}</td>
                          <td className="py-1">
                            <span
                              className={
                                it.status === 'AMBIGUOUS'
                                  ? 'text-orange-300'
                                  : 'text-yellow-300'
                              }
                            >
                              {it.status === 'AMBIGUOUS' ? '이름 중복' : '미매칭'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            ))}
            <p className="text-xs text-gray-500 mt-2">
              미매칭·중복 스텝은 링크 없이 저장됐습니다. 스텝 수정 API로 연결을 보정한 뒤 가이드를 공개하세요.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
