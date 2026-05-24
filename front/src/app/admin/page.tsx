'use client';

import { useState, useEffect, useCallback } from 'react';

const BASE = '';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

async function req<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, { ...options, headers, credentials: 'include' });
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
    { path: 'master', label: '전체 수집 (아이템+용병)', color: 'yellow' as const },
    { path: 'items', label: '장비/보석 수집', color: 'blue' as const },
    { path: 'materials', label: '잡화/소모품/재료 수집', color: 'blue' as const },
    { path: 'mercenaries', label: '용병 수집', color: 'blue' as const },
    { path: 'sets', label: '장비 세트 수집', color: 'blue' as const },
    { path: 'price', label: '가격 데이터 수집', color: 'green' as const },
  ];

  return (
    <Section title="크롤링 Job 수동 트리거">
      <p className="text-xs text-gray-400 mb-3">
        배치 Job을 즉시 실행합니다. Job이 시작되면 백그라운드에서 실행되며 결과는 서버 로그에서 확인할 수 있습니다.
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
  stats: { id?: number; statType: string; element?: string | null; value: number }[];
  skills: string[];
}

function ItemsTab({ notify }: { notify: (m: string) => void }) {
  const [items, setItems] = useState<ItemRow[]>([]);
  const [filterType, setFilterType] = useState('');
  const [filterName, setFilterName] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<ItemDetail | null>(null);
  const [editInfo, setEditInfo] = useState({ name: '', type: '', tradeCategory: '' });
  const [itemStats, setItemStats] = useState<EditableStat[]>([]);
  const [itemSkills, setItemSkills] = useState<string[]>([]);
  const [eqDetail, setEqDetail] = useState<EditableEquipmentDetail>({
    slot: '',
    equipmentKind: '',
    setId: '',
    ritualApplicable: false,
    hasSlotOption: false,
    equipSlot: '',
  });

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
      })));
      setItemSkills(data.skills ?? []);
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
        }));
      await req(`/admin/items/${selected.id}/stats`, { method: 'PUT', body: JSON.stringify({ stats }) });
      notify('✅ 스탯 저장 완료');
    } catch (e: unknown) { notify(`오류 - ${(e as Error).message}`); }
  }

  async function saveSkills() {
    if (!selected) return;
    try {
      const skills = itemSkills.map((skill) => skill.trim()).filter(Boolean);
      await req(`/admin/items/${selected.id}/skills`, { method: 'PUT', body: JSON.stringify({ skills }) });
      notify('✅ 스킬 저장 완료');
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

  return (
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
                      <th className="text-left py-1">액션</th>
                    </tr>
                  </thead>
                  <tbody>
                    {itemStats.length === 0 && <EmptyRows colSpan={4} label="스탯 없음" />}
                    {itemStats.map((stat, idx) => (
                      <tr key={stat.id ?? idx} className="border-b border-gray-700">
                        <td className="py-1 pr-2">
                          <StatTypeSelect value={stat.statType} onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, statType: v } : row))} />
                        </td>
                        <td className="py-1 pr-2">
                          <SmallInput value={stat.element ?? ''} onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, element: v } : row))} placeholder="없으면 공백" />
                        </td>
                        <td className="py-1 pr-2 w-28">
                          <SmallInput value={stat.value} onChange={(v) => setItemStats(rows => rows.map((row, i) => i === idx ? { ...row, value: v } : row))} type="number" />
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
                <Btn color="green" onClick={() => setItemStats(rows => [...rows, { statType: '', element: '', value: '' }])}>행 추가</Btn>
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
                    {itemSkills.length === 0 && <EmptyRows colSpan={2} label="스킬 없음" />}
                    {itemSkills.map((skill, idx) => (
                      <tr key={idx} className="border-b border-gray-700">
                        <td className="py-1 pr-2">
                          <SmallInput value={skill} onChange={(v) => setItemSkills(rows => rows.map((row, i) => i === idx ? v : row))} placeholder="스킬명" />
                        </td>
                        <td className="py-1 w-20">
                          <Btn color="red" onClick={() => setItemSkills(rows => rows.filter((_, i) => i !== idx))}>삭제</Btn>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex gap-2">
                <Btn color="green" onClick={() => setItemSkills(rows => [...rows, ''])}>행 추가</Btn>
                <Btn color="yellow" onClick={saveSkills}>저장 (전체 교체)</Btn>
              </div>
            </Section>
          </div>
        )}
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

function MercenariesTab({ notify }: { notify: (m: string) => void }) {
  const [mercs, setMercs] = useState<MercenaryRow[]>([]);
  const [filterNature, setFilterNature] = useState('');
  const [filterNation, setFilterNation] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [chars, setChars] = useState<CharRow[]>([]);
  const [mercStats, setMercStats] = useState<EditableStat[]>([]);
  const [mercSkills, setMercSkills] = useState<string[]>([]);
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
      const data = await req<PageResponse<MercenaryRow>>(`/admin/mercenaries?${sp}`);
      setMercs(data.content);
      setTotal(data.totalElements);
    } catch (e: unknown) {
      notify(`오류 - 용병 목록: ${(e as Error).message}`);
    }
  }, [page, filterNature, filterNation, notify]);

  useEffect(() => { load(); }, [load]);

  async function selectMerc(id: number) {
    setSelectedId(id);
    try {
      const [detail, charList] = await Promise.all([
        req<{ stats: { id?: number; statType: string; value: number }[]; skills: string[] }>(`/admin/mercenaries/${id}`),
        req<CharRow[]>(`/admin/mercenaries/${id}/characteristics`),
      ]);
      setMercStats((detail.stats ?? []).map((stat) => ({
        id: stat.id,
        statType: stat.statType,
        value: toText(stat.value),
      })));
      setMercSkills(detail.skills ?? []);
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
      const skills = mercSkills.map((skill) => skill.trim()).filter(Boolean);
      await req(`/admin/mercenaries/${selectedId}/skills`, { method: 'PUT', body: JSON.stringify({ skills }) });
      notify('✅ 용병 스킬 저장');
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
                            <SmallInput value={skill} onChange={(v) => setMercSkills(rows => rows.map((row, i) => i === idx ? v : row))} placeholder="스킬명" />
                          </td>
                          <td className="py-1 w-20">
                            <Btn color="red" onClick={() => setMercSkills(rows => rows.filter((_, i) => i !== idx))}>삭제</Btn>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="flex gap-2">
                  <Btn color="green" onClick={() => setMercSkills(rows => [...rows, ''])}>행 추가</Btn>
                  <Btn color="yellow" onClick={saveSkills}>저장 (전체 교체)</Btn>
                </div>
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

// ───────────────────────── 메인 관리자 페이지 ─────────────────────────

const TABS = [
  { id: 'crawler', label: '크롤러' },
  { id: 'reports', label: '신고 관리' },
  { id: 'listings', label: '등록글 관리' },
  { id: 'items', label: '아이템' },
  { id: 'mercenaries', label: '용병' },
  { id: 'sets', label: '세트' },
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

      {toast && <Toast msg={toast} onClose={() => setToast('')} />}
    </div>
  );
}
