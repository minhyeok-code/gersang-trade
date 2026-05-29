const BASE = '';

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export function setToken(token: string) {
  localStorage.setItem('accessToken', token);
  window.dispatchEvent(new Event('auth-changed'));
}

export function clearToken() {
  localStorage.removeItem('accessToken');
  window.dispatchEvent(new Event('auth-changed'));
}

export function getTokenRole(): string | null {
  const token = getToken();
  if (!token) return null;

  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    // JWT payload는 base64url 인코딩 — 패딩 보정 후 파싱
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const pad = base64.length % 4;
    if (pad) base64 += '='.repeat(4 - pad);

    const claims = JSON.parse(atob(base64)) as { role?: string };
    return claims.role ?? null;
  } catch {
    return null;
  }
}

export function getServer(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('selectedServer');
}

export function setServer(server: string) {
  if (typeof window === 'undefined') return;
  localStorage.setItem('selectedServer', server);
  window.dispatchEvent(new Event('server-changed'));
}

/** 채팅 메시지 — sentAt 기준 오래된 순(위→아래) */
export function sortChatMessages(messages: ChatMessageDto[]): ChatMessageDto[] {
  return [...messages].sort((a, b) => {
    const ta = a.sentAt ?? a.createdAt ?? '';
    const tb = b.sentAt ?? b.createdAt ?? '';
    return ta.localeCompare(tb);
  });
}

/** localStorage의 serverId → 서버명 (리스팅 API server 필터용) */
export function getSelectedServerName(servers: ServerDto[]): string | null {
  const serverId = getServer();
  if (!serverId) return null;
  return servers.find((s) => String(s.serverId) === serverId)?.name ?? null;
}

async function request<T>(path: string, options: RequestInit = {}, withServer = false): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (withServer) {
    const server = getServer();
    if (server) headers['X-Server-Id'] = server;
  }

  const res = await fetch(`${BASE}${path}`, { ...options, headers, credentials: 'include' });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export const api = {
  // ── 인증 ──
  logout: () => request<void>('/auth/logout', { method: 'POST' }),

  // ── 서버 ──
  getServers: () => request<ServerDto[]>('/api/servers'),

  // ── 아이템 카탈로그 ──
  searchItems: (q: string, params?: { type?: string; kind?: string; limit?: number }) => {
    const sp = new URLSearchParams({
      q,
      ...Object.fromEntries(
        Object.entries(params ?? {})
          .filter(([, v]) => v !== undefined)
          .map(([k, v]) => [k, String(v)])
      ),
    });
    return request<ItemSearchResult[]>(`/api/items/search?${sp}`);
  },
  getItemRituals: (itemId: number) => request<RitualDto[]>(`/api/items/${itemId}/rituals`),
  getItemPriceHistory: (itemId: number, days?: number) => {
    const sp = days ? `?days=${days}` : '';
    return request<PriceHistoryDto>(`/api/items/${itemId}/price-history${sp}`);
  },
  getEquipmentBySlot: (slot: string) => request<EquipmentItemDto[]>(`/api/items/equipment?slot=${slot}`),
  /** 덱 설정 페이지 초기 로딩 — 슬롯별 장비 API를 병렬 호출 */
  getEquipmentByAllSlots: async (slots: string[]) => {
    const results = await Promise.all(
      slots.map((slot) =>
        request<EquipmentItemDto[]>(`/api/items/equipment?slot=${slot}`)
          .then((items) => ({ slot, items }))
          .catch(() => ({ slot, items: [] as EquipmentItemDto[] }))
      )
    );
    return Object.fromEntries(results.map(({ slot, items }) => [slot, items])) as Record<string, EquipmentItemDto[]>;
  },
  getSets: () => request<unknown[]>('/api/sets'),
  getSet: (setId: number) => request<unknown>(`/api/sets/${setId}`),

  // ── 거래 리스팅 ──
  getListings: (params?: Record<string, string>) => {
    const sp = new URLSearchParams(params ?? {});
    return request<ListingDto[]>(`/api/listings?${sp}`, {}, true);
  },
  getListing: (id: number) => request<ListingDto>(`/api/listings/${id}`, {}, true),
  createListing: (body: unknown) =>
    request<ListingDto>('/api/listings', { method: 'POST', body: JSON.stringify(body) }, true),
  deleteListing: (id: number) =>
    request<void>(`/api/listings/${id}`, { method: 'DELETE' }),

  // ── 구매 희망 ──
  getWanted: (params?: Record<string, string>) => {
    const sp = new URLSearchParams(params ?? {});
    return request<WantedDto[]>(`/api/wanted?${sp}`, {}, true);
  },
  getWantedDetail: (id: number) => request<WantedDto>(`/api/wanted/${id}`),
  createWanted: (body: unknown) =>
    request<void>('/api/wanted', { method: 'POST', body: JSON.stringify(body) }),
  deleteWanted: (id: number) => request<void>(`/api/wanted/${id}`, { method: 'DELETE' }),

  // ── 채팅 ──
  getChatRooms: () => request<ChatRoomSummaryDto[]>('/api/chat-rooms'),
  getChatRoom: (id: number) => request<ChatRoomDetailDto>(`/api/chat-rooms/${id}`),
  markChatRoomRead: (roomId: number) =>
    request<void>(`/api/chat-rooms/${roomId}/read`, { method: 'POST' }),
  createChatRoom: (body: unknown) =>
    request<unknown>('/api/chat-rooms', { method: 'POST', body: JSON.stringify(body) }),
  sendMessage: (roomId: number, content: string) =>
    request<ChatMessageDto>(`/api/chat-rooms/${roomId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  confirmTrade: (roomId: number, finalPrice?: number) =>
    request<ChatRoomSummaryDto>(`/api/chat-rooms/${roomId}/trade-confirm`, {
      method: 'POST',
      body: JSON.stringify({ finalPrice: finalPrice ?? null }),
    }),
  /** @deprecated confirmTrade 사용 */
  posterConfirm: (roomId: number, finalPrice?: number) =>
    request<unknown>(`/api/chat-rooms/${roomId}/trade-confirm`, {
      method: 'POST',
      body: JSON.stringify({ finalPrice: finalPrice ?? null }),
    }),
  /** @deprecated confirmTrade 사용 */
  counterpartyConfirm: (roomId: number) =>
    request<unknown>(`/api/chat-rooms/${roomId}/trade-confirm`, { method: 'POST', body: '{}' }),

  // ── 유저 ──
  getMe: () => request<UserDto>('/api/users/me'),
  updateMe: (body: unknown) =>
    request<UserDto>('/api/users/me', { method: 'PATCH', body: JSON.stringify(body) }),
  updateMyServer: (serverId: number) =>
    request<void>('/api/users/me/server', { method: 'PATCH', body: JSON.stringify({ serverId }) }),
  getUser: (userId: number) => request<UserDto>(`/api/users/${userId}`),
  getMyListings: () => request<unknown[]>('/api/users/me/listings'),
  getMyTrades: () => request<unknown[]>('/api/users/me/trades'),
  deleteMe: () => request<void>('/api/users/me', { method: 'DELETE' }),

  // ── 덱 ──
  getDecks: () => request<unknown[]>('/api/decks'),
  createDeck: (body: unknown) =>
    request<unknown>('/api/decks', { method: 'POST', body: JSON.stringify(body) }),
  getDeck: (deckId: number) => request<unknown>(`/api/decks/${deckId}`),
  updateDeck: (deckId: number, body: unknown) =>
    request<unknown>(`/api/decks/${deckId}`, { method: 'PATCH', body: JSON.stringify(body) }),
  getDeckEffectOptions: () => request<DeckEffectCatalogDto>('/api/decks/effect-options'),
  updateDeckEffects: (deckId: number, body: DeckEffectUpdateBody) =>
    request<DeckEffectDto>(`/api/decks/${deckId}/effects`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteDeck: (deckId: number) => request<void>(`/api/decks/${deckId}`, { method: 'DELETE' }),

  // ── 등급 ──
  getMyGrade: () => request<GradeDto>('/api/users/me/grade'),
  getGrades: () => request<unknown[]>('/api/grades'),

  // ── 신고 ──
  createReport: (body: unknown) =>
    request<unknown>('/api/reports', { method: 'POST', body: JSON.stringify(body) }),

  // ── 알림 ──
  getNotifications: () => request<NotificationDto[]>('/api/notifications'),
  markAllRead: () => request<void>('/api/notifications/read-all', { method: 'PATCH' }),
  markRead: (id: number) => request<void>(`/api/notifications/${id}/read`, { method: 'PATCH' }),

  // ── 리뷰 ──
  getReceivedReviews: () => request<unknown[]>('/api/reviews/received'),

  // ── 덱 멤버/슬롯 ──
  addDeckMember: (deckId: number, body: { mercenaryId: number }) =>
    request<unknown>(`/api/decks/${deckId}/members`, { method: 'POST', body: JSON.stringify(body) }),
  removeDeckMember: (deckId: number, memberId: number) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}`, { method: 'DELETE' }),
  getDeckMemberStats: (deckId: number, memberId: number) =>
    request<MemberStatsDto>(`/api/decks/${deckId}/members/${memberId}/stats`),
  getDeckMemberCharacteristics: (deckId: number, memberId: number) =>
    request<MemberCharacteristicDto>(`/api/decks/${deckId}/members/${memberId}/characteristics`),
  updateDeckMemberLevel: (deckId: number, memberId: number, body: { level: 250 | 260 }) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}/level`, { method: 'PATCH', body: JSON.stringify(body) }),
  updateDeckMemberBuild: (
    deckId: number,
    memberId: number,
    body: { level: 250 | 260; bonusTarget: BonusStatTargetDto; bonusAmount: number }
  ) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}/build`, { method: 'PATCH', body: JSON.stringify(body) }),
  setDeckMemberCharacteristics: (deckId: number, memberId: number, body: { characteristics: { characteristicId: number; selectedLevel: number }[] }) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}/characteristics`, { method: 'PUT', body: JSON.stringify(body) }),
  equipSlot: (deckId: number, memberId: number, slot: string, body: { itemId: number }) =>
    request<unknown>(`/api/decks/${deckId}/members/${memberId}/slots/${slot}`, { method: 'PUT', body: JSON.stringify(body) }),
  equipSet: (deckId: number, memberId: number, setId: number) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}/sets/${setId}`, { method: 'PUT' }),
  unequipSlot: (deckId: number, memberId: number, slot: string) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}/slots/${slot}`, { method: 'DELETE' }),
  setSlotRitual: (deckId: number, memberId: number, slot: string, body: { ritualId: number; outcome: 'SUCCESS' | 'GREAT_SUCCESS' }) =>
    request<unknown>(`/api/decks/${deckId}/members/${memberId}/slots/${slot}/ritual`, { method: 'PUT', body: JSON.stringify(body) }),
  removeSlotRitual: (deckId: number, memberId: number, slot: string) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}/slots/${slot}/ritual`, { method: 'DELETE' }),

  // ── 용병 ──
  getMercenaries: (params?: { element?: string; q?: string; limit?: number }) => {
    const sp = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params ?? {})
          .filter(([, v]) => v !== undefined)
          .map(([k, v]) => [k, String(v)])
      )
    );
    const qs = sp.toString() ? `?${sp}` : '';
    return request<MercenaryDto[]>(`/api/mercenaries${qs}`);
  },

  // ── DPS 계산기 ──
  calcDps: (body: unknown) =>
    request<DpsResultDto>('/api/calculator/dps', { method: 'POST', body: JSON.stringify(body) }),
  getMonsters: () => request<MonsterDto[]>('/api/monsters'),
  getMonster: (id: number) => request<MonsterDto>(`/api/monsters/${id}`),

  // ── 기타 유저 ──
  getUserReviews: (userId: number) => request<unknown[]>(`/api/users/${userId}/reviews`),
  submitClearTime: (body: unknown) =>
    request<unknown>('/api/users/me/clear-time', { method: 'POST', body: JSON.stringify(body) }),
  getMyReports: () => request<unknown[]>('/api/reports/me'),
  submitReview: (reviewId: number, body: { rating: string }) =>
    request<void>(`/api/reviews/${reviewId}`, { method: 'POST', body: JSON.stringify(body) }),
  getPriceWatch: () => request<unknown[]>('/api/home/price-watch'),
};

// ── 타입 정의 ──

export interface ServerDto {
  serverId: number;
  name: string;
}

export interface ItemSearchResult {
  id: number;
  name: string;
  type: string;
  equipmentKind?: string;
  slot?: string;
  setId?: number;
  setName?: string;
  stackUnitName?: string;
  imageUrl?: string;
  equipSlot?: string;
}

export interface PriceHistoryDto {
  itemId: number;
  records: { date: string; avgPrice: number; minPrice: number; maxPrice: number; tradeCount: number }[];
}

export interface ListingDto {
  id: number;
  price: number;
  status: string;
  server: string;
  description?: string;
  note?: string;
  createdAt: string;
  seller?: {
    id: number;
    nickname: string;
    gameNickname?: string;
    gameAccessTime?: string;
    grade?: string;
  };
  sellerName?: string;
  sellerGameNickname?: string;
  sellerGameAccessTime?: string;
  bundles?: BundleDto[];
}

export interface BundleDto {
  id: number;
  bundleType: string;
  displayName?: string;
  displayTitle?: string;
  lines?: unknown[];
}

export interface WantedDto {
  id: number;
  /** 목록 API — 구매자 닉네임 (flat) */
  buyerName?: string;
  /** 상세 API 등 — 중첩 buyer (레거시/확장) */
  buyer?: {
    id: number;
    nickname: string;
    gameNickname?: string;
    gameAccessTime?: string;
    grade?: string;
  };
  /** 목록 API */
  offeredPrice?: number;
  price?: number;
  status: string;
  server: string;
  note?: string;
  description?: string;
  /** 목록 API — 구매 희망 아이템명 목록 */
  itemNames?: string[];
  itemName?: string;
  setName?: string;
  createdAt: string;
}

export interface UserDto {
  id: number;
  nickname: string;
  role?: 'USER' | 'ADMIN';
  email: string;
  gameNickname?: string;
  gameAccessTime?: string;
  grade?: string;
  gradeStep?: number;
  totalExp?: number;
  tradeCount?: number;
  status: string;
  createdAt?: string;
  profileImageUrl?: string;
  server?: string;
  serverId?: number;
  serverName?: string;
}

export interface GradeDto {
  grade: string;
  gradeStep: number | null;
  stepUnit: string | null;
  maxStep: number;
  expPerStep: number;
  stepProgressExp: number;
  totalExp: number;
  mannerScore?: number;
  tradeCount?: number;
}

export interface NotificationDto {
  id: number;
  type: string;
  chatRoomId?: number;
  title?: string;
  content?: string;
  message?: string;
  isRead?: boolean;
  read?: boolean;
  createdAt: string;
}

export interface ChatRoomSummaryDto {
  id: number;
  listingType: 'SELL' | 'WANTED';
  listingId: number;
  listingDisplayName?: string;
  initiationType?: 'APPLY' | 'NEGOTIATE';
  partnerNickname: string;
  status: string;
  finalPrice?: number;
  createdAt: string;
  hasUnread?: boolean;
  myTradeConfirmed?: boolean;
  partnerTradeConfirmed?: boolean;
}

export interface ChatMessageDto {
  id: number;
  senderId?: number;
  senderNickname?: string;
  senderRole?: string;
  content: string;
  messageType?: 'TEXT' | 'SYSTEM';
  type?: 'TEXT' | 'SYSTEM';
  sentAt?: string;
  createdAt?: string;
}

export interface ChatRoomDetailDto {
  id: number;
  listingType: 'SELL' | 'WANTED';
  listingId: number;
  listingDisplayName?: string;
  initiationType?: 'APPLY' | 'NEGOTIATE';
  partnerNickname?: string;
  status: string;
  finalPrice?: number;
  createdAt: string;
  posterNickname: string;
  counterpartyNickname: string;
  posterConfirmedAt?: string;
  counterpartyConfirmedAt?: string;
  myTradeConfirmed?: boolean;
  partnerTradeConfirmed?: boolean;
  completedAt?: string;
  messages: ChatMessageDto[];
}

export interface MemberDpsResultDto {
  memberId: number;
  mercenaryId: number;
  mercenaryName: string;
  elementValue: number;
  elementBonus: number;
  rawDps: number;
  elementAdjustedDps: number;
  adjustedDps: number;
  damageShare: number;
}

/** DPS 계산 응답 — rawTotalDps(순수 계수), adjustTotalDps(속성 보정), totalDps(저항 포함 최종) */
export interface DpsResultDto {
  monsterId: number;
  monsterName: string;
  totalResistPierce: number;
  resistAfterDebuff: number;
  resistPassRate: number;
  totalElementPierce: number;
  effectiveMonsterElement: number;
  rawTotalDps: number;
  adjustTotalDps: number;
  totalDps: number;
  memberResults: MemberDpsResultDto[];
}

export interface MonsterDto {
  id: number;
  name: string;
  imageUrl?: string;
  element?: string;
  hp?: number;
  hittingResistance?: number;
  magicResistance?: number;
  elementValue?: number;
  /** 구 API 호환 */
  resistance?: number;
}

export interface MercenaryDto {
  id: number;
  name: string;
  category?: string;
  element?: string;
  resistPierce?: number;
  elementValue?: number;
  imageUrl?: string;
  nation?: string;
}

export type BonusStatTargetDto = 'MAIN_STAT' | 'VITALITY';

export interface MemberStatsDto {
  memberId: number;
  mercenaryId?: number;
  mercenaryName: string;
  mercenaryImageUrl?: string;
  level?: number;
  bonusTarget?: BonusStatTargetDto;
  bonusAmount?: number;
  baseStats: { statType: string; value: number }[];
  equipStats: { statType: string; value: number }[];
  setEffectStats?: { statType: string; value: number }[];
  characteristicStats?: { statType: string; value: number }[];
  partyCharacteristicStats?: { statType: string; value: number }[];
  enemyDebuffStats?: { statType: string; value: number }[];
  ritualStats?: { statType: string; value: number }[];
  ritualSetEffectStats?: { statType: string; value: number }[];
  deckBuffStats?: { statType: string; value: number }[];
  levelBonusStats?: { statType: string; value: number }[];
  bonusStats?: { statType: string; value: number }[];
  protagonistBuffStats?: { statType: string; value: number }[];
  awakenedMyeongwangBuffStats?: { statType: string; value: number }[];
  resolvedMainStat?: string;
  myungwangTransferStats?: { statType: string; value: number }[];
  myungwangTransferDetails?: {
    sourceMercenaryName: string;
    statType: string;
    value: number;
  }[];
  equipmentSetEffects?: {
    setName: string;
    appliedPieces: number;
    requiredPieces: number;
    statType: string;
    statValue: number;
  }[];
  ritualSetEffects?: {
    ritualName: string;
    setName: string;
    outcome: string;
    appliedPieces: number;
    requiredPieces: number;
    statType: string;
    statValue: number;
    statUnit?: 'FLAT' | 'PERCENT';
  }[];
  totalStats: { statType: string; value: number }[];
  slots: {
    slot: string;
    itemId: number;
    itemName: string;
    imageUrl?: string;
    ritual?: SlotRitualDto | null;
  }[];
}

export interface DeckEffectBuffDto {
  statType: string;
  element: string;
  valueType: string;
  value: number;
  target: string;
}

export interface DeckEffectSpiritDto {
  id: number;
  name: string;
  displayLabel: string;
  nature: string;
  grade: string;
  acquireCondition?: string | null;
  specialEffectNote?: string | null;
  buffs: DeckEffectBuffDto[];
}

export interface DeckBuffSourceDto {
  id: number;
  name: string;
  sourceType: 'JINBEOP' | 'CHEUNGJIN' | 'LEGEND_GENERAL';
  sourceId: number;
  buffs: DeckEffectBuffDto[];
}

export interface DeckEffectDto {
  spirits: DeckEffectSpiritDto[];
  jinbeop?: DeckBuffSourceDto | null;
  cheungjin?: DeckBuffSourceDto | null;
  stats: { statType: string; value: number }[];
}

export interface DeckEffectCatalogDto {
  spirits: DeckEffectSpiritDto[];
  jinbeops: DeckBuffSourceDto[];
  cheungjins: DeckBuffSourceDto[];
}

export interface DeckEffectUpdateBody {
  spirit1Id?: number | null;
  spirit2Id?: number | null;
  jinbeopSourceId?: number | null;
  cheungjinSourceId?: number | null;
}

export interface MemberCharacteristicDto {
  memberId: number;
  level: number;
  maxCharacteristicPoints?: number | null;
  maxPoints?: number | null;
  characteristics: {
    characteristicId: number;
    key: string;
    name: string;
    point?: number | null;
    description?: string | null;
    requiredCharacteristicKey?: string | null;
    applyType: string;
    selectedLevel?: number | null;
    levels: {
      label: string;
      level: number;
      amount: string;
      amountValue?: number | null;
      statType?: string | null;
    }[];
  }[];
}

export interface DeckDetailDto {
  id: number;
  name: string;
  isActive: boolean;
  effects?: DeckEffectDto | null;
  members: {
    id: number;
    mercenaryId: number;
    mercenaryName: string;
    mercenaryImageUrl?: string;
    imageUrl?: string;
    level?: number;
    bonusTarget?: BonusStatTargetDto;
    bonusAmount?: number;
    slots: {
      slot: string;
      itemId: number;
      itemName: string;
      imageUrl?: string;
      ritual?: SlotRitualDto | null;
    }[];
  }[];
}

export interface RitualDto {
  id: number;
  displayName: string;
  ritualType?: 'WEAPON' | 'ARMOR';
  successMark?: string;
  greatSuccessMark?: string | null;
}

export interface SlotRitualDto {
  ritualId: number;
  displayName: string;
  outcome: 'SUCCESS' | 'GREAT_SUCCESS';
}

export interface EquipmentItemDto {
  id?: number;
  itemId: number;
  name: string;
  slot?: string;
  equipSlot?: string;
  equipmentKind?: string;
  imageUrl?: string;
  ritualApplicable?: boolean;
  hasSlotOption?: boolean;
  setId?: number;
  setName?: string;
  stats?: { statType: string; value: number; element?: string; scope?: string }[];
}
