package org.example.gersangtrade.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.chat.dto.request.ChatMessageSendRequest;
import org.example.gersangtrade.chat.dto.request.ChatRoomCreateRequest;
import org.example.gersangtrade.chat.dto.request.PosterConfirmRequest;
import org.example.gersangtrade.chat.dto.response.ChatMessageResponse;
import org.example.gersangtrade.chat.dto.response.ChatMessageSseEvent;
import org.example.gersangtrade.chat.dto.response.ChatRoomDetailResponse;
import org.example.gersangtrade.chat.dto.response.ChatRoomSummaryResponse;
import org.example.gersangtrade.chat.dto.response.RoomStatusSseEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.example.gersangtrade.chat.repository.ChatMessageRepository;
import org.example.gersangtrade.chat.repository.ChatRoomRepository;
import org.example.gersangtrade.domain.chat.ChatMessage;
import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatMessageType;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleEquipmentRitual;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.listing.repository.BundleEquipmentDetailRepository;
import org.example.gersangtrade.listing.repository.BundleEquipmentRitualRepository;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.listing.service.SetTitleGenerator;
import org.example.gersangtrade.home.event.TradeConfirmedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.example.gersangtrade.watchlist.service.WatchKeyBuilder;
import org.example.gersangtrade.notification.service.NotificationService;
import org.example.gersangtrade.report.service.KeywordDetectionService;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.domain.catalog.Server;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 채팅 서비스.
 * 채팅방 생성·메시지 전송·거래완료 확인(2단계) 흐름을 담당한다.
 *
 * 거래완료 순서: 게시자(poster) 확인 → 상대방(counterparty) 확인 → TradeConfirmed 생성.
 * 상세 흐름: docs/trade-flow-design.ko.md 참고.
 */
@Slf4j
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
    private final BundleEquipmentDetailRepository bundleEquipmentDetailRepository;
    private final BundleEquipmentRitualRepository bundleEquipmentRitualRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final WantedItemRepository wantedItemRepository;
    private final TradeConfirmedRepository tradeConfirmedRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final TradeStatService tradeStatService;
    private final ServerRepository serverRepository;
    private final NotificationService notificationService;
    private final KeywordDetectionService keywordDetectionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // ──────────────────────────────────────────────────────────────────────
    // 채팅방 생성
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 채팅방을 생성한다 — 흥정하기(NEGOTIATE) 또는 거래신청(APPLY) 공통 진입점.
     *
     * - 본인 게시물에 채팅 시도 시 예외 발생
     * - 동일 게시물에 이미 OPEN 상태의 채팅방이 있으면 기존 채팅방 반환
     * - 채팅 요청은 알림 센터가 아닌 채팅방 미읽음·목록으로만 표시
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

        // 이미 거래 완료·취소된 게시물에는 채팅 불가
        validateListingOpenForNewChat(request.listingType(), request.listingId());

        // 이미 진행 중(OPEN/AWAITING_PARTNER)인 채팅방이 있으면 기존 채팅방 반환
        boolean exists = chatRoomRepository.existsActiveByListingTypeAndListingIdAndCounterpartyId(
                request.listingType(), request.listingId(), userId);
        if (exists) {
            ChatRoom existing = chatRoomRepository
                    .findByListingTypeAndListingId(request.listingType(), request.listingId())
                    .stream()
                    .filter(r -> r.getCounterparty().getId().equals(userId))
                    .filter(r -> r.getStatus() == ChatRoomStatus.OPEN
                            || r.getStatus() == ChatRoomStatus.AWAITING_PARTNER)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("채팅방 조회 중 오류가 발생했습니다."));
            return toSummary(existing, userId);
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

        // 채팅 요청은 알림 센터가 아닌 채팅방 미읽음·목록으로만 표시한다

        return toSummary(room, userId);
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
                .map(room -> toSummary(room, userId))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 채팅방 상세 + 메시지 조회
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 채팅방 상세 정보와 메시지 목록을 반환한다 (최신 50건).
     * 채팅방 참여자(poster 또는 counterparty)만 조회 가능하다.
     * 조회 시 해당 사용자의 읽음 시각을 갱신한다.
     */
    @Transactional
    public ChatRoomDetailResponse getChatRoomDetail(Long userId, Long chatRoomId) {
        ChatRoom room = loadChatRoom(chatRoomId);
        validateParticipant(room, userId);
        room.markReadBy(userId);

        Slice<ChatMessage> messages = chatMessageRepository.findActiveByRoomId(
                chatRoomId, PageRequest.of(0, 50));

        // 최신 50건을 sentAt 오름차순(오래된 것 위, 최신 아래)으로 반환
        List<ChatMessageResponse> msgResponses = messages.getContent().stream()
                .sorted(java.util.Comparator.comparing(ChatMessage::getSentAt))
                .map(ChatMessageResponse::of)
                .collect(Collectors.toList());

        return ChatRoomDetailResponse.of(
                room, userId, resolveListingDisplayName(room), resolveListingPrice(room), msgResponses);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 메시지 전송
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 메시지를 전송한다.
     *
     * - 채팅방 참여자만 전송 가능
     * - OPEN 또는 AWAITING_PARTNER 상태의 채팅방에서만 전송 가능
     * - 메시지 저장 후 KeywordDetectionService로 현금거래 키워드 감지 (Soft 방식)
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long chatRoomId, ChatMessageSendRequest request) {
        User sender = loadActiveUser(userId);
        ChatRoom room = loadChatRoom(chatRoomId);
        validateParticipant(room, userId);

        // OPEN 또는 AWAITING_PARTNER 상태에서만 메시지 전송 가능
        if (room.getStatus() != ChatRoomStatus.OPEN && room.getStatus() != ChatRoomStatus.AWAITING_PARTNER) {
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

        // 발신자는 채팅 중이므로 읽음 처리
        room.markReadBy(userId);

        // 상대방에게 WebSocket push (알림 센터 CHAT_MESSAGE는 발행하지 않음)
        User recipient = room.getPoster().getId().equals(userId) ? room.getCounterparty() : room.getPoster();
        pushChatMessageToUser(recipient, chatRoomId, msg);

        return ChatMessageResponse.of(msg);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 거래완료 확인 — 양측 누구나 먼저 가능, 양측 모두 확인 시 거래 확정
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 거래완료 확인.
     * 게시자·상대방 모두 호출 가능하며, 한쪽만 확인된 경우 상대방에게 알림을 보낸다.
     * 양측 모두 확인되면 TradeConfirmed 생성 및 EXP·평가 처리를 수행한다.
     */
    @Transactional
    public ChatRoomSummaryResponse confirmTrade(Long userId, Long chatRoomId, PosterConfirmRequest request) {
        // 비관적 잠금: 두 사용자가 동시에 confirmTrade 호출 시 lost-update 방지
        ChatRoom room = chatRoomRepository.findWithLockById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        validateParticipant(room, userId);

        if (room.getStatus() == ChatRoomStatus.COMPLETED || room.getStatus() == ChatRoomStatus.CLOSED) {
            throw new IllegalStateException("종료된 채팅방에서는 거래완료할 수 없습니다.");
        }

        boolean isPoster = room.getPoster().getId().equals(userId);
        // 이번 confirm으로 양측 확인이 채워지면 거래 확정 시도
        boolean willComplete = isPoster
                ? room.getCounterpartyConfirmedAt() != null
                : room.getPosterConfirmedAt() != null;

        // 게시물·다른 채팅방에서 이미 거래 완료된 경우 채팅방을 닫고 안내 (롤백 방지를 위해 예외 대신 반환)
        Optional<String> staleReason = findStaleRoomReason(room, willComplete);
        if (staleReason.isPresent()) {
            closeRoomAsStale(room, staleReason.get());
            return toSummary(room, userId);
        }

        Long finalPrice = request != null ? request.finalPrice() : null;
        Long priceToApply = (room.getFinalPrice() == null && finalPrice != null) ? finalPrice : null;

        room.recordParticipantConfirm(isPoster, priceToApply);
        // 더티 체킹에만 의존하지 않고 명시적으로 플러시하여 DB 반영 보장
        chatRoomRepository.saveAndFlush(room);

        User confirmer = isPoster ? room.getPoster() : room.getCounterparty();
        User other = isPoster ? room.getCounterparty() : room.getPoster();

        if (room.getStatus() == ChatRoomStatus.COMPLETED) {
            // 확정 처리 성공 후에만 COMPLETED 상태를 push (실패 시 롤백·잘못된 push 방지)
            finalizeTrade(room);
            pushRoomStatus(room);
        } else {
            pushRoomStatus(room);
            saveSystemMessage(room,
                    confirmer.getNickname() + "님이 거래완료를 요청했습니다. 거래가 완료되었다면 거래완료 버튼을 눌러주세요.");
            saveNotification(other, NotificationType.POSTER_CONFIRMED, chatRoomId,
                    confirmer.getNickname() + "님이 거래완료를 눌렀습니다. 거래가 완료되었다면 거래완료 버튼을 눌러주세요.");
        }

        return toSummary(room, userId);
    }

    /** 양쪽 참여자에게 채팅방 상태 변경 WebSocket push (viewer 기준 확인 여부 포함) */
    private void pushRoomStatus(ChatRoom room) {
        pushRoomStatusToUser(room, room.getPoster());
        pushRoomStatusToUser(room, room.getCounterparty());
    }

    private void pushRoomStatusToUser(ChatRoom room, User viewer) {
        boolean isPoster = room.getPoster().getId().equals(viewer.getId());
        boolean myTradeConfirmed = isPoster
                ? room.getPosterConfirmedAt() != null
                : room.getCounterpartyConfirmedAt() != null;
        boolean partnerTradeConfirmed = isPoster
                ? room.getCounterpartyConfirmedAt() != null
                : room.getPosterConfirmedAt() != null;
        RoomStatusSseEvent statusEvent = new RoomStatusSseEvent(
                room.getId(),
                room.getStatus().name(),
                myTradeConfirmed,
                partnerTradeConfirmed
        );
        messagingTemplate.convertAndSendToUser(
                viewer.getId().toString(), "/queue/room-status", statusEvent);
    }

    /** @deprecated {@link #confirmTrade(Long, Long, PosterConfirmRequest)} 로 통합 */
    @Deprecated
    @Transactional
    public ChatRoomSummaryResponse posterConfirm(Long userId, Long chatRoomId, PosterConfirmRequest request) {
        return confirmTrade(userId, chatRoomId, request);
    }

    /** @deprecated {@link #confirmTrade(Long, Long, PosterConfirmRequest)} 로 통합 */
    @Deprecated
    @Transactional
    public ChatRoomSummaryResponse counterpartyConfirm(Long userId, Long chatRoomId) {
        return confirmTrade(userId, chatRoomId, new PosterConfirmRequest(null));
    }

    /**
     * 양측 거래완료 확인 후 TradeConfirmed·EXP·평가·알림 처리.
     */
    private void finalizeTrade(ChatRoom room) {
        log.info("finalizeTrade 시작: chatRoomId={}, listingType={}, listingId={}",
                room.getId(), room.getListingType(), room.getListingId());
        try {
            finalizeTradeInternal(room);
        } catch (Exception e) {
            log.error("finalizeTrade 실패 — chatRoomId={}, 원인: {}", room.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private void finalizeTradeInternal(ChatRoom room) {
        Long chatRoomId = room.getId();
        Long posterId = room.getPoster().getId();
        Long counterpartyId = room.getCounterparty().getId();

        // 이미 확정된 채팅방이면 중복 처리 방지 (재시도·동시 요청 대비)
        if (tradeConfirmedRepository.findByChatRoomId(chatRoomId).isPresent()) {
            log.warn("trade_confirmed 이미 존재 — chatRoomId={}", chatRoomId);
            return;
        }

        long confirmedPrice = resolveConfirmedPrice(room);
        log.info("confirmedPrice={}", confirmedPrice);
        String serverSnapshot = resolveServerSnapshot(room);
        String statKeySnapshot = resolveStatKey(room);

        User poster = loadManagedUser(posterId);
        User counterparty = loadManagedUser(counterpartyId);

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
        tradeConfirmedRepository.saveAndFlush(confirmed);

        // 서버명으로 Server 엔티티 조회 후 일별 통계 upsert (서버 미확인 시 통계 생략)
        serverRepository.findByName(serverSnapshot).ifPresentOrElse(
                server -> tradeStatService.upsertDailyStat(statKeySnapshot, confirmedPrice, 1L, LocalDate.now(), server),
                () -> log.warn("서버명으로 Server 조회 실패 — serverSnapshot={}, statKey={}", serverSnapshot, statKeySnapshot)
        );

        // stat upsert(@Modifying) 이후에도 managed User를 사용하기 위해 재조회
        poster = loadManagedUser(posterId);
        counterparty = loadManagedUser(counterpartyId);

        long expDelta = calculateTradeExp(confirmedPrice);
        applyExpAndGrade(poster, expDelta);
        applyExpAndGrade(counterparty, expDelta);

        poster.incrementTradeCount();
        counterparty.incrementTradeCount();

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

        completeListingStatus(room);
        closeOtherRooms(room);

        saveSystemMessage(room, "거래가 확정되었습니다.");

        String tradeCompleteMsg = "거래가 완료됐어요. 상대방을 3일 이내에 평가해보세요.";
        saveNotification(poster, NotificationType.TRADE_COMPLETED, chatRoomId, tradeCompleteMsg);
        saveNotification(counterparty, NotificationType.TRADE_COMPLETED, chatRoomId, tradeCompleteMsg);
        saveNotification(poster, NotificationType.REVIEW_REQUESTED, chatRoomId,
                counterparty.getNickname() + "님과의 거래를 평가해주세요.");
        saveNotification(counterparty, NotificationType.REVIEW_REQUESTED, chatRoomId,
                poster.getNickname() + "님과의 거래를 평가해주세요.");

        // 트랜잭션 커밋 후 캐시 무효화 (커밋 전 evict 시 stale 재캐싱 방지)
        eventPublisher.publishEvent(new TradeConfirmedEvent());
    }

    /** 채팅방 읽음 처리 — 패널 열람 중 실시간 메시지 수신 시 호출 */
    @Transactional
    public void markChatRoomRead(Long userId, Long chatRoomId) {
        ChatRoom room = loadChatRoom(chatRoomId);
        validateParticipant(room, userId);
        room.markReadBy(userId);
    }

    /** 참여 중인 모든 채팅방 읽음 처리 */
    @Transactional
    public void markAllChatRoomsRead(Long userId) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (ChatRoom room : chatRoomRepository.findByPosterId(userId)) {
            if (seen.add(room.getId())) {
                room.markReadBy(userId);
            }
        }
        for (ChatRoom room : chatRoomRepository.findByCounterpartyId(userId)) {
            if (seen.add(room.getId())) {
                room.markReadBy(userId);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // private 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /** 채팅방 목록 DTO 변환 (미읽음 여부 포함) */
    private ChatRoomSummaryResponse toSummary(ChatRoom room, Long viewerId) {
        boolean hasUnread = hasUnreadMessages(room, viewerId);
        return ChatRoomSummaryResponse.of(
                room, viewerId, resolveListingDisplayName(room), resolveListingPrice(room), hasUnread);
    }

    /** 상대방 TEXT 메시지 미읽음 여부 */
    private boolean hasUnreadMessages(ChatRoom room, Long viewerId) {
        return chatMessageRepository.existsUnreadFromOthers(
                room.getId(),
                viewerId,
                room.lastReadAtFor(viewerId),
                ChatMessageType.TEXT);
    }

    /** 활성 상태 사용자 조회 */
    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("차단된 계정은 채팅을 이용할 수 없습니다.");
        }
        return user;
    }

    /** finalizeTrade 등에서 managed User 엔티티를 조회한다 */
    private User loadManagedUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다."));
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

    /** 새 채팅 개설 가능한 게시물 상태인지 검증한다 */
    private void validateListingOpenForNewChat(ListingType listingType, Long listingId) {
        if (listingType == ListingType.SELL) {
            TradeListing listing = tradeListingRepository.findById(listingId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 판매 게시물입니다."));
            if (listing.getStatus() == ListingStatus.SOLD) {
                throw new IllegalStateException("이미 거래가 완료된 판매 게시물입니다.");
            }
            if (listing.getStatus() == ListingStatus.CANCELLED) {
                throw new IllegalStateException("취소된 판매 게시물입니다.");
            }
        } else {
            WantedListing listing = wantedListingRepository.findById(listingId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구매 게시물입니다."));
            if (listing.getStatus() == WantedStatus.PURCHASED) {
                throw new IllegalStateException("이미 거래가 완료된 구매 희망입니다.");
            }
            if (listing.getStatus() == WantedStatus.CANCELLED) {
                throw new IllegalStateException("취소된 구매 희망입니다.");
            }
        }
    }

    /** stale 채팅방 종료 사유 — 게시물 종료 또는 (비완료 요청 시) 다른 COMPLETED 채팅방 존재 */
    private Optional<String> findStaleRoomReason(ChatRoom room, boolean willCompleteOnThisRequest) {
        Optional<String> listingReason = findListingClosedReason(room);
        if (listingReason.isPresent()) {
            return listingReason;
        }

        // 이번 요청으로 정상 완료될 예정이고 게시물이 아직 열려 있으면 진행 (다른 COMPLETED 방 검사 생략)
        if (willCompleteOnThisRequest) {
            return Optional.empty();
        }

        if (chatRoomRepository.countOtherCompletedRoom(
                room.getListingType().name(),
                room.getListingId(),
                room.getCounterparty().getId(),
                room.getId()) > 0) {
            return Optional.of("해당 게시물은 이미 다른 채팅방에서 거래가 완료되었습니다.");
        }
        return Optional.empty();
    }

    /** 거래완료 확인 시 게시물이 이미 종료됐는지 조회한다 */
    private Optional<String> findListingClosedReason(ChatRoom room) {
        if (room.getListingType() == ListingType.SELL) {
            TradeListing listing = tradeListingRepository.findById(room.getListingId())
                    .orElseThrow(() -> new IllegalStateException("게시물을 조회할 수 없습니다."));
            if (listing.getStatus() == ListingStatus.SOLD) {
                return Optional.of("해당 판매 게시물은 이미 다른 거래로 완료되었습니다.");
            }
            if (listing.getStatus() == ListingStatus.CANCELLED) {
                return Optional.of("해당 판매 게시물은 취소되어 더 이상 거래할 수 없습니다.");
            }
            return Optional.empty();
        }

        WantedListing listing = wantedListingRepository.findById(room.getListingId())
                .orElseThrow(() -> new IllegalStateException("게시물을 조회할 수 없습니다."));
        if (listing.getStatus() == WantedStatus.PURCHASED) {
            return Optional.of("해당 구매 희망은 이미 다른 거래로 완료되었습니다.");
        }
        if (listing.getStatus() == WantedStatus.CANCELLED) {
            return Optional.of("해당 구매 희망은 취소되어 더 이상 거래할 수 없습니다.");
        }
        return Optional.empty();
    }

    /** 이미 종료된 게시물에 연결된 채팅방을 닫고 안내 메시지를 남긴다 */
    private void closeRoomAsStale(ChatRoom room, String message) {
        room.close();
        chatRoomRepository.saveAndFlush(room);
        saveSystemMessage(room, message);
        pushRoomStatus(room);
    }

    /** 게시물 등록 가격 조회 (채팅 거래가 입력 기본값) */
    private Long resolveListingPrice(ChatRoom room) {
        if (room.getListingType() == ListingType.SELL) {
            return tradeListingRepository.findById(room.getListingId())
                    .map(TradeListing::getPrice)
                    .orElse(null);
        }
        return wantedListingRepository.findById(room.getListingId())
                .map(WantedListing::getOfferedPrice)
                .orElse(null);
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
     * 채팅 목록에서 보여줄 게시물 표시명을 만든다.
     * 별도 상세 조회 없이 "거래자 / 아이템" 목록을 구성하기 위한 요약값이다.
     */
    private String resolveListingDisplayName(ChatRoom room) {
        try {
            if (room.getListingType() == ListingType.SELL) {
                List<org.example.gersangtrade.domain.listing.ListingBundle> bundles =
                        listingBundleRepository.findByListingIdOrderByIdAsc(room.getListingId());
                if (!bundles.isEmpty()) {
                    org.example.gersangtrade.domain.listing.ListingBundle bundle = bundles.get(0);
                    if (bundle.getTitleOverride() != null && !bundle.getTitleOverride().isBlank()) {
                        return bundle.getTitleOverride();
                    }
                    List<org.example.gersangtrade.domain.listing.BundleLine> lines =
                            bundleLineRepository.findByBundleIdOrderBySortOrderAsc(bundle.getId());
                    if (!lines.isEmpty()) {
                        String itemName = lines.get(0).getItem().getName();
                        return lines.size() > 1 ? itemName + " 외 " + (lines.size() - 1) + "개" : itemName;
                    }
                    return bundle.getBundleType().name();
                }
            } else {
                List<org.example.gersangtrade.domain.wanted.WantedItem> items =
                        wantedItemRepository.findByWantedListingIdOrderBySortOrderAsc(room.getListingId());
                if (!items.isEmpty()) {
                    String itemName = items.get(0).getItem().getName();
                    return items.size() > 1 ? itemName + " 외 " + (items.size() - 1) + "개 구매희망" : itemName + " 구매희망";
                }
            }
        } catch (Exception ignored) {
            // 표시명 조회 실패 시 채팅방 자체는 계속 표시한다.
        }
        return room.getListingType().name() + " #" + room.getListingId();
    }

    /**
     * 거래 통계 집계 키를 결정한다.
     * EQUIPMENT_SET 번들은 SET:{setId}:COMP:...:RC:...:MARK:... 형식,
     * 단품/재료는 ITEM:{itemId} 형식을 반환한다.
     * 아이템을 찾을 수 없는 경우 listingType:listingId 형식으로 fallback한다.
     */
    private String resolveStatKey(ChatRoom room) {
        try {
            if (room.getListingType() == ListingType.SELL) {
                return resolveStatKeySell(room.getListingId());
            } else {
                return resolveStatKeyBuy(room.getListingId());
            }
        } catch (Exception ignored) {
            // 아이템 조회 실패 시 fallback
        }
        return room.getListingType().name() + ":" + room.getListingId();
    }

    private String resolveStatKeySell(Long listingId) {
        var bundles = listingBundleRepository.findByListingIdOrderByIdAsc(listingId);
        if (bundles.isEmpty()) return null;

        var firstBundle = bundles.get(0);

        if (firstBundle.getBundleType() == BundleType.EQUIPMENT_SET) {
            return resolveSetStatKey(firstBundle);
        }

        // 단품·재료
        var lines = bundleLineRepository.findByBundleIdOrderBySortOrderAsc(firstBundle.getId());
        if (lines.isEmpty()) return null;
        var line = lines.get(0);
        Long itemId = line.getItem().getId();
        var details = bundleEquipmentDetailRepository.findByBundleLineIdIn(List.of(line.getId()));
        if (!details.isEmpty() && details.get(0).isHasRitual()) {
            var rituals = bundleEquipmentRitualRepository.findWithRitualByBundleLineIdIn(List.of(line.getId()));
            if (!rituals.isEmpty()) {
                BundleEquipmentRitual r = rituals.get(0);
                String mark = SetTitleGenerator.buildTitleMark(r.getRitual(), r.getOutcome());
                return WatchKeyBuilder.itemKey(itemId, mark);
            }
        }
        return WatchKeyBuilder.itemKey(itemId);
    }

    private String resolveSetStatKey(org.example.gersangtrade.domain.listing.ListingBundle bundle) {
        var lines = bundleLineRepository.findByBundleIdOrderBySortOrderAsc(bundle.getId());
        if (lines.isEmpty()) return null;

        var lineIds = lines.stream().map(l -> l.getId()).toList();
        var detailMap = bundleEquipmentDetailRepository.findWithEquipmentSetByBundleLineIdIn(lineIds)
                .stream().collect(java.util.stream.Collectors.toMap(BundleEquipmentDetail::getBundleLineId, d -> d));
        var ritualMap = bundleEquipmentRitualRepository.findWithRitualByBundleLineIdIn(lineIds)
                .stream().collect(java.util.stream.Collectors.groupingBy(r -> r.getBundleLine().getId()));

        var pieces = lines.stream()
                .sorted(java.util.Comparator.comparingInt(l -> l.getSortOrder()))
                .map(line -> {
                    BundleEquipmentDetail detail = detailMap.get(line.getId());
                    if (detail == null) return null;
                    String mark = null;
                    if (detail.isHasRitual()) {
                        var rituals = ritualMap.getOrDefault(line.getId(), java.util.List.of());
                        if (!rituals.isEmpty()) {
                            BundleEquipmentRitual r = rituals.get(0);
                            mark = SetTitleGenerator.buildTitleMark(r.getRitual(), r.getOutcome());
                        }
                    }
                    return new SetTitleGenerator.PieceTitleInput(detail.getEquipmentItem().getSlot(), mark);
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        SetTitleGenerator.WatchInfo info = SetTitleGenerator.resolveWatchInfo(pieces);
        if (info == null) return null;

        Long setId = bundle.getEquipmentSet() != null
                ? bundle.getEquipmentSet().getId()
                : detailMap.values().stream()
                .map(d -> d.getEquipmentItem().getEquipmentSet())
                .filter(java.util.Objects::nonNull)
                .map(s -> s.getId())
                .findFirst()
                .orElse(null);
        if (setId == null) return null;
        return WatchKeyBuilder.setKey(setId, info.composition(), info.ritualCount(), info.mark());
    }

    // BUY: 첫 번째 WantedItem이 세트 피스이면 SET watchKey (RC:0:MARK:ANY), 아니면 ITEM watchKey
    private String resolveStatKeyBuy(Long wantedListingId) {
        var items = wantedItemRepository.findByWantedListingIdOrderBySortOrderAsc(wantedListingId);
        if (items.isEmpty()) return null;

        // 모든 WantedItem의 EquipmentItem을 조회해 동일 set 여부 확인
        var equipmentItems = items.stream()
                .map(wi -> equipmentItemRepository.findWithItemByItemId(wi.getItem().getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        if (equipmentItems.isEmpty()) {
            return WatchKeyBuilder.itemKey(items.get(0).getItem().getId());
        }

        var setIds = equipmentItems.stream()
                .map(EquipmentItem::getEquipmentSet)
                .filter(java.util.Objects::nonNull)
                .map(s -> s.getId())
                .distinct()
                .toList();

        if (setIds.size() != 1) {
            // 단품 또는 혼합 → 첫 번째 아이템 ITEM 키
            return WatchKeyBuilder.itemKey(items.get(0).getItem().getId());
        }

        Long setId = setIds.get(0);
        var pieces = equipmentItems.stream()
                .filter(e -> e.getEquipmentSet() != null && e.getEquipmentSet().getId().equals(setId))
                .map(e -> new SetTitleGenerator.PieceTitleInput(e.getSlot(), null))
                .toList();

        SetTitleGenerator.WatchInfo info = SetTitleGenerator.resolveWatchInfo(pieces);
        if (info == null) return WatchKeyBuilder.itemKey(items.get(0).getItem().getId());
        // BUY 측은 ritual 정보 없이 RC:0:MARK:NONE 저장
        return WatchKeyBuilder.setKey(setId, info.composition(), 0, null);
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
            tradeListingRepository.findById(room.getListingId()).ifPresent(listing -> {
                listing.completeTrade();
                tradeListingRepository.save(listing);
            });
        } else {
            wantedListingRepository.findById(room.getListingId()).ifPresent(listing -> {
                listing.completePurchase();
                wantedListingRepository.save(listing);
            });
        }
    }

    /** 동일 게시물의 다른 OPEN/AWAITING_PARTNER 채팅방을 모두 종료한다 */
    private void closeOtherRooms(ChatRoom completedRoom) {
        chatRoomRepository
                .findByListingTypeAndListingId(completedRoom.getListingType(), completedRoom.getListingId())
                .stream()
                .filter(r -> !r.getId().equals(completedRoom.getId()))
                .filter(r -> r.getStatus() == ChatRoomStatus.OPEN
                          || r.getStatus() == ChatRoomStatus.AWAITING_PARTNER)
                .forEach(r -> {
                    r.close();
                    saveSystemMessage(r, "해당 게시물의 거래가 다른 채팅방에서 완료되어 채팅방이 종료되었습니다.");
                });
    }

    /** 시스템 메시지를 저장하고 양쪽 참여자에게 WebSocket push */
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
        pushChatMessageToUser(room.getPoster(), room.getId(), sysMsg);
        pushChatMessageToUser(room.getCounterparty(), room.getId(), sysMsg);
    }

    /** 특정 사용자에게 채팅 메시지 WebSocket push */
    private void pushChatMessageToUser(User recipient, Long chatRoomId, ChatMessage msg) {
        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/chat-message",
                new ChatMessageSseEvent(chatRoomId, ChatMessageResponse.of(msg)));
    }

    /** 알림을 저장하고 SSE push한다 */
    private void saveNotification(User user, NotificationType type, Long chatRoomId, String message) {
        notificationService.send(user, type, chatRoomId, message);
    }
}
