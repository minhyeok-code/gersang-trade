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
  getRituals: () => request<RitualDto[]>('/api/rituals'),
  getItemPriceHistory: (itemId: number, days?: number) => {
    const sp = days ? `?days=${days}` : '';
    return request<PriceHistoryDto>(`/api/items/${itemId}/price-history${sp}`);
  },
  getEquipmentBySlot: (slot: string) => request<EquipmentItemDto[]>(`/api/items/equipment?slot=${slot}`),
  /** 용병 전용장비 — restriction 기준 조회 (덱 장비 선택 UI) */
  getExclusiveEquipment: (mercenaryId: number, slot: string) =>
    request<EquipmentItemDto[]>(`/api/mercenaries/${mercenaryId}/exclusive-equipment?slot=${slot}`),
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
  getSets: (name?: string) => {
    const sp = new URLSearchParams({ size: '20' });
    if (name) sp.set('name', name);
    return request<{ content: SetSummaryDto[]; totalElements: number }>(`/api/sets?${sp}`);
  },
  getSet: (setId: number) => request<SetDetailDto>(`/api/sets/${setId}`),

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
  getUser: (userId: number) => request<PublicUserDto>(`/api/users/${userId}`),
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
  getPendingReviews: () => request<PendingReviewDto[]>('/api/reviews/pending'),

  // ── 덱 멤버/슬롯 ──
  addDeckMember: (deckId: number, body: { mercenaryId: number }) =>
    request<unknown>(`/api/decks/${deckId}/members`, { method: 'POST', body: JSON.stringify(body) }),
  removeDeckMember: (deckId: number, memberId: number) =>
    request<void>(`/api/decks/${deckId}/members/${memberId}`, { method: 'DELETE' }),
  getDeckMemberElementValues: (deckId: number) =>
    request<MemberElementValueDto[]>(`/api/decks/${deckId}/members/element-values`),
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
  getMercenaryCharacteristics: (mercenaryId: number) =>
    request<MercenaryCharacteristicCatalogDto>(`/api/mercenaries/${mercenaryId}/characteristics`),
  getMercenaryCharacteristicSetup: (mercenaryId: number) =>
    request<MercenaryCharacteristicSetupDto>(`/api/mercenaries/${mercenaryId}/characteristics/setup`),

  // ── DPS 계산기 ──
  calcDps: (body: unknown) =>
    request<DpsResultDto>('/api/calculator/dps', { method: 'POST', body: JSON.stringify(body) }),
  evaluateDpsValue: (body: DpsEvaluationRequestBody) =>
    request<DpsValueEvaluationResponseDto>('/api/calculator/dps/evaluations', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  getMyDpsEvaluations: (page = 0, size = 20) =>
    request<PageDto<DpsEvaluationSummaryDto>>(
      `/api/calculator/dps/evaluations?page=${page}&size=${size}`
    ),
  getMyDpsEvaluation: (id: number) =>
    request<DpsValueEvaluationResponseDto>(`/api/calculator/dps/evaluations/${id}`),
  deleteMyDpsEvaluation: (id: number) =>
    request<void>(`/api/calculator/dps/evaluations/${id}`, { method: 'DELETE' }),
  getMonsters: () => request<MonsterDto[]>('/api/monsters'),
  getMonster: (id: number) => request<MonsterDto>(`/api/monsters/${id}`),

  // ── 기타 유저 ──
  getUserReviews: (userId: number) => request<unknown[]>(`/api/users/${userId}/reviews`),
  submitClearTime: (body: ClearTimeSubmitBody) =>
    request<ClearTimeResponseDto>('/api/users/me/clear-time', { method: 'POST', body: JSON.stringify(body) }),
  getMyClearTimes: () => request<MyClearTimeDto[]>('/api/users/me/clear-times'),
  getHuntHubStatus: () => request<HuntHubStatusDto>('/api/users/me/hunt-hub-status'),
  getHuntMonsters: () => request<HuntMonsterSummaryDto[]>('/api/hunt/monsters'),
  getHuntMonsterRecords: (monsterId: number) =>
    request<HuntPublicRecordDto[]>(`/api/hunt/monsters/${monsterId}/records`),
  getHuntSnapshot: (snapshotId: number) =>
    request<HuntSnapshotDto>(`/api/hunt/snapshots/${snapshotId}`),
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
  exclusiveMercenaryId?: number;
  restrictionMercenaryIds?: number[];
}

export interface PriceHistoryDto {
  itemId: number;
  records: { date: string; avgPrice: number; minPrice: number; maxPrice: number; tradeCount: number }[];
}

export interface ListingDto {
  id: number;
  sellerId?: number;
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
  mannerScore?: number;
  tradeCount?: number;
  status: string;
  createdAt?: string;
  profileImageUrl?: string;
  server?: string;
  serverId?: number;
  serverName?: string;
}

export interface PublicUserDto {
  id: number;
  nickname: string;
  profileImageUrl?: string;
  gameNickname?: string;
  gameAccessTime?: string;
  grade?: string;
  gradeStep?: number;
  tradeCount?: number;
  mannerScore?: number;
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

export interface PendingReviewDto {
  reviewId: number;
  counterpartyNickname: string;
  revealAt: string;
  chatRoomId: number | null;
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
  partnerId?: number;
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
export interface ClearTimeSubmitBody {
  monsterId: number;
  deckId: number;
  clearTimeSeconds: number;
  isPublic?: boolean;
}

export interface MyClearTimeDto {
  id: number;
  monsterId: number;
  monsterName: string;
  clearTimeSeconds: number;
  totalResistPierce?: number | null;
  totalElementPierce?: number | null;
  rawDps?: number | null;
  adjustDps?: number | null;
  finalDps: number;
  resistAfterDebuff?: number | null;
  effectiveMonsterElement?: number | null;
  resistPassRate?: number | null;
  deckSnapshotId: number;
  isPublic: boolean;
  recordedAt: string;
}

export interface HuntHubStatusDto {
  distinctMonsterCount: number;
  requiredDistinctMonsters: number;
  unlocked: boolean;
}

export interface HuntMonsterSummaryDto {
  monsterId: number;
  monsterName: string;
  publicRecordCount: number;
}

export interface HuntPublicRecordDto {
  id: number;
  monsterId: number;
  monsterName: string;
  clearTimeSeconds: number;
  totalResistPierce?: number | null;
  totalElementPierce?: number | null;
  rawDps?: number | null;
  adjustDps?: number | null;
  finalDps: number;
  resistAfterDebuff?: number | null;
  effectiveMonsterElement?: number | null;
  resistPassRate?: number | null;
  deckSnapshotId: number;
  authorNickname: string;
  recordedAt: string;
}

/** 백엔드 DeckSnapshotContent — 클리어타임 저장 시 고정되는 덱 JSON */
export interface DeckSnapshotCharacteristicSelectionDto {
  characteristicId: number;
  selectedLevel: number;
}

export interface DeckSnapshotMemberDto {
  member: DeckDetailDto['members'][number];
  characteristics: DeckSnapshotCharacteristicSelectionDto[];
}

export interface DeckSnapshotDpsContextDto {
  resistanceType: 'HITTING' | 'MAGIC';
  memberInputs: {
    memberId: number;
    level: number;
    bonusTarget: BonusStatTargetDto;
    bonusAmount: number;
  }[];
  memberElementValues?: {
    memberId: number;
    elementValue: number;
  }[];
}

export interface DeckSnapshotContentDto {
  deckId: number;
  deckName: string;
  attrXValue?: number | null;
  totalResDown?: number | null;
  effects?: DeckEffectDto | null;
  members: DeckSnapshotMemberDto[];
  dpsContext: DeckSnapshotDpsContextDto;
}

export interface HuntSnapshotDto {
  id: number;
  content: DeckSnapshotContentDto;
  createdAt: string;
}

export interface ClearTimeResponseDto {
  id: number;
  monsterId: number;
  monsterName: string;
  deckId: number;
  deckSnapshotId: number;
  clearTimeSeconds: number;
  rawDps: number;
  adjustDps: number;
  finalDps: number;
  isPublic: boolean;
  expEarned: number;
  recordedAt: string;
}

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

/** Spring Data Page 응답 */
export interface PageDto<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type ScenarioItemTypeDto = 'ITEM_SINGLE' | 'ITEM_SET' | 'MERCENARY';
export type MercenaryModeDto = 'REPLACE' | 'APPEND';
export type PriceSourceDto = 'USER_INPUT' | 'TRADE_STAT' | 'MIXED' | 'MISSING';
export type ResistanceTypeDto = 'HITTING' | 'MAGIC';

export interface DpsTripleDto {
  raw: number;
  adjust: number;
  finalDps: number;
}

export interface DpsRateTripleDto {
  raw: number;
  adjust: number;
  finalDps: number;
}

export interface EfficiencyTripleDto {
  raw: number | null;
  adjust: number | null;
  finalDps: number | null;
}

export interface DpsValueEvaluationResponseDto {
  persisted: boolean;
  evaluationId: number | null;
  scenarioDeckSnapshotId: number | null;
  before: DpsTripleDto;
  after: DpsTripleDto;
  delta: DpsTripleDto;
  increaseRate: DpsRateTripleDto;
  efficiencyPerEok: EfficiencyTripleDto;
  price: number | null;
  formattedPrice: string | null;
  priceSource: PriceSourceDto;
  tradeCount: number | null;
}

export interface DpsEvaluationSummaryDto {
  evaluationId: number;
  candidateType: ScenarioItemTypeDto;
  candidateLabel: string;
  candidateRef: number;
  mercenaryMode: MercenaryModeDto | null;
  monsterId: number;
  monsterName: string;
  finalDpsIncreaseRate: number;
  efficiencyPerEokFinal: number | null;
  price: number | null;
  formattedPrice: string | null;
  priceSource: PriceSourceDto;
  createdAt: string;
}

export interface ScenarioLineBody {
  itemId: number;
  quantity: number;
  sortOrder: number;
  equipmentDetail?: {
    enhanceLevel?: number;
    hasRitual?: boolean;
    rituals?: { ritualId: number; outcome: 'SUCCESS' | 'GREAT_SUCCESS' }[];
  };
}

export interface ScenarioRequestBody {
  type: ScenarioItemTypeDto;
  setId?: number | null;
  affectedMemberId?: number | null;
  lines?: ScenarioLineBody[];
  mercenaryId?: number | null;
  mode?: MercenaryModeDto | null;
  level?: number | null;
  bonusTarget?: BonusStatTargetDto | null;
  bonusAmount?: number | null;
  characteristics?: { characteristicId: number; selectedLevel: number }[];
}

export interface DpsEvaluationRequestBody {
  deckId: number;
  monsterId: number;
  resistanceType: ResistanceTypeDto;
  scenario: ScenarioRequestBody;
  priceOverrides?: Record<string, number>;
  price?: number | null;
  memberInputs: {
    memberId: number;
    level: 250 | 260;
    bonusTarget: BonusStatTargetDto;
    bonusAmount: number;
  }[];
  persist: boolean;
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

export interface MercenaryCharacteristicCatalogDto {
  mercenaryId: number;
  characteristics: {
    characteristicId: number;
    name: string;
    levels: { level: number; label: string | null }[];
  }[];
}

export interface MercenaryCharacteristicSetupDto {
  mercenaryId: number;
  maxCharacteristicPoints: number;
  characteristics: {
    characteristicId: number;
    key: string;
    name: string;
    point?: number | null;
    description?: string | null;
    requiredCharacteristicKey?: string | null;
    applyType: string;
    levels: {
      label?: string | null;
      level: number;
      amount?: string | null;
      amountValue?: number | null;
      statType?: string | null;
      element?: string | null;
    }[];
  }[];
}

export interface MemberElementValueDto {
  memberId: number;
  elementValue: number;
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
  deckBuffDetails?: { sourceName: string; sourceType: string; statType: string; value: number }[];
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
  partyItemBuffStats?: { statType: string; value: number }[] | null;
  lgAllyElementalStats?: { statKey: string; value: number }[] | null;
  lgAllyDetails?: { mercenaryName: string; statKey: string; value: number }[] | null;
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
  gonmyeongLevel?: number | null;
  gahoLevel?: number | null;
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
  gonmyeongLevel?: number | null;
  gahoLevel?: number | null;
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
      label?: string | null;
      level: number;
      amount?: string | null;
      amountValue?: number | null;
      statType?: string | null;
      element?: string | null;
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

export interface SetSummaryDto {
  id: number;
  name: string;
  totalPieces: number;
}

export interface SetPieceDto {
  itemId: number;
  itemName: string;
  imageUrl?: string;
  slot: string;
  equipSlot?: string;
  ritualApplicable: boolean;
}

export interface SetDetailDto {
  id: number;
  name: string;
  totalPieces: number;
  pieces: SetPieceDto[];
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
  exclusiveMercenaryId?: number | null;
  exclusiveMercenaryName?: string | null;
  restrictionMercenaryIds?: number[];
  stats?: { statType: string; value: number; element?: string; scope?: string }[];
}
