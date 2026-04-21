package org.example.gersangtrade.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.chat.dto.request.ChatMessageSendRequest;
import org.example.gersangtrade.chat.dto.request.ChatRoomCreateRequest;
import org.example.gersangtrade.chat.dto.request.PosterConfirmRequest;
import org.example.gersangtrade.chat.dto.response.ChatMessageResponse;
import org.example.gersangtrade.chat.dto.response.ChatRoomDetailResponse;
import org.example.gersangtrade.chat.dto.response.ChatRoomSummaryResponse;
import org.example.gersangtrade.chat.repository.ChatMessageRepository;
import org.example.gersangtrade.chat.repository.ChatRoomRepository;
import org.example.gersangtrade.domain.chat.ChatMessage;
import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatMessageType;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.notification.service.NotificationService;
import org.example.gersangtrade.report.service.KeywordDetectionService;
import org.example.gersangtrade.trade.repository.TradeConfirmedRepository;
import org.example.gersangtrade.trade.repository.TradeReviewRepository;
import org.example.gersangtrade.trade.service.TradeStatService;
import org.example.gersangtrade.user.util.ExpGradeCalculator;
import org.example.gersangtrade.wanted.repository.WantedItemRepository;
import org.example.gersangtrade.wanted.repository.WantedListingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 서비스.
 * 채팅방 생성·메시지 전송·거래완료 확인(2단계) 흐름을 담당한다.
 *
 * 거래완료 순서: 게시자(poster) 확인 → 상대방(counterparty) 확인 → TradeConfirmed 생성.
 * 상세 흐름: docs/trade-flow-design.ko.md 참고.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final TradeListingRepository tradeListingRepository;
    private final WantedListingRepository wantedListingRepository;
    private final ListingBundleRepository listingBundleRepository;
    private final BundleLineRepository bundleLineRepository;
    private final WantedItemRepository wantedItemRepository;
    private final TradeConfirmedRepository tradeConfirmedRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final TradeStatService tradeStatService;
    private final NotificationService notificationService;
    private final KeywordDetectionService keywordDetectionService;

    // ──────────────────────────────────────────────────────────────────────
    // 채팅방 생성
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 채팅방을 생성한다 — 흥정하기(NEGOTIATE) 또는 거래신청(APPLY) 공통 진입점.
     *
     * - 본인 게시물에 채팅 시도 시 예외 발생
     * - 동일 게시물에 이미 OPEN 상태의 채팅방이 있으면 기존 채팅방 반환
     * - 채팅방 생성 후 게시자에게 CHAT_OPENED 알림 저장
     */
    @Transactional
    public ChatRoomSummaryResponse createChatRoom(Long userId, ChatRoomCreateRequest request) {
        User counterparty = loadActiveUser(userId);

        // 게시물 및 게시자 조회
        User poster = loadPoster(request.listingType(), request.listingId());

        // 본인 게시물에 채팅 불가
        if (poster.getId().equals(userId)) {
            throw new IllegalStateException("본인 게시물에는 채팅을 개설할 수 없습니다.");
        }

        // 이미 OPEN 상태의 채팅방이 있으면 기존 채팅방 반환
        boolean exists = chatRoomRepository.existsByListingTypeAndListingIdAndCounterpartyIdAndStatus(
                request.listingType(), request.listingId(), userId, ChatRoomStatus.OPEN);
        if (exists) {
            // 기존 채팅방을 찾아서 반환
            ChatRoom existing = chatRoomRepository
                    .findByListingTypeAndListingId(request.listingType(), request.listingId())
                    .stream()
                    .filter(r -> r.getCounterparty().getId().equals(userId) && r.getStatus() == ChatRoomStatus.OPEN)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("채팅방 조회 중 오류가 발생했습니다."));
            return ChatRoomSummaryResponse.of(existing, userId);
        }

        // 새 채팅방 생성
        ChatRoom room = ChatRoom.builder()
                .listingType(request.listingType())
                .listingId(request.listingId())
                .initiationType(request.initiationType())
                .poster(poster)
                .counterparty(counterparty)
                .build();
        chatRoomRepository.save(room);

        // 입장 시스템 메시지 저장
        saveSystemMessage(room, "채팅방이 개설되었습니다. 서로 존중하는 거래 문화를 만들어 주세요.");

        // 게시자에게 알림
        saveNotification(poster, NotificationType.CHAT_OPENED, room.getId(),
                counterparty.getNickname() + "님이 채팅을 요청했습니다.");

        return ChatRoomSummaryResponse.of(room, userId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 채팅방 목록 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 내가 참여 중인 채팅방 목록을 반환한다 (게시자·상대방 모두 포함).
     */
    public List<ChatRoomSummaryResponse> getMyChatRooms(Long userId) {
        List<ChatRoom> asPoster = chatRoomRepository.findByPosterId(userId);
        List<ChatRoom> asCounterparty = chatRoomRepository.findByCounterpartyId(userId);

        return java.util.stream.Stream.concat(asPoster.stream(), asCounterparty.stream())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(room -> ChatRoomSummaryResponse.of(room, userId))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 채팅방 상세 + 메시지 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 채팅방 상세 정보와 메시지 목록을 반환한다 (최신 50건).
     * 채팅방 참여자(poster 또는 counterparty)만 조회 가능하다.
     */
    public ChatRoomDetailResponse getChatRoomDetail(Long userId, Long chatRoomId) {
        ChatRoom room = loadChatRoom(chatRoomId);
        validateParticipant(room, userId);

        Slice<ChatMessage> messages = chatMessageRepository.findActiveByRoomId(
                chatRoomId, PageRequest.of(0, 50));

        List<ChatMessageResponse> msgResponses = messages.getContent().stream()
                .map(ChatMessageResponse::of)
                .collect(Collectors.toList());

        return ChatRoomDetailResponse.of(room, msgResponses);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 메시지 전송
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 메시지를 전송한다.
     *
     * - 채팅방 참여자만 전송 가능
     * - OPEN 또는 POSTER_CONFIRMED 상태의 채팅방에서만 전송 가능
     * - 메시지 저장 후 KeywordDetectionService로 현금거래 키워드 감지 (Soft 방식)
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long chatRoomId, ChatMessageSendRequest request) {
        User sender = loadActiveUser(userId);
        ChatRoom room = loadChatRoom(chatRoomId);
        validateParticipant(room, userId);

        // OPEN 또는 POSTER_CONFIRMED 상태에서만 메시지 전송 가능
        if (room.getStatus() != ChatRoomStatus.OPEN && room.getStatus() != ChatRoomStatus.POSTER_CONFIRMED) {
            throw new IllegalStateException("종료된 채팅방에는 메시지를 전송할 수 없습니다.");
        }

        // 메시지 저장
        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.content())
                .messageType(ChatMessageType.TEXT)
                .flagged(false)
                .flagReason(null)
                .build();
        chatMessageRepository.save(msg);

        // 현금거래 키워드 감지 (Soft 방식 — 메시지 전송은 허용, 자동 신고만 생성)
        keywordDetectionService.detect(request.content(), msg.getId(), chatRoomId);

        // 상대방에게 채팅 알림
        User recipient = room.getPoster().getId().equals(userId) ? room.getCounterparty() : room.getPoster();
        saveNotification(recipient, NotificationType.CHAT_MESSAGE, chatRoomId,
                sender.getNickname() + "님이 메시지를 보냈습니다.");

        return ChatMessageResponse.of(msg);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 거래완료 확인 (1단계) — 게시자
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 게시자가 거래완료를 요청한다 (1단계).
     * finalPrice가 null이면 게시물 원래 가격을 사용한다.
     * OPEN → POSTER_CONFIRMED 상태 전이 후 상대방에게 알림.
     */
    @Transactional
    public ChatRoomSummaryResponse posterConfirm(Long userId, Long chatRoomId, PosterConfirmRequest request) {
        ChatRoom room = loadChatRoom(chatRoomId);

        // 게시자만 호출 가능
        if (!room.getPoster().getId().equals(userId)) {
            throw new IllegalStateException("게시자만 거래완료를 먼저 요청할 수 있습니다.");
        }
        // OPEN 상태에서만 호출 가능
        if (room.getStatus() != ChatRoomStatus.OPEN) {
            throw new IllegalStateException("현재 상태에서는 거래완료를 요청할 수 없습니다. (현재: " + room.getStatus() + ")");
        }

        room.posterConfirm(request.finalPrice());

        saveSystemMessage(room, "게시자가 거래완료를 요청했습니다. 거래 내용을 확인 후 거래완료 버튼을 눌러주세요.");

        saveNotification(room.getCounterparty(), NotificationType.POSTER_CONFIRMED, chatRoomId,
                room.getPoster().getNickname() + "님이 거래완료를 요청했습니다. 확인 후 거래완료 버튼을 눌러주세요.");

        return ChatRoomSummaryResponse.of(room, userId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 거래완료 확인 (2단계) — 상대방
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 상대방(counterparty)이 거래완료를 확인한다 (2단계).
     *
     * 처리 순서 (단일 트랜잭션):
     *  1. ChatRoom → COMPLETED
     *  2. 거래 확정 가격 결정 (finalPrice 또는 게시물 가격)
     *  3. TradeConfirmed 생성 (통계 집계의 원천 데이터)
     *  4. 양측 EXP 지급 및 등급 갱신
     *  5. 양측 tradeCount 증가
     *  6. TradeReview 2건 생성 (블라인드, revealAt = now + 3일)
     *  7. 게시물 상태 SOLD/PURCHASED 전환
     *  8. 동일 게시물의 다른 OPEN/POSTER_CONFIRMED 채팅방 CLOSED 처리
     *  9. 양측에 TRADE_COMPLETED + REVIEW_REQUESTED 알림 저장
     */
    @Transactional
    public ChatRoomSummaryResponse counterpartyConfirm(Long userId, Long chatRoomId) {
        ChatRoom room = loadChatRoom(chatRoomId);

        // 상대방만 호출 가능
        if (!room.getCounterparty().getId().equals(userId)) {
            throw new IllegalStateException("상대방만 거래완료를 최종 확인할 수 있습니다.");
        }
        // POSTER_CONFIRMED 상태에서만 호출 가능
        if (room.getStatus() != ChatRoomStatus.POSTER_CONFIRMED) {
            throw new IllegalStateException("게시자가 먼저 거래완료를 요청해야 합니다. (현재: " + room.getStatus() + ")");
        }

        room.counterpartyConfirm();

        User poster = room.getPoster();
        User counterparty = room.getCounterparty();

        // 거래 확정 가격 결정 (finalPrice 없으면 게시물 원래 가격)
        long confirmedPrice = resolveConfirmedPrice(room);

        // 서버명 스냅샷 조회
        String serverSnapshot = resolveServerSnapshot(room);

        // 통계 집계 키 결정 ("ITEM:{itemId}" 형식)
        String statKeySnapshot = resolveStatKey(room);

        // TradeConfirmed 생성
        User seller = (room.getListingType() == ListingType.SELL) ? poster : counterparty;
        User buyer = (room.getListingType() == ListingType.SELL) ? counterparty : poster;

        TradeConfirmed confirmed = TradeConfirmed.builder()
                .chatRoom(room)
                .listingType(room.getListingType())
                .seller(seller)
                .buyer(buyer)
                .serverSnapshot(serverSnapshot)
                .confirmedPrice(confirmedPrice)
                .statKeySnapshot(statKeySnapshot)
                .confirmedAt(LocalDateTime.now())
                .build();
        tradeConfirmedRepository.save(confirmed);

        // 일별 시세 통계 upsert (거래 확정 이벤트 기반 집계)
        tradeStatService.upsertDailyStat(statKeySnapshot, confirmedPrice, 1L, LocalDate.now());

        // EXP 지급 및 등급 갱신
        long expDelta = calculateTradeExp(confirmedPrice);
        applyExpAndGrade(poster, expDelta);
        applyExpAndGrade(counterparty, expDelta);

        // 거래 횟수 증가
        poster.incrementTradeCount();
        counterparty.incrementTradeCount();

        // 블라인드 거래 평가 생성 (revealAt = 3일 후)
        LocalDateTime revealAt = LocalDateTime.now().plusDays(3);
        tradeReviewRepository.save(TradeReview.builder()
                .tradeConfirmed(confirmed)
                .reviewer(poster)
                .target(counterparty)
                .revealAt(revealAt)
                .build());
        tradeReviewRepository.save(TradeReview.builder()
                .tradeConfirmed(confirmed)
                .reviewer(counterparty)
                .target(poster)
                .revealAt(revealAt)
                .build());

        // 게시물 상태 SOLD / PURCHASED 전환
        completeListingStatus(room);

        // 동일 게시물의 다른 OPEN/POSTER_CONFIRMED 채팅방 종료
        closeOtherRooms(room);

        // 양측 알림 저장
        String tradeCompleteMsg = "거래가 완료됐어요. 상대방을 3일 이내에 평가해보세요.";
        saveNotification(poster, NotificationType.TRADE_COMPLETED, chatRoomId, tradeCompleteMsg);
        saveNotification(counterparty, NotificationType.TRADE_COMPLETED, chatRoomId, tradeCompleteMsg);
        saveNotification(poster, NotificationType.REVIEW_REQUESTED, chatRoomId,
                counterparty.getNickname() + "님과의 거래를 평가해주세요.");
        saveNotification(counterparty, NotificationType.REVIEW_REQUESTED, chatRoomId,
                poster.getNickname() + "님과의 거래를 평가해주세요.");

        // 어뷰징 탐지: 7일 이내 동일 두 유저 간 3건 이상 거래 → TODO 관리자 알림
        // checkAbuse(poster, counterparty);

        return ChatRoomSummaryResponse.of(room, userId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // private 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /** 활성 상태 사용자 조회 */
    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("차단된 계정은 채팅을 이용할 수 없습니다.");
        }
        return user;
    }

    /** 게시물 종류에 따라 게시자를 반환한다 */
    private User loadPoster(ListingType listingType, Long listingId) {
        if (listingType == ListingType.SELL) {
            TradeListing listing = tradeListingRepository.findById(listingId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매 게시물입니다."));
            return listing.getSeller();
        } else {
            WantedListing listing = wantedListingRepository.findById(listingId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매 게시물입니다."));
            return listing.getBuyer();
        }
    }

    /** 채팅방 조회 */
    private ChatRoom loadChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
    }

    /** 채팅방 참여자(poster 또는 counterparty) 여부 검증 */
    private void validateParticipant(ChatRoom room, Long userId) {
        boolean isPoster = room.getPoster().getId().equals(userId);
        boolean isCounterparty = room.getCounterparty().getId().equals(userId);
        if (!isPoster && !isCounterparty) {
            throw new IllegalStateException("해당 채팅방의 참여자가 아닙니다.");
        }
    }

    /** 거래 확정 가격 결정 (finalPrice 없으면 게시물 원래 가격) */
    private long resolveConfirmedPrice(ChatRoom room) {
        if (room.getFinalPrice() != null) {
            return room.getFinalPrice();
        }
        if (room.getListingType() == ListingType.SELL) {
            return tradeListingRepository.findById(room.getListingId())
                    .map(TradeListing::getPrice)
                    .orElseThrow(() -> new IllegalStateException("게시물 가격을 조회할 수 없습니다."));
        } else {
            return wantedListingRepository.findById(room.getListingId())
                    .map(WantedListing::getOfferedPrice)
                    .orElseThrow(() -> new IllegalStateException("게시물 가격을 조회할 수 없습니다."));
        }
    }

    /** 서버명 스냅샷 조회 */
    private String resolveServerSnapshot(ChatRoom room) {
        if (room.getListingType() == ListingType.SELL) {
            return tradeListingRepository.findById(room.getListingId())
                    .map(TradeListing::getServer)
                    .orElse("알 수 없음");
        } else {
            return wantedListingRepository.findById(room.getListingId())
                    .map(WantedListing::getServer)
                    .orElse("알 수 없음");
        }
    }

    /**
     * 거래 통계 집계 키를 결정한다.
     * 게시물의 첫 번째 아이템 ID를 기준으로 "ITEM:{itemId}" 형식의 키를 반환한다.
     * 아이템을 찾을 수 없는 경우 listingType:listingId 형식으로 fallback한다.
     */
    private String resolveStatKey(ChatRoom room) {
        try {
            if (room.getListingType() == ListingType.SELL) {
                // 판매 등록글: 첫 번째 번들의 첫 번째 라인 아이템 ID
                List<org.example.gersangtrade.domain.listing.ListingBundle> bundles =
                        listingBundleRepository.findByListingIdOrderByIdAsc(room.getListingId());
                if (!bundles.isEmpty()) {
                    List<org.example.gersangtrade.domain.listing.BundleLine> lines =
                            bundleLineRepository.findByBundleIdOrderBySortOrderAsc(bundles.get(0).getId());
                    if (!lines.isEmpty()) {
                        return "ITEM:" + lines.get(0).getItem().getId();
                    }
                }
            } else {
                // 구매 희망 등록글: 첫 번째 WantedItem의 아이템 ID
                List<org.example.gersangtrade.domain.wanted.WantedItem> items =
                        wantedItemRepository.findByWantedListingIdOrderBySortOrderAsc(room.getListingId());
                if (!items.isEmpty()) {
                    return "ITEM:" + items.get(0).getItem().getId();
                }
            }
        } catch (Exception ignored) {
            // 아이템 조회 실패 시 fallback
        }
        // fallback: listingType:listingId
        return room.getListingType().name() + ":" + room.getListingId();
    }

    /**
     * 거래 금액 구간별 EXP 계산.
     * 기본 20 EXP + 금액 구간 보너스.
     * 단위: 골드 (1억 = 100,000,000)
     */
    private long calculateTradeExp(long confirmedPrice) {
        long baseExp = 20L;
        long bonusExp;
        if (confirmedPrice < 100_000_000L) {
            bonusExp = 10L;        // 1억 미만
        } else if (confirmedPrice < 500_000_000L) {
            bonusExp = 20L;        // 1억 ~ 5억
        } else if (confirmedPrice < 1_500_000_000L) {
            bonusExp = 35L;        // 5억 ~ 15억
        } else if (confirmedPrice < 5_000_000_000L) {
            bonusExp = 55L;        // 15억 ~ 50억
        } else {
            bonusExp = 80L;        // 50억 이상
        }
        return baseExp + bonusExp;
    }

    /** EXP를 지급하고 등급·호봉을 갱신한다 */
    private void applyExpAndGrade(User user, long expDelta) {
        ExpGradeCalculator.GradeAndStep result =
                ExpGradeCalculator.calculate(user.getTotalExp(), expDelta);
        // applyExp는 내부에서 totalExp += expDelta 처리하므로 delta 전달
        user.applyExp(expDelta, result.grade(), result.step());
    }

    /** 거래 완료 시 게시물 상태를 SOLD 또는 PURCHASED로 전환한다 */
    private void completeListingStatus(ChatRoom room) {
        if (room.getListingType() == ListingType.SELL) {
            tradeListingRepository.findById(room.getListingId())
                    .ifPresent(TradeListing::completeTrade);
        } else {
            wantedListingRepository.findById(room.getListingId())
                    .ifPresent(WantedListing::completePurchase);
        }
    }

    /** 동일 게시물의 다른 OPEN/POSTER_CONFIRMED 채팅방을 모두 종료한다 */
    private void closeOtherRooms(ChatRoom completedRoom) {
        chatRoomRepository
                .findByListingTypeAndListingId(completedRoom.getListingType(), completedRoom.getListingId())
                .stream()
                .filter(r -> !r.getId().equals(completedRoom.getId()))
                .filter(r -> r.getStatus() == ChatRoomStatus.OPEN
                          || r.getStatus() == ChatRoomStatus.POSTER_CONFIRMED)
                .forEach(r -> {
                    r.close();
                    saveSystemMessage(r, "해당 게시물의 거래가 다른 채팅방에서 완료되어 채팅방이 종료되었습니다.");
                });
    }

    /** 시스템 메시지를 저장한다 */
    private void saveSystemMessage(ChatRoom room, String content) {
        ChatMessage sysMsg = ChatMessage.builder()
                .chatRoom(room)
                .sender(null)
                .content(content)
                .messageType(ChatMessageType.SYSTEM)
                .flagged(false)
                .flagReason(null)
                .build();
        chatMessageRepository.save(sysMsg);
    }

    /** 알림을 저장하고 SSE push한다 */
    private void saveNotification(User user, NotificationType type, Long chatRoomId, String message) {
        notificationService.send(user, type, chatRoomId, message);
    }
}
