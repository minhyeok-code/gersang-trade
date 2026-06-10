'use client';

import { useState, useEffect, useCallback, useRef } from 'react';

const BASE = '';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

function setToken(token: string) {
  localStorage.setItem('accessToken', token);
}

/** AT 만료 시 RT 쿠키로 새 AT를 발급받는다. 실패 시 null 반환. */
async function tryRefresh(): Promise<string | null> {
  try {
    const res = await fetch(`${BASE}/auth/refresh`, { method: 'POST', credentials: 'include' });
    if (!res.ok) return null;
    const data = await res.json() as { accessToken?: string };
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

  // AT 만료(401/403) → 자동 갱신 후 1회 재시도
  if (res.status === 401 || res.status === 403) {
    const newToken = await tryRefresh();
    if (newToken) {
      res = await doFetch(newToken);
    }
  }

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${res.status}: ${text}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ───────────────────────── 공통 컴포넌트 ─────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-6 bg-gray-800 rounded p-4">
      <h2 className="text-lg font-bold text-yellow-400 mb-3">{title}</h2>
      {children}
    </div>
  );
}

function Btn({
  onClick,
  children,
  color = 'yellow',
  disabled,
}: {
  onClick: () => void;
  children: React.ReactNode;
  color?: 'yellow' | 'red' | 'green' | 'gray' | 'blue';
  disabled?: boolean;
}) {
  const cls: Record<string, string> = {
    yellow: 'bg-yellow-500 hover:bg-yellow-400 text-black',
    red: 'bg-red-600 hover:bg-red-500 text-white',
    green: 'bg-green-600 hover:bg-green-500 text-white',
    gray: 'bg-gray-600 hover:bg-gray-500 text-white',
    blue: 'bg-blue-600 hover:bg-blue-500 text-white',
  };
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`px-3 py-1.5 rounded text-sm font-medium ${cls[color]} disabled:opacity-50 disabled:cursor-not-allowed`}
    >
      {children}
    </button>
  );
}

function Input({
  value,
  onChange,
  placeholder,
  type = 'text',
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  type?: string;
}) {
  return (
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm text-gray-100 w-full"
    />
  );
}

function Toast({ msg, onClose }: { msg: string; onClose: () => void }) {
  useEffect(() => {
    const t = setTimeout(onClose, 3500);
    return () => clearTimeout(t);
  }, [onClose]);
  const isErr = msg.startsWith('오류');
  return (
    <div
      className={`fixed bottom-4 right-4 z-50 px-4 py-2 rounded shadow-lg text-sm ${
        isErr ? 'bg-red-700 text-white' : 'bg-green-700 text-white'
      }`}
    >
      {msg}
    </div>
  );
}

type EditableStat = {
  id?: number;
  statType: string;
  element?: string | null;
  value: string;
  /** 아이템 스탯 전용 — 용병 스탯 행에는 없음 */
  scope?: string;
};

type EditableEquipmentDetail = {
  slot: string;
  equipmentKind: string;
  setId: string;
  ritualApplicable: boolean;
  hasSlotOption: boolean;
  equipSlot: string;
};

type EditableLevel = {
  id?: number;
  label: string;
  level: string;
  amount: string;
  statType: string;
};

const STAT_TYPE_OPTIONS = [
  { value: 'ELEMENT_VALUE', label: '속성값' },
  { value: 'ELEMENT_PIERCE', label: '속성깎' },
  { value: 'RESIST_PIERCE', label: '저항깎' },
  { value: 'MIN_POWER', label: '최소공격력' },
  { value: 'MAX_POWER', label: '최대공격력' },
  { value: 'STRENGTH', label: '힘' },
  { value: 'VITALITY', label: '생명력' },
  { value: 'DEXTERITY', label: '민첩' },
  { value: 'INTELLECT', label: '지력' },
  { value: 'DEFENSE', label: '방어' },
  { value: 'SIGHT', label: '시야' },
  { value: 'HIT_RATE', label: '명중률' },
  { value: 'CRITICAL_CHANCE', label: '크리티컬확률' },
  { value: 'MAGIC_RESISTANCE', label: '마법저항' },
  { value: 'HITTING_RESISTANCE', label: '타격저항' },
  { value: 'MAGIC_RESISTANCE_PIERCE', label: '마법저항깎' },
  { value: 'HITTING_RESISTANCE_PIERCE', label: '타격저항깎' },
  { value: 'DAMAGE_PERCENT', label: '데미지증가' },
  { value: 'SKILL_DAMAGE_PERCENT', label: '스킬데미지증가' },
  { value: 'FIELD_MOVE_SPEED', label: '필드이동속도' },
  { value: 'ALL_STAT', label: '모든능력치' },
  { value: 'CRITICAL_RATE', label: '치명타확률' },
  { value: 'CRITICAL_DAMAGE', label: '치명타피해' },
  { value: 'MIN_DAMAGE', label: '최소데미지' },
  { value: 'MAX_DAMAGE', label: '최대데미지' },
  { value: 'ATTACK_SPEED', label: '공격속도' },
  { value: 'MOVE_SPEED', label: '이동속도' },
  { value: 'HP_RECOVERY', label: '체력회복' },
  { value: 'MP_RECOVERY', label: '마력회복' },
  { value: 'DAMAGE_PERCENT_GROUND', label: '지상데미지증가' },
  { value: 'DAMAGE_PERCENT_AIR', label: '공중데미지증가' },
  { value: 'STUN_DURATION', label: '기절시간' },
  { value: 'ATTACK_POWER', label: '공격력' },
  { value: 'SKILL_RANGE', label: '사거리' },
];

const BUFF_SCOPE_OPTIONS = [
  { value: 'SELF', label: '착용자 본인' },
  { value: 'ALLY', label: '아군 전체' },
  { value: 'ALLY_HEAVENLY_KING', label: '동속성 사천왕 (명왕부·명왕무기)' },
  { value: 'ALLY_SAME_ELEMENT', label: '동속성 아군 % (각성 명왕 무기)' },
  { value: 'ENEMY', label: '적 디버프' },
] as const;

const ELEMENT_OPTIONS = [
  { value: '', label: '없음 (NONE)' },
  { value: 'ADAPTIVE', label: '용병 속성 추종 (ADAPTIVE)' },
  { value: 'FIRE', label: '화 (FIRE)' },
  { value: 'WATER', label: '수 (WATER)' },
  { value: 'WIND', label: '풍 (WIND)' },
  { value: 'EARTH', label: '토 (EARTH)' },
  { value: 'THUNDER', label: '뇌 (THUNDER)' },
] as const;

function toText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value);
}

function inputClass(extra = '') {
  return `bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm text-gray-100 ${extra}`;
}

function SmallInput({
  value,
  onChange,
  placeholder,
  type = 'text',
  className = '',
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  type?: string;
  className?: string;
}) {
  return (
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className={inputClass(`w-full ${className}`)}
    />
  );
}

function StatTypeSelect({
  value,
  onChange,
  allowEmpty = false,
}: {
  value: string;
  onChange: (v: string) => void;
  allowEmpty?: boolean;
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={inputClass('w-full')}
    >
      {allowEmpty && <option value="">계산 제외</option>}
      {!allowEmpty && <option value="">선택</option>}
      {STAT_TYPE_OPTIONS.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label} ({option.value})
        </option>
      ))}
    </select>
  );
}

function ElementSelect({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  const normalized = value === 'NONE' ? '' : (value ?? '');
  return (
    <select
      value={normalized}
      onChange={(e) => onChange(e.target.value)}
      className={inputClass('w-full min-w-[9rem]')}
    >
      {ELEMENT_OPTIONS.map((option) => (
        <option key={option.value || 'NONE'} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

function BuffScopeSelect({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <select
      value={value || 'SELF'}
      onChange={(e) => onChange(e.target.value)}
      className={inputClass('w-full min-w-[10rem]')}
    >
      {BUFF_SCOPE_OPTIONS.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

function ValueTypeSelect({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={inputClass('w-full')}
    >
      <option value="FLAT">고정값</option>
      <option value="PERCENT">퍼센트(%)</option>
    </select>
  );
}

function EmptyRows({ colSpan, label }: { colSpan: number; label: string }) {
  return (
    <tr>
      <td colSpan={colSpan} className="text-center text-gray-500 py-4">
        {label}
      </td>
    </tr>
  );
}

// ───────────────────────── 크롤러 탭 ─────────────────────────

function CrawlerTab({ notify }: { notify: (m: string) => void }) {
  const [loading, setLoading] = useState<string | null>(null);

  async function trigger(path: string, label: string) {
    setLoading(path);
    try {
      const msg = await req<string>(`/admin/crawler/${path}`, { method: 'POST' });
      notify(`✅ ${label}: ${msg}`);
    } catch (e: unknown) {
      notify(`오류 - ${label}: ${(e as Error).message}`);
    } finally {
      setLoading(null);
    }
  }

  const jobs = [
    { path: 'master', label: '전체 수집 (아이템+용병+전용장비)', color: 'yellow' as const },
    { path: 'items', label: '장비/보석 수집', color: 'blue' as const },
    { path: 'materials', label: '잡화/소모품/재료 수집', color: 'blue' as const },
    { path: 'mercenaries', label: '용병 수집', color: 'blue' as const },
    { path: 'exclusive-equipment', label: '전용장비 수집', color: 'blue' as const },
    { path: 'sets', label: '장비 세트 수집', color: 'blue' as const },
    { path: 'rituals', label: '주술 수집', color: 'blue' as const },
    { path: 'monsters', label: '몬스터 수집', color: 'blue' as const },
    { path: 'price', label: '가격 데이터 수집', color: 'green' as const },
  ];

  return (
    <Section title="크롤링 Job 수동 트리거">
      <p className="text-xs text-gray-400 mb-3">
        배치 Job을 즉시 실행합니다. Job이 시작되면 백그라운드에서 실행되며 결과는 서버 로그에서 확인할 수 있습니다.
        전용장비 수집은 앱 시더·용병 수집 이후 실행하는 것을 권장합니다.
      </p>
      <div className="flex flex-wrap gap-3">
        {jobs.map((j) => (
          <Btn
            key={j.path}
            color={j.color}
            disabled={loading !== null}
            onClick={() => trigger(j.path, j.label)}
          >
            {loading === j.path ? '실행 중...' : j.label}
          </Btn>
        ))}
      </div>
    </Section>
  );
}

// ───────────────────────── 신고 관리 탭 ─────────────────────────

interface ReportItem {
  id: number;
  targetType: string;
  targetId: number;
  reason: string;
  status: string;
  reporterNickname: string;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

function ReportsTab({ notify }: { notify: (m: string) => void }) {
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [reason, setReason] = useState('');
  const [blockUserId, setBlockUserId] = useState('');
  const [blockReason, setBlockReason] = useState('');
  const [msgId, setMsgId] = useState('');

  const load = useCallback(async () => {
    try {
      const sp = new URLSearchParams({ page: String(page), size: '20' });
      if (status) sp.set('status', status);
      const data = await req<PageResponse<ReportItem>>(`/admin/reports?${sp}`);
      setReports(data.content);
      setTotal(data.totalElements);
    } catch (e: unknown) {
      notify(`오류 - 신고 목록: ${(e as Error).message}`);
    }
  }, [page, status, notify]);

  useEffect(() => { load(); }, [load]);

  async function patchReport(id: number, action: string, body?: object) {
    try {
      await req(`/admin/reports/${id}/${action}`, {
        method: 'PATCH',
        body: body ? JSON.stringify(body) : undefined,
      });
      notify(`✅ 신고 #${id} ${action} 완료`);
      load();
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  async function blockUser() {
    if (!blockUserId) return;
    try {
      await req(`/admin/users/${blockUserId}/block`, {
        method: 'POST',
        body: JSON.stringify({ reason: blockReason }),
      });
      notify(`✅ 사용자 #${blockUserId} 차단 완료`);
      setBlockUserId('');
      setBlockReason('');
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  async function unblockUser(userId: string) {
    try {
      await req(`/admin/users/${userId}/unblock`, { method: 'POST' });
      notify(`✅ 사용자 #${userId} 차단 해제`);
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  async function toggleMsg(id: string, action: 'hide' | 'unhide') {
    try {
      await req(`/admin/messages/${id}/${action}`, { method: 'PATCH' });
      notify(`✅ 메시지 #${id} ${action === 'hide' ? '숨김' : '숨김 해제'} 완료`);
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  const statusOptions = ['', 'PENDING', 'REVIEWING', 'PROCESSED', 'DISMISSED'];

  return (
    <div>
      <Section title="신고 목록">
        <div className="flex gap-2 mb-3 flex-wrap">
          <select
            value={status}
            onChange={(e) => { setStatus(e.target.value); setPage(0); }}
            className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm"
          >
            {statusOptions.map((s) => (
              <option key={s} value={s}>{s || '전체'}</option>
            ))}
          </select>
          <Btn color="gray" onClick={load}>새로고침</Btn>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="text-gray-400 border-b border-gray-700">
              <tr>
                <th className="text-left py-1 pr-3">ID</th>
                <th className="text-left py-1 pr-3">신고자</th>
                <th className="text-left py-1 pr-3">대상</th>
                <th className="text-left py-1 pr-3">사유</th>
                <th className="text-left py-1 pr-3">상태</th>
                <th className="text-left py-1 pr-3">일시</th>
                <th className="text-left py-1">액션</th>
              </tr>
            </thead>
            <tbody>
              {reports.length === 0 && (
                <tr><td colSpan={7} className="text-gray-500 py-4 text-center">신고 없음</td></tr>
              )}
              {reports.map((r) => (
                <tr key={r.id} className="border-b border-gray-700 hover:bg-gray-750">
                  <td className="py-1 pr-3 text-gray-300">{r.id}</td>
                  <td className="py-1 pr-3">{r.reporterNickname}</td>
                  <td className="py-1 pr-3 text-xs text-gray-400">{r.targetType}#{r.targetId}</td>
                  <td className="py-1 pr-3 max-w-xs truncate">{r.reason}</td>
                  <td className="py-1 pr-3">
                    <span className={`px-1.5 py-0.5 rounded text-xs ${
                      r.status === 'PENDING' ? 'bg-yellow-700' :
                      r.status === 'REVIEWING' ? 'bg-blue-700' :
                      r.status === 'PROCESSED' ? 'bg-green-700' : 'bg-gray-600'
                    }`}>{r.status}</span>
                  </td>
                  <td className="py-1 pr-3 text-xs text-gray-400">{r.createdAt?.slice(0, 10)}</td>
                  <td className="py-1">
                    <div className="flex gap-1 flex-wrap">
                      {r.status === 'PENDING' && (
                        <Btn color="blue" onClick={() => patchReport(r.id, 'review')}>검토</Btn>
                      )}
                      {r.status === 'REVIEWING' && processingId !== r.id && (
                        <>
                          <Btn color="green" onClick={() => setProcessingId(r.id)}>처리</Btn>
                          <Btn color="gray" onClick={() => setProcessingId(-(r.id))}>기각</Btn>
                        </>
                      )}
                      {(processingId === r.id || processingId === -(r.id)) && (
                        <div className="flex gap-1 items-center">
                          <Input value={reason} onChange={setReason} placeholder="사유 입력" />
                          <Btn
                            color={processingId > 0 ? 'green' : 'gray'}
                            onClick={async () => {
                              await patchReport(
                                Math.abs(processingId!),
                                processingId! > 0 ? 'process' : 'dismiss',
                                { reason }
                              );
                              setProcessingId(null);
                              setReason('');
                            }}
                          >
                            확인
                          </Btn>
                          <Btn color="gray" onClick={() => { setProcessingId(null); setReason(''); }}>취소</Btn>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {total > 20 && (
          <div className="flex gap-2 mt-3 justify-center">
            <Btn color="gray" disabled={page === 0} onClick={() => setPage(p => p - 1)}>이전</Btn>
            <span className="text-sm text-gray-400 self-center">{page + 1} / {Math.ceil(total / 20)}</span>
            <Btn color="gray" disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>다음</Btn>
          </div>
        )}
      </Section>

      <Section title="사용자 차단 / 해제">
        <div className="flex gap-2 flex-wrap items-end">
          <div className="w-32">
            <label className="text-xs text-gray-400 block mb-1">사용자 ID</label>
            <Input value={blockUserId} onChange={setBlockUserId} placeholder="userId" />
          </div>
          <div className="flex-1 min-w-48">
            <label className="text-xs text-gray-400 block mb-1">차단 사유</label>
            <Input value={blockReason} onChange={setBlockReason} placeholder="사유 (선택)" />
          </div>
          <Btn color="red" onClick={blockUser}>차단</Btn>
          <Btn color="gray" onClick={() => unblockUser(blockUserId)}>차단 해제</Btn>
        </div>
      </Section>

      <Section title="채팅 메시지 숨김">
        <div className="flex gap-2 flex-wrap items-end">
          <div className="w-32">
            <label className="text-xs text-gray-400 block mb-1">메시지 ID</label>
            <Input value={msgId} onChange={setMsgId} placeholder="messageId" />
          </div>
          <Btn color="red" onClick={() => toggleMsg(msgId, 'hide')}>숨김</Btn>
          <Btn color="gray" onClick={() => toggleMsg(msgId, 'unhide')}>숨김 해제</Btn>
        </div>
      </Section>
    </div>
  );
}

// ───────────────────────── 등록글 관리 탭 ─────────────────────────

function ListingsTab({ notify }: { notify: (m: string) => void }) {
  const [listingId, setListingId] = useState('');

  async function toggle(action: 'hide' | 'unhide') {
    if (!listingId) return;
    try {
      await req(`/admin/listings/${listingId}/${action}`, { method: 'PATCH' });
      notify(`✅ 등록글 #${listingId} ${action === 'hide' ? '숨김' : '숨김 해제'} 완료`);
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  return (
    <Section title="거래 등록글 숨김 관리">
      <p className="text-xs text-gray-400 mb-3">
        정책 위반 등록글을 숨김 처리합니다. 판매자가 직접 취소한 것과 구분됩니다.
      </p>
      <div className="flex gap-2 flex-wrap items-end">
        <div className="w-40">
          <label className="text-xs text-gray-400 block mb-1">등록글 ID</label>
          <Input value={listingId} onChange={setListingId} placeholder="listingId" />
        </div>
        <Btn color="red" onClick={() => toggle('hide')}>숨김 처리</Btn>
        <Btn color="gray" onClick={() => toggle('unhide')}>숨김 해제</Btn>
      </div>
    </Section>
  );
}

// ───────────────────────── 아이템 관리 탭 ─────────────────────────

interface ItemRow {
  id: number;
  name: string;
  type: string;
  statCount: number;
}

interface ItemDetail {
  id: number;
  name: string;
  type: string;
  tradeCategory?: string;
  equipment?: {
    slot: string;
    equipmentKind: string;
    kind?: string;
    setId?: number | null;
    ritualApplicable: boolean;
    hasSlotOption: boolean;
    equipSlot?: string | null;
  } | null;
  equipmentDetail?: {
    slot: string;
    kind?: string;
    equipmentKind?: string;
    setId: number | null;
    ritualApplicable: boolean;
    hasSlotOption: boolean;
    equipSlot?: string | null;
  };
  stats: { id?: number; statType: string; element?: string | null; value: number; scope?: string | null }[];
  skills: { id: number | null; skillName: string; effects: { statKey: string; statValue: number; valueType?: string }[] }[];
}

interface ItemSkillRow {
  id: number | null;
  skillName: string;
  effects: { statKey: string; statValue: string; valueType: string }[];
}

interface CleanupCandidate {
  id: number;
  name: string;
  type: string;
  slot?: string | null;
  setName?: string | null;
  statCount: number;
  matchedCriteria: string[];
}

const CLEANUP_CRITERIA = [
  { id: 'UNSUPPORTED_SLOT', label: '미지원 부위(보주/날개/칭호)' },
  { id: 'NO_EQUIP_SLOT', label: '덱 착용 슬롯 없음(반지 제외)' },
  { id: 'NON_TRADEABLE_SET', label: '비거래 세트 소속' },
  { id: 'NOT_SET_PIECE', label: '세트 피스 미등록' },
  { id: 'NO_STATS', label: '스탯 없음' },
  { id: 'UNREFERENCED', label: '미참조(거래/덱/시세 없음)' },
] as const;

const CLEANUP_CRITERIA_LABEL: Record<string, string> = Object.fromEntries(
  CLEANUP_CRITERIA.map((c) => [c.id, c.label]),
);

function ItemsTab({ notify }: { notify: (m: string) => void }) {
  const [items, setItems] = useState<ItemRow[]>([]);
  const [filterType, setFilterType] = useState('');
  const [filterName, setFilterName] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<ItemDetail | null>(null);
  const [editInfo, setEditInfo] = useState({ name: '', type: '', tradeCategory: '' });
  const [itemStats, setItemStats] = useState<EditableStat[]>([]);
  const [itemSkills, setItemSkills] = useState<ItemSkillRow[]>([]);
  const [eqDetail, setEqDetail] = useState<EditableEquipmentDetail>({
    slot: '',
    equipmentKind: '',
    setId: '',
    ritualApplicable: false,
    hasSlotOption: false,
    equipSlot: '',
  });
  const [cleanupCriteria, setCleanupCriteria] = useState<string[]>([
    'UNSUPPORTED_SLOT',
    'NO_EQUIP_SLOT',
    'UNREFERENCED',
  ]);
  const [cleanupCandidates, setCleanupCandidates] = useState<CleanupCandidate[]>([]);
  const [cleanupSelectedIds, setCleanupSelectedIds] = useState<Set<number>>(new Set());
  const [cleanupScanning, setCleanupScanning] = useState(false);

  const load = useCallback(async () => {
    try {
      const sp = new URLSearchParams({ page: String(page), size: '50', sort: 'name,asc' });
      if (filterType) sp.set('type', filterType);
      if (filterName) sp.set('name', filterName);
      const data = await req<PageResponse<ItemRow>>(`/admin/items?${sp}`);
      setItems(data.content);
      setTotal(data.totalElements);
    } catch (e: unknown) {
      notify(`오류 - 아이템 목록: ${(e as Error).message}`);
    }
  }, [page, filterType, filterName, notify]);

  useEffect(() => { load(); }, [load]);

  async function selectItem(id: number) {
    try {
      const data = await req<ItemDetail>(`/admin/items/${id}`);
      setSelected(data);
      setEditInfo({ name: data.name, type: data.type, tradeCategory: data.tradeCategory ?? '' });
      setItemStats((data.stats ?? []).map((stat) => ({
        id: stat.id,
        statType: stat.statType,
        element: stat.element ?? '',
        value: toText(stat.value),
        scope: stat.scope ?? 'SELF',
      })));
      setItemSkills((data.skills ?? []).map(s => ({
        id: s.id,
        skillName: s.skillName,
        effects: (s.effects ?? []).map(e => ({ statKey: e.statKey, statValue: String(e.statValue), valueType: e.valueType ?? 'FLAT' })),
      })));
      const equipment = data.equipmentDetail ?? data.equipment;
      setEqDetail({
        slot: equipment?.slot ?? '',
        equipmentKind: equipment?.equipmentKind ?? equipment?.kind ?? '',
        setId: toText(equipment?.setId),
        ritualApplicable: equipment?.ritualApplicable ?? false,
        hasSlotOption: equipment?.hasSlotOption ?? false,
        equipSlot: toText(equipment?.equipSlot),
      });
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  async function saveInfo() {
    if (!selected) return;
    try {
      await req(`/admin/items/${selected.id}`, {
        method: 'PUT',
        body: JSON.stringify(editInfo),
      });
      notify('✅ 기본정보 저장 완료');
      load();
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveStats() {
    if (!selected) return;
    try {
      const stats = itemStats
        .filter((stat) => stat.statType.trim() && stat.value.trim())
        .map((stat) => ({
          statType: stat.statType.trim(),
          element: stat.element?.trim() || null,
          value: Number(stat.value),
          scope: stat.scope?.trim() || 'SELF',
        }));
      await req(`/admin/items/${selected.id}/stats`, { method: 'PUT', body: JSON.stringify({ stats }) });
      notify('✅ 스탯 저장 완료');
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveSkills() {
    if (!selected) return;
    try {
      const skills = itemSkills.map(s => s.skillName.trim()).filter(Boolean);
      await req(`/admin/items/${selected.id}/skills`, { method: 'PUT', body: JSON.stringify({ skills }) });
      notify('✅ 스킬 저장 (효과는 초기화됨)');
      await selectItem(selected.id);
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveItemSkillEffects(skillId: number, effects: { statKey: string; statValue: string; valueType: string }[]) {
    if (!selected) return;
    try {
      const body = {
        effects: effects
          .filter(e => e.statKey)
          .map(e => ({ statKey: e.statKey, statValue: Number(e.statValue), valueType: e.valueType || 'FLAT' })),
      };
      await req(`/admin/items/${selected.id}/skills/${skillId}/effects`, { method: 'PUT', body: JSON.stringify(body) });
      notify('✅ 스킬 효과 저장');
      await selectItem(selected.id);
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveEqDetail() {
    if (!selected) return;
    try {
      const body = {
        slot: eqDetail.slot || null,
        equipmentKind: eqDetail.equipmentKind || null,
        setId: eqDetail.setId ? Number(eqDetail.setId) : null,
        ritualApplicable: eqDetail.ritualApplicable,
        hasSlotOption: eqDetail.hasSlotOption,
        equipSlot: eqDetail.equipSlot || null,
      };
      await req(`/admin/items/${selected.id}/equipment-detail`, { method: 'PUT', body: JSON.stringify(body) });
      notify('✅ 장비 상세 저장 완료');
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function deleteItem() {
    if (!selected) return;
    const ok = window.confirm(`아이템 "${selected.name}"을(를) DB에서 실제 삭제할까요?\n거래/덱 등에서 참조 중이면 삭제되지 않습니다.`);
    if (!ok) return;

    try {
      await req(`/admin/items/${selected.id}`, { method: 'DELETE' });
      notify(`✅ 아이템 #${selected.id} 삭제 완료`);
      setSelected(null);
      setItemStats([]);
      setItemSkills([]);
      await load();
    } catch (e: unknown) {
      notify(`오류 - 아이템 삭제: ${(e as Error).message}`);
    }
  }

  function toggleCleanupCriterion(id: string) {
    setCleanupCriteria((prev) =>
      prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id],
    );
  }

  async function scanCleanupCandidates() {
    setCleanupScanning(true);
    try {
      const sp = new URLSearchParams();
      cleanupCriteria.forEach((c) => sp.append('criteria', c));
      const data = await req<CleanupCandidate[]>(`/admin/items/cleanup-candidates?${sp}`);
      setCleanupCandidates(data);
      setCleanupSelectedIds(new Set(data.map((c) => c.id)));
      notify(`✅ 정리 후보 ${data.length}건 조회`);
    } catch (e: unknown) {
      notify(`오류 - 정리 후보 조회: ${(e as Error).message}`);
    } finally {
      setCleanupScanning(false);
    }
  }

  function toggleCleanupRow(id: number) {
    setCleanupSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleCleanupSelectAll(checked: boolean) {
    setCleanupSelectedIds(checked ? new Set(cleanupCandidates.map((c) => c.id)) : new Set());
  }

  async function bulkDeleteCleanupSelected() {
    const ids = Array.from(cleanupSelectedIds);
    if (ids.length === 0) {
      notify('삭제할 항목을 선택하세요');
      return;
    }
    const ok = window.confirm(
      `선택한 ${ids.length}개 아이템을 DB에서 삭제할까요?\n거래/덱 등에서 참조 중인 항목은 건너뜁니다.`,
    );
    if (!ok) return;

    try {
      const res = await req<{ deletedCount: number; deletedIds: number[]; failed: { itemId: number; reason: string }[] }>(
        '/admin/items/bulk-delete',
        { method: 'POST', body: JSON.stringify({ itemIds: ids }) },
      );
      const failMsg = res.failed.length > 0
        ? ` / 실패 ${res.failed.length}건`
        : '';
      notify(`✅ 삭제 완료 ${res.deletedCount}건${failMsg}`);
      setCleanupCandidates((prev) => prev.filter((c) => !res.deletedIds.includes(c.id)));
      setCleanupSelectedIds((prev) => {
        const next = new Set(prev);
        res.deletedIds.forEach((id) => next.delete(id));
        return next;
      });
      if (selected && res.deletedIds.includes(selected.id)) {
        setSelected(null);
        setItemStats([]);
        setItemSkills([]);
      }
      await load();
    } catch (e: unknown) {
      notify(`오류 - 일괄 삭제: ${(e as Error).message}`);
    }
  }

  return (
    <div>
    <Section title="크롤링 정리 — 불필요 아이템 검색·삭제">
      <p className="text-xs text-gray-400 mb-3">
        선택한 기준 중 하나라도 해당하면 후보에 표시됩니다. 삭제 전 목록을 반드시 확인하세요.
      </p>
      <div className="flex flex-wrap gap-3 mb-3">
        {CLEANUP_CRITERIA.map((c) => (
          <label key={c.id} className="flex items-center gap-1.5 text-sm">
            <input
              type="checkbox"
              checked={cleanupCriteria.includes(c.id)}
              onChange={() => toggleCleanupCriterion(c.id)}
            />
            {c.label}
          </label>
        ))}
      </div>
      <div className="flex gap-2 mb-3">
        <Btn color="blue" onClick={scanCleanupCandidates} disabled={cleanupScanning || cleanupCriteria.length === 0}>
          {cleanupScanning ? '검색 중…' : '후보 검색'}
        </Btn>
        <Btn
          color="red"
          onClick={bulkDeleteCleanupSelected}
          disabled={cleanupSelectedIds.size === 0}
        >
          선택 삭제 ({cleanupSelectedIds.size})
        </Btn>
      </div>
      {cleanupCandidates.length > 0 && (
        <div className="overflow-x-auto max-h-64 overflow-y-auto border border-gray-700 rounded">
          <table className="w-full text-sm">
            <thead className="text-gray-400 border-b border-gray-700 sticky top-0 bg-gray-800">
              <tr>
                <th className="py-1 px-2 text-left w-8">
                  <input
                    type="checkbox"
                    checked={cleanupSelectedIds.size === cleanupCandidates.length && cleanupCandidates.length > 0}
                    onChange={(e) => toggleCleanupSelectAll(e.target.checked)}
                  />
                </th>
                <th className="py-1 px-2 text-left">이름</th>
                <th className="py-1 px-2 text-left">타입</th>
                <th className="py-1 px-2 text-left">슬롯</th>
                <th className="py-1 px-2 text-left">세트</th>
                <th className="py-1 px-2 text-left">스탯</th>
                <th className="py-1 px-2 text-left">해당 기준</th>
              </tr>
            </thead>
            <tbody>
              {cleanupCandidates.map((c) => (
                <tr key={c.id} className="border-b border-gray-700 hover:bg-gray-700/50">
                  <td className="py-1 px-2">
                    <input
                      type="checkbox"
                      checked={cleanupSelectedIds.has(c.id)}
                      onChange={() => toggleCleanupRow(c.id)}
                    />
                  </td>
                  <td className="py-1 px-2">
                    <button
                      type="button"
                      className="text-left hover:text-yellow-300"
                      onClick={() => selectItem(c.id)}
                    >
                      {c.name}
                    </button>
                  </td>
                  <td className="py-1 px-2 text-gray-400">{c.type}</td>
                  <td className="py-1 px-2 text-gray-400">{c.slot ?? '-'}</td>
                  <td className="py-1 px-2 text-gray-400">{c.setName ?? '-'}</td>
                  <td className="py-1 px-2 text-gray-400">{c.statCount}</td>
                  <td className="py-1 px-2 text-xs text-orange-300">
                    {c.matchedCriteria.map((k) => CLEANUP_CRITERIA_LABEL[k] ?? k).join(', ')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Section>

    <div className="flex gap-4">
      {/* 목록 */}
      <div className="w-72 shrink-0">
        <Section title="아이템 목록">
          <div className="flex gap-1 mb-2">
            <select
              value={filterType}
              onChange={(e) => { setFilterType(e.target.value); setPage(0); }}
              className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-xs flex-1"
            >
              <option value="">전체</option>
              <option value="MATERIAL">재료</option>
              <option value="EQUIPMENT">장비</option>
            </select>
          </div>
          <div className="mb-2">
            <Input value={filterName} onChange={(v) => { setFilterName(v); setPage(0); }} placeholder="이름 검색" />
          </div>
          <Btn color="gray" onClick={load}>검색</Btn>
          <p className="text-xs text-gray-400 mt-1">총 {total}개</p>
          <ul className="mt-2 max-h-96 overflow-y-auto divide-y divide-gray-700">
            {items.map((i) => (
              <li
                key={i.id}
                onClick={() => selectItem(i.id)}
                className={`py-1.5 px-1 cursor-pointer text-sm hover:bg-gray-700 ${selected?.id === i.id ? 'bg-gray-700 text-yellow-300' : ''}`}
              >
                <span className="font-medium">{i.name}</span>
                <span className="ml-1 text-xs text-gray-400">({i.type})</span>
                {i.statCount === 0 && <span className="ml-1 text-xs text-red-400">스탯없음</span>}
              </li>
            ))}
          </ul>
          {total > 50 && (
            <div className="flex gap-1 mt-2">
              <Btn color="gray" disabled={page === 0} onClick={() => setPage(p => p - 1)}>이전</Btn>
              <Btn color="gray" disabled={(page + 1) * 50 >= total} onClick={() => setPage(p => p + 1)}>다음</Btn>
            </div>
          )}
        </Section>
      </div>

      {/* 상세 편집 */}
      <div className="flex-1 min-w-0">
        {!selected ? (
          <p className="text-gray-500 mt-8 text-center">아이템을 선택하세요</p>
        ) : (
          <div>
            <Section title={`아이템 #${selected.id} — 기본정보`}>
              <div className="grid grid-cols-2 gap-2 mb-2">
                <div>
                  <label className="text-xs text-gray-400 block mb-1">이름</label>
                  <Input value={editInfo.name} onChange={(v) => setEditInfo(p => ({ ...p, name: v }))} />
                </div>
                <div>
                  <label className="text-xs text-gray-400 block mb-1">타입</label>
                  <select
                    value={editInfo.type}
                    onChange={(e) => setEditInfo(p => ({ ...p, type: e.target.value }))}
                    className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm w-full"
                  >
                    <option value="MATERIAL">MATERIAL</option>
                    <option value="EQUIPMENT">EQUIPMENT</option>
                  </select>
                </div>
              </div>
              <div className="flex gap-2">
                <Btn color="yellow" onClick={saveInfo}>저장</Btn>
                <Btn color="red" onClick={deleteItem}>아이템 삭제</Btn>
              </div>
            </Section>

            {selected.type === 'EQUIPMENT' && (
              <Section title="장비 상세">
                <div className="grid grid-cols-2 lg:grid-cols-3 gap-3 mb-3">
                  <div>
                    <label className="text-xs text-gray-400 block mb-1">장비 슬롯</label>
                    <SmallInput value={eqDetail.slot} onChange={(v) => setEqDetail(p => ({ ...p, slot: v }))} placeholder="WEAPON" />
                  </div>
                  <div>
                    <label className="text-xs text-gray-400 block mb-1">장비 종류</label>
                    <SmallInput value={eqDetail.equipmentKind} onChange={(v) => setEqDetail(p => ({ ...p, equipmentKind: v }))} placeholder="NORMAL" />
                  </div>
                  <div>
                    <label className="text-xs text-gray-400 block mb-1">세트 ID</label>
                    <SmallInput value={eqDetail.setId} onChange={(v) => setEqDetail(p => ({ ...p, setId: v }))} placeholder="미소속이면 공백" type="number" />
                  </div>
                  <div>
                    <label className="text-xs text-gray-400 block mb-1">덱 장착 슬롯</label>
                    <SmallInput value={eqDetail.equipSlot} onChange={(v) => setEqDetail(p => ({ ...p, equipSlot: v }))} placeholder="미설정이면 공백" />
                  </div>
                  <label className="flex items-center gap-2 text-sm mt-5">
                    <input type="checkbox" checked={eqDetail.ritualApplicable} onChange={(e) => setEqDetail(p => ({ ...p, ritualApplicable: e.target.checked }))} />
                    주술 가능
                  </label>
                  <label className="flex items-center gap-2 text-sm mt-5">
                    <input type="checkbox" checked={eqDetail.hasSlotOption} onChange={(e) => setEqDetail(p => ({ ...p, hasSlotOption: e.target.checked }))} />
                    슬롯 옵션 있음
                  </label>
                </div>
                <Btn color="yellow" onClick={saveEqDetail}>저장</Btn>
              </Section>
            )}

            <Section title="스탯">
              <div className="overflow-x-auto mb-3">
                <table className="w-full text-sm">
                  <thead className="text-gray-400 border-b border-gray-700">
                    <tr>
                      <th className="text-left py-1 pr-2">스탯 타입</th>
                      <th className="text-left py-1 pr-2">속성</th>
                      <th className="text-left py-1 pr-2">값</th>
                      <th className="text-left py-1 pr-2">적용 범위</th>
                      <th className="text-left py-1">액션</th>
                    </tr>
                  </thead>
                  <tbody>
                    {itemStats.length === 0 && <EmptyRows colSpan={5} label="스탯 없음" />}
                    {itemStats.map((stat, idx) => (
                      <tr key={stat.id ?? idx} className="border-b border-gray-700">
                        <td className="py-1 pr-2">
                          <StatTypeSelect value={stat.statType} onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, statType: v } : row))} />
                        </td>
                        <td className="py-1 pr-2">
                          <ElementSelect
                            value={stat.element ?? ''}
                            onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, element: v } : row))}
                          />
                        </td>
                        <td className="py-1 pr-2 w-28">
                          <SmallInput value={stat.value} onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, value: v } : row))} type="number" />
                        </td>
                        <td className="py-1 pr-2">
                          <BuffScopeSelect
                            value={stat.scope ?? 'SELF'}
                            onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, scope: v } : row))}
                          />
                        </td>
                        <td className="py-1">
                          <Btn color="red" onClick={() => setItemStats(rows => rows.filter((_, i) => i !== idx))}>삭제</Btn>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex gap-2">
                <Btn color="green" onClick={() => setItemStats(rows => [...rows, { statType: '', element: '', value: '', scope: 'SELF' }])}>행 추가</Btn>
                <Btn color="yellow" onClick={saveStats}>저장 (전체 교체)</Btn>
              </div>
            </Section>

            <Section title="스킬">
              <div className="space-y-2 mb-3">
                {itemSkills.length === 0 && <p className="text-gray-500 text-sm">스킬 없음</p>}
                {itemSkills.map((skill, idx) => (
                  <div key={skill.id ?? `new-${idx}`} className="border border-gray-700 rounded p-2">
                    <div className="flex items-center gap-2 mb-2">
                      <SmallInput
                        value={skill.skillName}
                        onChange={(v) => setItemSkills(rows => rows.map((r, i) => i === idx ? { ...r, skillName: v } : r))}
                        placeholder="스킬명"
                      />
                      <Btn color="red" onClick={() => setItemSkills(rows => rows.filter((_, i) => i !== idx))}>삭제</Btn>
                    </div>
                    {skill.id != null && (
                      <div className="ml-2 border-l border-gray-600 pl-2">
                        <p className="text-xs text-gray-400 mb-1">스킬 효과 (저항깎·속성깎)</p>
                        {skill.effects.map((eff, ei) => (
                          <div key={ei} className="flex items-center gap-1 mb-1">
                            <StatTypeSelect
                              value={eff.statKey}
                              onChange={(v) => setItemSkills(rows => rows.map((r, i) =>
                                i === idx ? { ...r, effects: r.effects.map((e, j) => j === ei ? { ...e, statKey: v } : e) } : r
                              ))}
                            />
                            <SmallInput
                              value={eff.statValue}
                              onChange={(v) => setItemSkills(rows => rows.map((r, i) =>
                                i === idx ? { ...r, effects: r.effects.map((e, j) => j === ei ? { ...e, statValue: v } : e) } : r
                              ))}
                              type="number"
                              placeholder="수치"
                              className="w-24"
                            />
                            <div className="w-28 shrink-0">
                              <ValueTypeSelect
                                value={eff.valueType}
                                onChange={(v) => setItemSkills(rows => rows.map((r, i) =>
                                  i === idx ? { ...r, effects: r.effects.map((e, j) => j === ei ? { ...e, valueType: v } : e) } : r
                                ))}
                              />
                            </div>
                            <Btn color="red" onClick={() => setItemSkills(rows => rows.map((r, i) =>
                              i === idx ? { ...r, effects: r.effects.filter((_, j) => j !== ei) } : r
                            ))}>삭제</Btn>
                          </div>
                        ))}
                        <div className="flex gap-2 mt-1">
                          <Btn color="gray" onClick={() => setItemSkills(rows => rows.map((r, i) =>
                            i === idx ? { ...r, effects: [...r.effects, { statKey: '', statValue: '', valueType: 'FLAT' }] } : r
                          ))}>효과 추가</Btn>
                          <Btn color="blue" onClick={() => saveItemSkillEffects(skill.id!, skill.effects)}>효과 저장</Btn>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
              <div className="flex gap-2">
                <Btn color="green" onClick={() => setItemSkills(rows => [...rows, { id: null, skillName: '', effects: [] }])}>스킬 추가</Btn>
                <Btn color="yellow" onClick={saveSkills}>저장 (전체 교체)</Btn>
              </div>
            </Section>
          </div>
        )}
      </div>
    </div>
    </div>
  );
}

// ───────────────────────── 용병 관리 탭 ─────────────────────────

interface MercenaryRow {
  id: number;
  name: string;
  nature: string;
  nation: string;
  characteristicCount: number;
}

interface CharRow {
  id: number;
  name: string;
  point?: number | null;
  description?: string | null;
  requiredCharacteristicId: number | null;
  levels: { id?: number; label: string; level: number; amount: string; statType?: string | null }[];
}

interface SkillEffect { statKey: string; statValue: string; valueType: string; }
interface MercSkillRow { id: number | null; skillName: string; effects: SkillEffect[]; }

function MercenariesTab({ notify }: { notify: (m: string) => void }) {
  const [mercs, setMercs] = useState<MercenaryRow[]>([]);
  const [filterNature, setFilterNature] = useState('');
  const [filterNation, setFilterNation] = useState('');
  const [filterName, setFilterName] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [chars, setChars] = useState<CharRow[]>([]);
  const [mercStats, setMercStats] = useState<EditableStat[]>([]);
  const [mercSkills, setMercSkills] = useState<MercSkillRow[]>([]);
  const [bulkIds, setBulkIds] = useState('');
  const [bulkNature, setBulkNature] = useState('');
  const [bulkNation, setBulkNation] = useState('');
  // 특성 편집
  const [newChar, setNewChar] = useState({ name: '', point: '1', description: '', requiredCharacteristicId: '' });
  const [charLevels, setCharLevels] = useState<Record<number, EditableLevel[]>>({});

  const load = useCallback(async () => {
    try {
      const sp = new URLSearchParams({ page: String(page), size: '30', sort: 'name,asc' });
      if (filterNature) sp.set('nature', filterNature);
      if (filterNation) sp.set('nation', filterNation);
      if (filterName.trim()) sp.set('name', filterName.trim());
      const data = await req<PageResponse<MercenaryRow>>(`/admin/mercenaries?${sp}`);
      setMercs(data.content);
      setTotal(data.totalElements);
    } catch (e: unknown) {
      notify(`오류 - 용병 목록: ${(e as Error).message}`);
    }
  }, [page, filterNature, filterNation, filterName, notify]);

  useEffect(() => { load(); }, [load]);

  async function selectMerc(id: number) {
    setSelectedId(id);
    try {
      const [detail, charList] = await Promise.all([
        req<{ stats: { id?: number; statType: string; value: number }[]; skills: { id: number; skillName: string; effects: { statKey: string; statValue: number; valueType?: string }[] }[] }>(`/admin/mercenaries/${id}`),
        req<CharRow[]>(`/admin/mercenaries/${id}/characteristics`),
      ]);
      setMercStats((detail.stats ?? []).map((stat) => ({
        id: stat.id,
        statType: stat.statType,
        value: toText(stat.value),
      })));
      setMercSkills((detail.skills ?? []).map(s => ({
        id: s.id,
        skillName: s.skillName,
        effects: (s.effects ?? []).map(e => ({ statKey: e.statKey, statValue: String(e.statValue), valueType: e.valueType ?? 'FLAT' })),
      })));
      setChars(charList);
      const lvMap: Record<number, EditableLevel[]> = {};
      charList.forEach((c) => {
        lvMap[c.id] = (c.levels ?? []).map((level) => ({
          id: level.id,
          label: level.label ?? '',
          level: toText(level.level),
          amount: level.amount ?? '',
          statType: level.statType ?? '',
        }));
      });
      setCharLevels(lvMap);
    } catch (e: unknown) {
      notify(`오류 - ${(e as Error).message}`);
    }
  }

  async function saveStats() {
    if (!selectedId) return;
    try {
      const stats = mercStats
        .filter((stat) => stat.statType.trim() && stat.value.trim())
        .map((stat) => ({ statType: stat.statType.trim(), value: Number(stat.value) }));
      await req(`/admin/mercenaries/${selectedId}/stats`, { method: 'PUT', body: JSON.stringify({ stats }) });
      notify('✅ 용병 스탯 저장');
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveSkills() {
    if (!selectedId) return;
    try {
      const skills = mercSkills.map(s => s.skillName.trim()).filter(Boolean);
      await req(`/admin/mercenaries/${selectedId}/skills`, { method: 'PUT', body: JSON.stringify({ skills }) });
      notify('✅ 용병 스킬 저장 (효과는 초기화됨 — 스킬별 효과를 다시 저장하세요)');
      await selectMerc(selectedId);
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveSkillEffects(skillId: number, effects: SkillEffect[]) {
    if (!selectedId) return;
    try {
      const body = { effects: effects.filter(e => e.statKey).map(e => ({ statKey: e.statKey, statValue: Number(e.statValue), valueType: e.valueType || 'FLAT' })) };
      await req(`/admin/mercenaries/${selectedId}/skills/${skillId}/effects`, { method: 'PUT', body: JSON.stringify(body) });
      notify('✅ 스킬 효과 저장');
      await selectMerc(selectedId);
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function bulkUpdate() {
    try {
      const ids = bulkIds.split(',').map((s) => parseInt(s.trim())).filter(Boolean);
      const body: Record<string, unknown> = { ids };
      if (bulkNature) body.nature = bulkNature;
      if (bulkNation) body.nation = bulkNation;
      const res = await req<{ updated: number }>('/admin/mercenaries/bulk', { method: 'PATCH', body: JSON.stringify(body) });
      notify(`✅ 대량 변경 완료: ${res.updated}개`);
      load();
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function addChar() {
    if (!selectedId) return;
    try {
      const body = {
        name: newChar.name.trim(),
        point: newChar.point ? Number(newChar.point) : null,
        description: newChar.description.trim() || null,
        requiredCharacteristicId: newChar.requiredCharacteristicId ? Number(newChar.requiredCharacteristicId) : null,
      };
      await req(`/admin/mercenaries/${selectedId}/characteristics`, { method: 'POST', body: JSON.stringify(body) });
      notify('✅ 특성 추가');
      setNewChar({ name: '', point: '1', description: '', requiredCharacteristicId: '' });
      selectMerc(selectedId);
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function deleteChar(charId: number) {
    if (!selectedId) return;
    try {
      await req(`/admin/mercenaries/${selectedId}/characteristics/${charId}`, { method: 'DELETE' });
      notify('✅ 특성 삭제');
      selectMerc(selectedId);
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveLevels(charId: number) {
    if (!selectedId) return;
    try {
      const levels = (charLevels[charId] ?? [])
        .filter((level) => level.label.trim() && level.level.trim() && level.amount.trim())
        .map((level) => ({
          label: level.label.trim(),
          level: Number(level.level),
          amount: level.amount.trim(),
          statType: level.statType.trim() || null,
        }));
      await req(`/admin/mercenaries/${selectedId}/characteristics/${charId}/levels`, {
        method: 'PUT',
        body: JSON.stringify({ levels }),
      });
      notify('✅ 레벨 저장');
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  const natures = ['', 'FIRE', 'WATER', 'THUNDER', 'AIR', 'EARTH', 'NONE'];
  const nations = ['', 'JOSEON', 'CHINA', 'JAPAN', 'TAIWAN', 'INDIA', 'MONGOL', 'NONE'];
  const selectedMercenary = mercs.find((m) => m.id === selectedId);

  async function deleteMercenary() {
    if (!selectedId) return;
    const label = selectedMercenary?.name ?? `#${selectedId}`;
    const ok = window.confirm(`용병 "${label}"을(를) DB에서 실제 삭제할까요?\n덱/장비/재료 등에서 참조 중이면 삭제되지 않습니다.`);
    if (!ok) return;

    try {
      await req(`/admin/mercenaries/${selectedId}`, { method: 'DELETE' });
      notify(`✅ 용병 #${selectedId} 삭제 완료`);
      setSelectedId(null);
      setChars([]);
      setCharLevels({});
      setMercStats([]);
      setMercSkills([]);
      await load();
    } catch (e: unknown) {
      notify(`오류 - 용병 삭제: ${(e as Error).message}`);
    }
  }

  return (
    <div>
      <Section title="대량 nature/nation 변경">
        <div className="flex gap-2 flex-wrap items-end">
          <div className="flex-1 min-w-48">
            <label className="text-xs text-gray-400 block mb-1">용병 ID (쉼표 구분)</label>
            <Input value={bulkIds} onChange={setBulkIds} placeholder="1,2,3" />
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1">Nature</label>
            <select value={bulkNature} onChange={(e) => setBulkNature(e.target.value)}
              className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm">
              {natures.map((n) => <option key={n} value={n}>{n || '변경안함'}</option>)}
            </select>
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1">Nation</label>
            <select value={bulkNation} onChange={(e) => setBulkNation(e.target.value)}
              className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm">
              {nations.map((n) => <option key={n} value={n}>{n || '변경안함'}</option>)}
            </select>
          </div>
          <Btn color="yellow" onClick={bulkUpdate}>일괄 변경</Btn>
        </div>
      </Section>

      <div className="flex gap-4">
        {/* 목록 */}
        <div className="w-64 shrink-0">
          <Section title="용병 목록">
            <input
              value={filterName}
              onChange={(e) => { setFilterName(e.target.value); setPage(0); }}
              onKeyDown={(e) => e.key === 'Enter' && load()}
              placeholder="이름 검색"
              className="w-full bg-gray-700 border border-gray-600 rounded px-2 py-1 text-xs mb-2"
            />
            <div className="flex gap-1 mb-2">
              <select value={filterNature} onChange={(e) => { setFilterNature(e.target.value); setPage(0); }}
                className="bg-gray-700 border border-gray-600 rounded px-1 py-1 text-xs flex-1">
                {natures.map((n) => <option key={n} value={n}>{n || '전체'}</option>)}
              </select>
              <select value={filterNation} onChange={(e) => { setFilterNation(e.target.value); setPage(0); }}
                className="bg-gray-700 border border-gray-600 rounded px-1 py-1 text-xs flex-1">
                {nations.map((n) => <option key={n} value={n}>{n || '전체'}</option>)}
              </select>
            </div>
            <Btn color="gray" onClick={load}>검색</Btn>
            <p className="text-xs text-gray-400 mt-1">총 {total}개</p>
            <ul className="mt-2 max-h-96 overflow-y-auto divide-y divide-gray-700">
              {mercs.map((m) => (
                <li
                  key={m.id}
                  onClick={() => selectMerc(m.id)}
                  className={`py-1.5 px-1 cursor-pointer text-sm hover:bg-gray-700 ${selectedId === m.id ? 'bg-gray-700 text-yellow-300' : ''}`}
                >
                  <span className="font-medium">{m.name}</span>
                  <span className="ml-1 text-xs text-gray-400">{m.nature}/{m.nation}</span>
                  {m.characteristicCount === 0 && <span className="ml-1 text-xs text-red-400">특성없음</span>}
                </li>
              ))}
            </ul>
            {total > 30 && (
              <div className="flex gap-1 mt-2">
                <Btn color="gray" disabled={page === 0} onClick={() => setPage(p => p - 1)}>이전</Btn>
                <Btn color="gray" disabled={(page + 1) * 30 >= total} onClick={() => setPage(p => p + 1)}>다음</Btn>
              </div>
            )}
          </Section>
        </div>

        {/* 상세 */}
        <div className="flex-1 min-w-0">
          {!selectedId ? (
            <p className="text-gray-500 mt-8 text-center">용병을 선택하세요</p>
          ) : (
            <div>
              <Section title={`용병 ${selectedMercenary?.name ?? `#${selectedId}`} 관리`}>
                <p className="text-xs text-gray-400 mb-3">
                  삭제는 DB에서 실제로 제거합니다. 덱, 장비, 재료 등에서 참조 중이면 실패합니다.
                </p>
                <Btn color="red" onClick={deleteMercenary}>용병 삭제</Btn>
              </Section>

              <Section title="스탯">
                <div className="overflow-x-auto mb-3">
                  <table className="w-full text-sm">
                    <thead className="text-gray-400 border-b border-gray-700">
                      <tr>
                        <th className="text-left py-1 pr-2">스탯 타입</th>
                        <th className="text-left py-1 pr-2">값</th>
                        <th className="text-left py-1">액션</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mercStats.length === 0 && <EmptyRows colSpan={3} label="스탯 없음" />}
                      {mercStats.map((stat, idx) => (
                        <tr key={stat.id ?? idx} className="border-b border-gray-700">
                          <td className="py-1 pr-2">
                            <StatTypeSelect value={stat.statType} onChange={(v) => setMercStats(rows => rows.map((row, i) => i === idx ? { ...row, statType: v } : row))} />
                          </td>
                          <td className="py-1 pr-2 w-32">
                            <SmallInput value={stat.value} onChange={(v) => setMercStats(rows => rows.map((row, i) => i === idx ? { ...row, value: v } : row))} type="number" />
                          </td>
                          <td className="py-1">
                            <Btn color="red" onClick={() => setMercStats(rows => rows.filter((_, i) => i !== idx))}>삭제</Btn>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="flex gap-2">
                  <Btn color="green" onClick={() => setMercStats(rows => [...rows, { statType: '', value: '' }])}>행 추가</Btn>
                  <Btn color="yellow" onClick={saveStats}>저장 (전체 교체)</Btn>
                </div>
              </Section>

              <Section title="스킬">
                <div className="overflow-x-auto mb-3">
                  <table className="w-full text-sm">
                    <thead className="text-gray-400 border-b border-gray-700">
                      <tr>
                        <th className="text-left py-1 pr-2">스킬명</th>
                        <th className="text-left py-1">액션</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mercSkills.length === 0 && <EmptyRows colSpan={2} label="스킬 없음" />}
                      {mercSkills.map((skill, idx) => (
                        <tr key={idx} className="border-b border-gray-700">
                          <td className="py-1 pr-2">
                            <SmallInput value={skill.skillName} onChange={(v) => setMercSkills(rows => rows.map((row, i) => i === idx ? { ...row, skillName: v } : row))} placeholder="스킬명" />
                          </td>
                          <td className="py-1 w-20">
                            <Btn color="red" onClick={() => setMercSkills(rows => rows.filter((_, i) => i !== idx))}>삭제</Btn>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="flex gap-2 mb-4">
                  <Btn color="green" onClick={() => setMercSkills(rows => [...rows, { id: null, skillName: '', effects: [] }])}>행 추가</Btn>
                  <Btn color="yellow" onClick={saveSkills}>저장 (전체 교체)</Btn>
                </div>

                {/* 스킬별 효과 편집 — 저장 후 ID가 생긴 스킬만 표시 */}
                {mercSkills.some(s => s.id !== null) && (
                  <div className="space-y-3">
                    <p className="text-xs text-gray-400">스킬 효과 (저항깎·속성깎 등 상대에게 적용되는 디버프)</p>
                    {mercSkills.filter(s => s.id !== null).map((skill) => (
                      <div key={skill.id} className="rounded border border-gray-700 p-2">
                        <p className="text-sm font-medium text-yellow-300 mb-2">{skill.skillName}</p>
                        <table className="w-full text-sm mb-2">
                          <thead className="text-gray-400 border-b border-gray-700">
                            <tr>
                              <th className="text-left py-1 pr-2">효과 종류</th>
                              <th className="text-left py-1 pr-2">수치</th>
                              <th className="text-left py-1 pr-2">수치 타입</th>
                              <th className="text-left py-1">액션</th>
                            </tr>
                          </thead>
                          <tbody>
                            {skill.effects.length === 0 && <EmptyRows colSpan={4} label="효과 없음" />}
                            {skill.effects.map((effect, eIdx) => (
                              <tr key={eIdx} className="border-b border-gray-700">
                                <td className="py-1 pr-2">
                                  <StatTypeSelect value={effect.statKey} onChange={(v) => setMercSkills(rows => rows.map(r => r.id !== skill.id ? r : { ...r, effects: r.effects.map((e, i) => i === eIdx ? { ...e, statKey: v } : e) }))} />
                                </td>
                                <td className="py-1 pr-2 w-28">
                                  <SmallInput type="number" value={effect.statValue} onChange={(v) => setMercSkills(rows => rows.map(r => r.id !== skill.id ? r : { ...r, effects: r.effects.map((e, i) => i === eIdx ? { ...e, statValue: v } : e) }))} placeholder="수치" />
                                </td>
                                <td className="py-1 pr-2 w-32">
                                  <ValueTypeSelect value={effect.valueType} onChange={(v) => setMercSkills(rows => rows.map(r => r.id !== skill.id ? r : { ...r, effects: r.effects.map((e, i) => i === eIdx ? { ...e, valueType: v } : e) }))} />
                                </td>
                                <td className="py-1 w-20">
                                  <Btn color="red" onClick={() => setMercSkills(rows => rows.map(r => r.id !== skill.id ? r : { ...r, effects: r.effects.filter((_, i) => i !== eIdx) }))}>삭제</Btn>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        <div className="flex gap-2">
                          <Btn color="green" onClick={() => setMercSkills(rows => rows.map(r => r.id !== skill.id ? r : { ...r, effects: [...r.effects, { statKey: '', statValue: '', valueType: 'FLAT' }] }))}>행 추가</Btn>
                          <Btn color="yellow" onClick={() => saveSkillEffects(skill.id!, skill.effects)}>효과 저장</Btn>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Section>

              <Section title="특성 관리">
                <div className="mb-4 rounded border border-gray-700 p-3">
                  <p className="text-sm font-medium text-yellow-300 mb-3">새 특성 추가</p>
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-2 mb-3">
                    <div>
                      <label className="text-xs text-gray-400 block mb-1">특성명</label>
                      <SmallInput value={newChar.name} onChange={(v) => setNewChar(p => ({ ...p, name: v }))} placeholder="저항깎 증가" />
                    </div>
                    <div>
                      <label className="text-xs text-gray-400 block mb-1">포인트</label>
                      <SmallInput value={newChar.point} onChange={(v) => setNewChar(p => ({ ...p, point: v }))} type="number" placeholder="각성이면 공백" />
                    </div>
                    <div>
                      <label className="text-xs text-gray-400 block mb-1">선행 특성 ID</label>
                      <SmallInput value={newChar.requiredCharacteristicId} onChange={(v) => setNewChar(p => ({ ...p, requiredCharacteristicId: v }))} type="number" placeholder="없으면 공백" />
                    </div>
                    <div>
                      <label className="text-xs text-gray-400 block mb-1">설명</label>
                      <SmallInput value={newChar.description} onChange={(v) => setNewChar(p => ({ ...p, description: v }))} placeholder="설명" />
                    </div>
                  </div>
                  <Btn color="green" onClick={addChar}>특성 추가</Btn>
                </div>

                <div className="divide-y divide-gray-700">
                  {chars.map((c) => (
                    <div key={c.id} className="py-2">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-sm font-medium text-yellow-300">{c.name}</span>
                        <span className="text-xs text-gray-400">#{c.id}</span>
                        {c.requiredCharacteristicId && (
                          <span className="text-xs text-gray-500">선행: #{c.requiredCharacteristicId}</span>
                        )}
                        <Btn color="red" onClick={() => deleteChar(c.id)}>삭제</Btn>
                      </div>
                      <div className="overflow-x-auto mb-2">
                        <table className="w-full text-sm">
                          <thead className="text-gray-400 border-b border-gray-700">
                            <tr>
                              <th className="text-left py-1 pr-2">항목명</th>
                              <th className="text-left py-1 pr-2">레벨</th>
                              <th className="text-left py-1 pr-2">수치</th>
                              <th className="text-left py-1 pr-2">스탯 타입</th>
                              <th className="text-left py-1">액션</th>
                            </tr>
                          </thead>
                          <tbody>
                            {(charLevels[c.id] ?? []).length === 0 && <EmptyRows colSpan={5} label="레벨 수치 없음" />}
                            {(charLevels[c.id] ?? []).map((level, idx) => (
                              <tr key={level.id ?? idx} className="border-b border-gray-700">
                                <td className="py-1 pr-2">
                                  <SmallInput value={level.label} onChange={(v) => setCharLevels(map => ({ ...map, [c.id]: (map[c.id] ?? []).map((row, i) => i === idx ? { ...row, label: v } : row) }))} placeholder="타격저항력" />
                                </td>
                                <td className="py-1 pr-2 w-20">
                                  <SmallInput value={level.level} onChange={(v) => setCharLevels(map => ({ ...map, [c.id]: (map[c.id] ?? []).map((row, i) => i === idx ? { ...row, level: v } : row) }))} type="number" />
                                </td>
                                <td className="py-1 pr-2 w-28">
                                  <SmallInput value={level.amount} onChange={(v) => setCharLevels(map => ({ ...map, [c.id]: (map[c.id] ?? []).map((row, i) => i === idx ? { ...row, amount: v } : row) }))} placeholder="20%" />
                                </td>
                                <td className="py-1 pr-2">
                                  <StatTypeSelect allowEmpty value={level.statType} onChange={(v) => setCharLevels(map => ({ ...map, [c.id]: (map[c.id] ?? []).map((row, i) => i === idx ? { ...row, statType: v } : row) }))} />
                                </td>
                                <td className="py-1">
                                  <Btn color="red" onClick={() => setCharLevels(map => ({ ...map, [c.id]: (map[c.id] ?? []).filter((_, i) => i !== idx) }))}>삭제</Btn>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <div className="flex gap-2">
                        <Btn color="green" onClick={() => setCharLevels(map => ({ ...map, [c.id]: [...(map[c.id] ?? []), { label: '', level: '', amount: '', statType: '' }] }))}>레벨 행 추가</Btn>
                        <Btn color="yellow" onClick={() => saveLevels(c.id)}>레벨 저장</Btn>
                      </div>
                    </div>
                  ))}
                  {chars.length === 0 && <p className="text-gray-500 text-sm py-2">특성 없음</p>}
                </div>
              </Section>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ───────────────────────── 세트 관리 탭 ─────────────────────────

interface SetRow {
  id: number;
  name: string;
  pieceCount: number;
  tradeable: boolean;
}

function SetsTab({ notify }: { notify: (m: string) => void }) {
  const [sets, setSets] = useState<SetRow[]>([]);
  const [filterName, setFilterName] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<SetRow | null>(null);
  const [editName, setEditName] = useState('');
  const [editPiece, setEditPiece] = useState('');
  const [editTradeable, setEditTradeable] = useState(true);

  const load = useCallback(async () => {
    try {
      const sp = new URLSearchParams({ page: String(page), size: '30', sort: 'name' });
      if (filterName) sp.set('name', filterName);
      const data = await req<PageResponse<SetRow>>(`/admin/sets?${sp}`);
      setSets(data.content);
      setTotal(data.totalElements);
    } catch (e: unknown) {
      notify(`오류 - 세트 목록: ${(e as Error).message}`);
    }
  }, [page, filterName, notify]);

  useEffect(() => { load(); }, [load]);

  function selectSet(s: SetRow) {
    setSelected(s);
    setEditName(s.name);
    setEditPiece(String(s.pieceCount));
    setEditTradeable(s.tradeable);
  }

  async function saveSet() {
    if (!selected) return;
    try {
      await req(`/admin/sets/${selected.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ name: editName, pieceCount: parseInt(editPiece), tradeable: editTradeable }),
      });
      notify(`✅ 세트 #${selected.id} 수정 완료`);
      load();
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  return (
    <div className="flex gap-4">
      <div className="w-72 shrink-0">
        <Section title="세트 목록">
          <div className="mb-2">
            <Input value={filterName} onChange={(v) => { setFilterName(v); setPage(0); }} placeholder="이름 검색" />
          </div>
          <Btn color="gray" onClick={load}>검색</Btn>
          <p className="text-xs text-gray-400 mt-1">총 {total}개</p>
          <ul className="mt-2 max-h-96 overflow-y-auto divide-y divide-gray-700">
            {sets.map((s) => (
              <li
                key={s.id}
                onClick={() => selectSet(s)}
                className={`py-1.5 px-1 cursor-pointer text-sm hover:bg-gray-700 ${selected?.id === s.id ? 'bg-gray-700 text-yellow-300' : ''}`}
              >
                <span className="font-medium">{s.name}</span>
                <span className="ml-1 text-xs text-gray-400">({s.pieceCount}피스)</span>
                {!s.tradeable && <span className="ml-1 text-xs text-gray-500">미노출</span>}
              </li>
            ))}
          </ul>
          {total > 30 && (
            <div className="flex gap-1 mt-2">
              <Btn color="gray" disabled={page === 0} onClick={() => setPage(p => p - 1)}>이전</Btn>
              <Btn color="gray" disabled={(page + 1) * 30 >= total} onClick={() => setPage(p => p + 1)}>다음</Btn>
            </div>
          )}
        </Section>
      </div>

      <div className="flex-1">
        {!selected ? (
          <p className="text-gray-500 mt-8 text-center">세트를 선택하세요</p>
        ) : (
          <Section title={`세트 #${selected.id} 수정`}>
            <div className="grid gap-3 max-w-sm">
              <div>
                <label className="text-xs text-gray-400 block mb-1">이름</label>
                <Input value={editName} onChange={setEditName} />
              </div>
              <div>
                <label className="text-xs text-gray-400 block mb-1">피스 수</label>
                <Input value={editPiece} onChange={setEditPiece} type="number" />
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="tradeable"
                  checked={editTradeable}
                  onChange={(e) => setEditTradeable(e.target.checked)}
                  className="w-4 h-4"
                />
                <label htmlFor="tradeable" className="text-sm">거래 노출 허용</label>
              </div>
              <Btn color="yellow" onClick={saveSet}>저장</Btn>
            </div>
          </Section>
        )}
      </div>
    </div>
  );
}

// ───────────────────────── 이미지 관리 탭 ─────────────────────────

type ItemImageTarget = {
  id: number;
  name: string;
  type: 'MATERIAL' | 'EQUIPMENT';
  imageUrl: string | null;
};

type GemImageTarget = {
  id: number;
  name: string;
  gemGrade: string;
  ritualId: number | null;
  imageUrl: string | null;
};

type MercenaryImageTarget = {
  id: number;
  name: string;
  category: string;
  nature: string;
  imageUrl: string | null;
};

/** multipart/form-data 업로드 전용 — Content-Type 미설정으로 브라우저가 boundary 자동 처리 */
async function reqMultipart<T>(path: string, formData: FormData): Promise<T> {
  const doFetch = (token: string | null) => {
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return fetch(`${BASE}${path}`, { method: 'POST', headers, body: formData, credentials: 'include' });
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
  return res.json();
}

const GEM_GRADE_LABEL: Record<string, string> = {
  BASE: '기본', POLISHED: '세공됨', ENHANCED: '강화됨', BRILLIANT: '빛나는',
};
const NATURE_LABEL: Record<string, string> = {
  FIRE: '화', WATER: '수', THUNDER: '뇌', WIND: '풍', EARTH: '토', NONE: '무',
};
const CATEGORY_LABEL: Record<string, string> = {
  AWAKENED_HEAVENLY_KING: '각성사천왕', MYEONGWANG: '명왕', LEGENDARY_GENERAL: '명장',
};

function ImageCell({ url }: { url: string | null }) {
  if (!url) return <span className="text-xs text-red-400">미등록</span>;
  return (
    <img
      src={url}
      alt=""
      className="w-10 h-10 object-cover rounded border border-gray-600"
      onError={(e) => { (e.target as HTMLImageElement).replaceWith(Object.assign(document.createElement('span'), { textContent: '오류', className: 'text-xs text-gray-500' })); }}
    />
  );
}

function ItemImageSection({ notify }: { notify: (m: string) => void }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('');
  const [mode, setMode] = useState<'idle' | 'search' | 'missing'>('idle');
  const [results, setResults] = useState<ItemImageTarget[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [uploadingId, setUploadingId] = useState<number | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  async function search(p = 0) {
    setLoading(true);
    setMode('search');
    try {
      const sp = new URLSearchParams({ page: String(p), size: '30' });
      if (name.trim()) sp.set('name', name.trim());
      if (type) sp.set('type', type);
      const data = await req<PageResponse<ItemImageTarget>>(`/admin/images/items?${sp}`);
      setResults(data.content);
      setPage(data.number);
      setTotalPages(data.totalPages);
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setLoading(false);
    }
  }

  async function loadMissing() {
    setLoading(true);
    setMode('missing');
    try {
      const data = await req<ItemImageTarget[]>('/admin/images/items/missing');
      setResults(data);
      setPage(0);
      setTotalPages(1);
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setLoading(false);
    }
  }

  function openPicker(id: number) {
    setUploadingId(id);
    fileRef.current?.click();
  }

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || uploadingId == null) return;
    const id = uploadingId;
    setUploadingId(id);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await reqMultipart<{ imageUrl: string }>(`/admin/images/items/${id}`, fd);
      setResults(prev => prev.map(r => r.id === id ? { ...r, imageUrl: res.imageUrl } : r));
      notify('이미지 등록 완료');
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setUploadingId(null);
    }
  }

  return (
    <Section title="아이템 이미지">
      <div className="flex gap-2 flex-wrap items-end mb-3">
        <div className="flex-1 min-w-40">
          <Input value={name} onChange={setName} placeholder="아이템명 검색" />
        </div>
        <select value={type} onChange={(e) => setType(e.target.value)} className={inputClass('w-28')}>
          <option value="">전체</option>
          <option value="MATERIAL">재료</option>
          <option value="EQUIPMENT">장비</option>
        </select>
        <Btn onClick={() => search(0)} disabled={loading}>검색</Btn>
        <Btn color="blue" onClick={loadMissing} disabled={loading}>거래 있는데 이미지 없는 장비</Btn>
      </div>

      {mode === 'missing' && results.length > 0 && (
        <p className="text-xs text-yellow-400 mb-2">거래 이력 있으나 이미지 없는 장비 {results.length}개</p>
      )}

      <input type="file" ref={fileRef} accept="image/*" className="hidden" onChange={handleFile} />

      {loading ? (
        <p className="text-gray-400 text-sm py-4">불러오는 중...</p>
      ) : mode === 'idle' ? (
        <p className="text-gray-500 text-sm py-4">검색하거나 누락 목록을 조회하세요</p>
      ) : results.length === 0 ? (
        <p className="text-gray-500 text-sm py-4">결과 없음</p>
      ) : (
        <>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-400 text-left border-b border-gray-700">
                <th className="py-1.5 pr-3 w-12">ID</th>
                <th className="py-1.5 pr-3">이름</th>
                <th className="py-1.5 pr-3 w-14">타입</th>
                <th className="py-1.5 pr-3 w-14">이미지</th>
                <th className="py-1.5 w-20"></th>
              </tr>
            </thead>
            <tbody>
              {results.map(r => (
                <tr key={r.id} className="border-b border-gray-700/40 hover:bg-gray-700/30">
                  <td className="py-1.5 pr-3 text-gray-400 text-xs">{r.id}</td>
                  <td className="py-1.5 pr-3">{r.name}</td>
                  <td className="py-1.5 pr-3 text-xs text-gray-400">{r.type === 'MATERIAL' ? '재료' : '장비'}</td>
                  <td className="py-1.5 pr-3"><ImageCell url={r.imageUrl} /></td>
                  <td className="py-1.5">
                    <Btn color="green" onClick={() => openPicker(r.id)} disabled={uploadingId === r.id}>
                      {uploadingId === r.id ? '업로드 중' : '등록'}
                    </Btn>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {mode === 'search' && totalPages > 1 && (
            <div className="flex items-center gap-2 mt-2">
              <Btn color="gray" disabled={page === 0} onClick={() => search(page - 1)}>이전</Btn>
              <span className="text-xs text-gray-400">{page + 1} / {totalPages}</span>
              <Btn color="gray" disabled={page + 1 >= totalPages} onClick={() => search(page + 1)}>다음</Btn>
            </div>
          )}
        </>
      )}
    </Section>
  );
}

function GemImageSection({ notify }: { notify: (m: string) => void }) {
  const [name, setName] = useState('');
  const [mode, setMode] = useState<'idle' | 'search' | 'missing'>('idle');
  const [results, setResults] = useState<GemImageTarget[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [uploadingId, setUploadingId] = useState<number | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  async function search(p = 0) {
    setLoading(true);
    setMode('search');
    try {
      const sp = new URLSearchParams({ page: String(p), size: '30' });
      if (name.trim()) sp.set('name', name.trim());
      const data = await req<PageResponse<GemImageTarget>>(`/admin/images/gems?${sp}`);
      setResults(data.content);
      setPage(data.number);
      setTotalPages(data.totalPages);
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setLoading(false);
    }
  }

  async function loadMissing() {
    setLoading(true);
    setMode('missing');
    try {
      const data = await req<GemImageTarget[]>('/admin/images/gems/missing');
      setResults(data);
      setPage(0);
      setTotalPages(1);
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setLoading(false);
    }
  }

  function openPicker(id: number) {
    setUploadingId(id);
    fileRef.current?.click();
  }

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || uploadingId == null) return;
    const id = uploadingId;
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await reqMultipart<{ imageUrl: string }>(`/admin/images/gems/${id}`, fd);
      setResults(prev => prev.map(r => r.id === id ? { ...r, imageUrl: res.imageUrl } : r));
      notify('이미지 등록 완료');
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setUploadingId(null);
    }
  }

  return (
    <Section title="보석 이미지">
      <div className="flex gap-2 flex-wrap items-end mb-3">
        <div className="flex-1 min-w-40">
          <Input value={name} onChange={setName} placeholder="보석명 검색" />
        </div>
        <Btn onClick={() => search(0)} disabled={loading}>검색</Btn>
        <Btn color="blue" onClick={loadMissing} disabled={loading}>이미지 없는 보석 목록</Btn>
      </div>

      {mode === 'missing' && results.length > 0 && (
        <p className="text-xs text-yellow-400 mb-2">이미지 미등록 보석 {results.length}개</p>
      )}

      <input type="file" ref={fileRef} accept="image/*" className="hidden" onChange={handleFile} />

      {loading ? (
        <p className="text-gray-400 text-sm py-4">불러오는 중...</p>
      ) : mode === 'idle' ? (
        <p className="text-gray-500 text-sm py-4">검색하거나 누락 목록을 조회하세요</p>
      ) : results.length === 0 ? (
        <p className="text-gray-500 text-sm py-4">결과 없음</p>
      ) : (
        <>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-400 text-left border-b border-gray-700">
                <th className="py-1.5 pr-3 w-12">ID</th>
                <th className="py-1.5 pr-3">이름</th>
                <th className="py-1.5 pr-3 w-20">등급</th>
                <th className="py-1.5 pr-3 w-14">이미지</th>
                <th className="py-1.5 w-20"></th>
              </tr>
            </thead>
            <tbody>
              {results.map(r => (
                <tr key={r.id} className="border-b border-gray-700/40 hover:bg-gray-700/30">
                  <td className="py-1.5 pr-3 text-gray-400 text-xs">{r.id}</td>
                  <td className="py-1.5 pr-3">
                    {r.name}
                    {r.ritualId && <span className="ml-1 text-xs text-purple-400">(주술)</span>}
                  </td>
                  <td className="py-1.5 pr-3 text-xs text-gray-400">{GEM_GRADE_LABEL[r.gemGrade] ?? r.gemGrade}</td>
                  <td className="py-1.5 pr-3"><ImageCell url={r.imageUrl} /></td>
                  <td className="py-1.5">
                    <Btn color="green" onClick={() => openPicker(r.id)} disabled={uploadingId === r.id}>
                      {uploadingId === r.id ? '업로드 중' : '등록'}
                    </Btn>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {mode === 'search' && totalPages > 1 && (
            <div className="flex items-center gap-2 mt-2">
              <Btn color="gray" disabled={page === 0} onClick={() => search(page - 1)}>이전</Btn>
              <span className="text-xs text-gray-400">{page + 1} / {totalPages}</span>
              <Btn color="gray" disabled={page + 1 >= totalPages} onClick={() => search(page + 1)}>다음</Btn>
            </div>
          )}
        </>
      )}
    </Section>
  );
}

function MercenaryImageSection({ notify }: { notify: (m: string) => void }) {
  const [name, setName] = useState('');
  const [mode, setMode] = useState<'idle' | 'search' | 'missing'>('idle');
  const [results, setResults] = useState<MercenaryImageTarget[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [uploadingId, setUploadingId] = useState<number | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  async function search(p = 0) {
    setLoading(true);
    setMode('search');
    try {
      const sp = new URLSearchParams({ page: String(p), size: '30' });
      if (name.trim()) sp.set('name', name.trim());
      const data = await req<PageResponse<MercenaryImageTarget>>(`/admin/images/mercenaries?${sp}`);
      setResults(data.content);
      setPage(data.number);
      setTotalPages(data.totalPages);
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setLoading(false);
    }
  }

  async function loadMissing() {
    setLoading(true);
    setMode('missing');
    try {
      const data = await req<MercenaryImageTarget[]>('/admin/images/mercenaries/missing');
      setResults(data);
      setPage(0);
      setTotalPages(1);
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setLoading(false);
    }
  }

  function openPicker(id: number) {
    setUploadingId(id);
    fileRef.current?.click();
  }

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || uploadingId == null) return;
    const id = uploadingId;
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await reqMultipart<{ imageUrl: string }>(`/admin/images/mercenaries/${id}`, fd);
      setResults(prev => prev.map(r => r.id === id ? { ...r, imageUrl: res.imageUrl } : r));
      notify('이미지 등록 완료');
    } catch (e) {
      notify(`오류: ${e}`);
    } finally {
      setUploadingId(null);
    }
  }

  return (
    <Section title="용병 이미지">
      <div className="flex gap-2 flex-wrap items-end mb-3">
        <div className="flex-1 min-w-40">
          <Input value={name} onChange={setName} placeholder="용병명 검색" />
        </div>
        <Btn onClick={() => search(0)} disabled={loading}>검색</Btn>
        <Btn color="blue" onClick={loadMissing} disabled={loading}>이미지 없는 용병 목록</Btn>
      </div>

      {mode === 'missing' && results.length > 0 && (
        <p className="text-xs text-yellow-400 mb-2">이미지 미등록 용병 {results.length}명</p>
      )}

      <input type="file" ref={fileRef} accept="image/*" className="hidden" onChange={handleFile} />

      {loading ? (
        <p className="text-gray-400 text-sm py-4">불러오는 중...</p>
      ) : mode === 'idle' ? (
        <p className="text-gray-500 text-sm py-4">검색하거나 누락 목록을 조회하세요</p>
      ) : results.length === 0 ? (
        <p className="text-gray-500 text-sm py-4">결과 없음</p>
      ) : (
        <>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-400 text-left border-b border-gray-700">
                <th className="py-1.5 pr-3 w-12">ID</th>
                <th className="py-1.5 pr-3">이름</th>
                <th className="py-1.5 pr-3 w-20">분류</th>
                <th className="py-1.5 pr-3 w-12">속성</th>
                <th className="py-1.5 pr-3 w-14">이미지</th>
                <th className="py-1.5 w-20"></th>
              </tr>
            </thead>
            <tbody>
              {results.map(r => (
                <tr key={r.id} className="border-b border-gray-700/40 hover:bg-gray-700/30">
                  <td className="py-1.5 pr-3 text-gray-400 text-xs">{r.id}</td>
                  <td className="py-1.5 pr-3">{r.name}</td>
                  <td className="py-1.5 pr-3 text-xs text-gray-400">{CATEGORY_LABEL[r.category] ?? r.category}</td>
                  <td className="py-1.5 pr-3 text-xs text-gray-400">{NATURE_LABEL[r.nature] ?? r.nature}</td>
                  <td className="py-1.5 pr-3"><ImageCell url={r.imageUrl} /></td>
                  <td className="py-1.5">
                    <Btn color="green" onClick={() => openPicker(r.id)} disabled={uploadingId === r.id}>
                      {uploadingId === r.id ? '업로드 중' : '등록'}
                    </Btn>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {mode === 'search' && totalPages > 1 && (
            <div className="flex items-center gap-2 mt-2">
              <Btn color="gray" disabled={page === 0} onClick={() => search(page - 1)}>이전</Btn>
              <span className="text-xs text-gray-400">{page + 1} / {totalPages}</span>
              <Btn color="gray" disabled={page + 1 >= totalPages} onClick={() => search(page + 1)}>다음</Btn>
            </div>
          )}
        </>
      )}
    </Section>
  );
}

function ImageTab({ notify }: { notify: (m: string) => void }) {
  const [subTab, setSubTab] = useState<'items' | 'gems' | 'mercenaries'>('items');
  const subTabs = [
    { id: 'items' as const, label: '아이템' },
    { id: 'gems' as const, label: '보석' },
    { id: 'mercenaries' as const, label: '용병' },
  ];
  return (
    <div>
      <div className="flex gap-2 mb-4">
        {subTabs.map(t => (
          <button
            key={t.id}
            onClick={() => setSubTab(t.id)}
            className={`px-3 py-1.5 text-sm rounded font-medium transition-colors ${
              subTab === t.id ? 'bg-yellow-500 text-black' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>
      {subTab === 'items' && <ItemImageSection notify={notify} />}
      {subTab === 'gems' && <GemImageSection notify={notify} />}
      {subTab === 'mercenaries' && <MercenaryImageSection notify={notify} />}
    </div>
  );
}

// ───────────────────────── 메인 관리자 페이지 ─────────────────────────

const TABS = [
  { id: 'crawler', label: '크롤러' },
  { id: 'reports', label: '신고 관리' },
  { id: 'listings', label: '등록글 관리' },
  { id: 'items', label: '아이템' },
  { id: 'mercenaries', label: '용병' },
  { id: 'sets', label: '세트' },
  { id: 'images', label: '이미지 등록' },
];

export default function AdminPage() {
  const [tab, setTab] = useState('crawler');
  const [toast, setToast] = useState('');

  const notify = useCallback((m: string) => setToast(m), []);

  return (
    <div className="max-w-7xl mx-auto p-4 text-gray-100 [color-scheme:dark] [&_input]:text-gray-100 [&_input::placeholder]:text-gray-400 [&_select]:text-gray-100 [&_textarea]:text-gray-100 [&_textarea::placeholder]:text-gray-400 [&_option]:bg-gray-800 [&_option]:text-gray-100">
      <h1 className="text-2xl font-bold text-yellow-400 mb-4">관리자 페이지</h1>

      {/* 탭 */}
      <div className="flex gap-1 mb-6 border-b border-gray-700 flex-wrap">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2 text-sm font-medium -mb-px border-b-2 transition-colors ${
              tab === t.id
                ? 'border-yellow-400 text-yellow-400'
                : 'border-transparent text-gray-400 hover:text-gray-200'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'crawler' && <CrawlerTab notify={notify} />}
      {tab === 'reports' && <ReportsTab notify={notify} />}
      {tab === 'listings' && <ListingsTab notify={notify} />}
      {tab === 'items' && <ItemsTab notify={notify} />}
      {tab === 'mercenaries' && <MercenariesTab notify={notify} />}
      {tab === 'sets' && <SetsTab notify={notify} />}
      {tab === 'images' && <ImageTab notify={notify} />}

      {toast && <Toast msg={toast} onClose={() => setToast('')} />}
    </div>
  );
}
