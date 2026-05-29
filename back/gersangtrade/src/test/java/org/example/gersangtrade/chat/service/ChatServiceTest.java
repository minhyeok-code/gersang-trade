package org.example.gersangtrade.chat.service;

import org.example.gersangtrade.chat.dto.request.ChatMessageSendRequest;
import org.example.gersangtrade.chat.dto.request.ChatRoomCreateRequest;
import org.example.gersangtrade.chat.dto.request.PosterConfirmRequest;
import org.example.gersangtrade.chat.dto.response.ChatMessageResponse;
import org.example.gersangtrade.chat.dto.response.ChatRoomSummaryResponse;
import org.example.gersangtrade.chat.repository.ChatMessageRepository;
import org.example.gersangtrade.chat.repository.ChatRoomRepository;
import org.example.gersangtrade.domain.chat.ChatMessage;
import org.example.gersangtrade.domain.chat.ChatRoom;
import org.example.gersangtrade.domain.chat.enums.ChatRoomStatus;
import org.example.gersangtrade.domain.chat.enums.InitiationType;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.example.gersangtrade.domain.notification.enums.NotificationType;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.notification.service.NotificationService;
import org.example.gersangtrade.report.service.KeywordDetectionService;
import org.example.gersangtrade.trade.repository.TradeConfirmedRepository;
import org.example.gersangtrade.trade.repository.TradeReviewRepository;
import org.example.gersangtrade.trade.service.TradeStatService;
import org.example.gersangtrade.wanted.repository.WantedItemRepository;
import org.example.gersangtrade.wanted.repository.WantedListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatService 단위 테스트.
 * Mockito로 레포지토리를 Mock 처리하여 서비스 로직만 검증한다.
 * BeforeEach에서 공통 Mock stub을 설정하므로 LENIENT 모드를 사용한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private TradeListingRepository tradeListingRepository;
    @Mock private WantedListingRepository wantedListingRepository;
    @Mock private ListingBundleRepository listingBundleRepository;
    @Mock private BundleLineRepository bundleLineRepository;
    @Mock private WantedItemRepository wantedItemRepository;
    @Mock private TradeConfirmedRepository tradeConfirmedRepository;
    @Mock private TradeReviewRepository tradeReviewRepository;
    @Mock private TradeStatService tradeStatService;
    @Mock private NotificationService notificationService;
    @Mock private KeywordDetectionService keywordDetectionService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    // ── 공통 픽스처 ──────────────────────────────────────────────────────────

    /** 게시자 (판매자 측) */
    private User poster;
    /** 상대방 (구매자 측) */
    private User counterparty;
    /** 차단된 사용자 */
    private User blockedUser;
    /** 판매 게시물 Mock */
    private TradeListing tradeListing;

    @BeforeEach
    void setUp() {
        poster = User.builder()
                .oauthProvider("google").oauthId("google-poster")
                .nickname("판매자").email("poster@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build();

        counterparty = User.builder()
                .oauthProvider("google").oauthId("google-counter")
                .nickname("구매자").email("counter@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build();

        blockedUser = User.builder()
                .oauthProvider("google").oauthId("google-blocked")
                .nickname("차단유저").email("blocked@test.com")
                .role(Role.USER).status(UserStatus.BLOCKED)
                .build();

        // poster ID = 1, counterparty ID = 2 로 spy를 통해 ID 주입
        poster = spy(poster);
        doReturn(1L).when(poster).getId();

        counterparty = spy(counterparty);
        doReturn(2L).when(counterparty).getId();

        blockedUser = spy(blockedUser);
        doReturn(3L).when(blockedUser).getId();

        tradeListing = mock(TradeListing.class);
        when(tradeListing.getSeller()).thenReturn(poster);
        when(tradeListing.getPrice()).thenReturn(50_000_000L);
        when(tradeListing.getServer()).thenReturn("서버1");
        when(tradeListing.getStatus()).thenReturn(ListingStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(poster));
        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
    }

    // ──────────────────────────────────────────────────────────────────────
    // createChatRoom 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createChatRoom_정상_판매게시물_채팅방생성및알림저장")
    void createChatRoom_정상_판매게시물_채팅방생성및알림저장() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ListingType.SELL, 1L, InitiationType.NEGOTIATE);

        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(chatRoomRepository.existsActiveByListingTypeAndListingIdAndCounterpartyId(
                any(), any(), any())).thenReturn(false);

        ChatRoom savedRoom = mock(ChatRoom.class);
        when(savedRoom.getId()).thenReturn(100L);
        when(savedRoom.getListingType()).thenReturn(ListingType.SELL);
        when(savedRoom.getListingId()).thenReturn(1L);
        when(savedRoom.getInitiationType()).thenReturn(InitiationType.NEGOTIATE);
        when(savedRoom.getStatus()).thenReturn(ChatRoomStatus.OPEN);
        when(savedRoom.getFinalPrice()).thenReturn(null);
        when(savedRoom.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(savedRoom.getPoster()).thenReturn(poster);
        when(savedRoom.getCounterparty()).thenReturn(counterparty);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);

        ChatRoomSummaryResponse response = chatService.createChatRoom(2L, request);

        // 채팅방 저장, 시스템 메시지 저장, 알림 저장 각 1회 확인
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ChatRoomStatus.OPEN);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatMessageRepository).save(any(ChatMessage.class)); // 시스템 메시지
        verify(notificationService).send(any(), any(), any(), any()); // 게시자 알림
    }

    @Test
    @DisplayName("createChatRoom_이미OPEN채팅방존재_기존채팅방반환")
    void createChatRoom_이미OPEN채팅방존재_기존채팅방반환() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ListingType.SELL, 1L, InitiationType.NEGOTIATE);

        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(chatRoomRepository.existsActiveByListingTypeAndListingIdAndCounterpartyId(
                any(), any(), any())).thenReturn(true);

        // 기존 채팅방 조회 시 반환할 Mock
        ChatRoom existingRoom = mock(ChatRoom.class);
        when(existingRoom.getId()).thenReturn(99L);
        when(existingRoom.getListingType()).thenReturn(ListingType.SELL);
        when(existingRoom.getListingId()).thenReturn(1L);
        when(existingRoom.getInitiationType()).thenReturn(InitiationType.NEGOTIATE);
        when(existingRoom.getStatus()).thenReturn(ChatRoomStatus.OPEN);
        when(existingRoom.getFinalPrice()).thenReturn(null);
        when(existingRoom.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(existingRoom.getPoster()).thenReturn(poster);
        when(existingRoom.getCounterparty()).thenReturn(counterparty);
        when(chatRoomRepository.findByListingTypeAndListingId(ListingType.SELL, 1L))
                .thenReturn(List.of(existingRoom));

        ChatRoomSummaryResponse response = chatService.createChatRoom(2L, request);

        // 새 채팅방 저장 없이 기존 채팅방 반환
        assertThat(response.id()).isEqualTo(99L);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("createChatRoom_본인게시물_예외발생")
    void createChatRoom_본인게시물_예외발생() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ListingType.SELL, 1L, InitiationType.NEGOTIATE);

        // poster(id=1)가 본인 게시물에 채팅 시도
        when(userRepository.findById(1L)).thenReturn(Optional.of(poster));
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));

        assertThatThrownBy(() -> chatService.createChatRoom(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인 게시물에는 채팅을 개설할 수 없습니다");
    }

    @Test
    @DisplayName("createChatRoom_이미완료된판매게시물_예외발생")
    void createChatRoom_이미완료된판매게시물_예외발생() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ListingType.SELL, 1L, InitiationType.NEGOTIATE);

        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(tradeListing.getStatus()).thenReturn(ListingStatus.SOLD);

        assertThatThrownBy(() -> chatService.createChatRoom(2L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 거래가 완료된 판매 게시물");
    }

    @Test
    @DisplayName("createChatRoom_차단된사용자_예외발생")
    void createChatRoom_차단된사용자_예외발생() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                ListingType.SELL, 1L, InitiationType.NEGOTIATE);

        when(userRepository.findById(3L)).thenReturn(Optional.of(blockedUser));

        assertThatThrownBy(() -> chatService.createChatRoom(3L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차단된 계정은 채팅을 이용할 수 없습니다");
    }

    // ──────────────────────────────────────────────────────────────────────
    // sendMessage 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendMessage_정상_OPEN상태_메시지저장및WebSocketPush")
    void sendMessage_정상_OPEN상태_메시지저장및WebSocketPush() {
        ChatRoom room = mockOpenRoom();
        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        ChatMessage savedMsg = mock(ChatMessage.class);
        when(savedMsg.getId()).thenReturn(1L);
        when(savedMsg.getSender()).thenReturn(counterparty);
        when(savedMsg.getContent()).thenReturn("안녕하세요");
        when(savedMsg.getSentAt()).thenReturn(LocalDateTime.now());
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMsg);

        ChatMessageResponse response = chatService.sendMessage(
                2L, 100L, new ChatMessageSendRequest("안녕하세요"));

        assertThat(response).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSendToUser(anyString(), eq("/queue/chat-message"), any());
        verify(notificationService, never()).send(any(), eq(NotificationType.CHAT_MESSAGE), any(), any());
    }

    @Test
    @DisplayName("sendMessage_AWAITING_PARTNER상태_메시지전송가능")
    void sendMessage_AWAITING_PARTNER상태_메시지전송가능() {
        // AWAITING_PARTNER 상태에서도 메시지 전송 허용
        ChatRoom room = mock(ChatRoom.class);
        when(room.getStatus()).thenReturn(ChatRoomStatus.AWAITING_PARTNER);
        when(room.getPoster()).thenReturn(poster);
        when(room.getCounterparty()).thenReturn(counterparty);

        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        ChatMessage savedMsg = mock(ChatMessage.class);
        when(savedMsg.getSender()).thenReturn(counterparty);
        when(savedMsg.getContent()).thenReturn("확인했습니다");
        when(savedMsg.getSentAt()).thenReturn(LocalDateTime.now());
        when(chatMessageRepository.save(any())).thenReturn(savedMsg);

        ChatMessageResponse response = chatService.sendMessage(
                2L, 100L, new ChatMessageSendRequest("확인했습니다"));

        assertThat(response).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage_COMPLETED상태_메시지전송불가_예외발생")
    void sendMessage_COMPLETED상태_메시지전송불가_예외발생() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getStatus()).thenReturn(ChatRoomStatus.COMPLETED);
        when(room.getPoster()).thenReturn(poster);
        when(room.getCounterparty()).thenReturn(counterparty);

        when(userRepository.findById(2L)).thenReturn(Optional.of(counterparty));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.sendMessage(
                2L, 100L, new ChatMessageSendRequest("메시지")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("종료된 채팅방에는 메시지를 전송할 수 없습니다");
    }

    @Test
    @DisplayName("sendMessage_참여자아닌사용자_예외발생")
    void sendMessage_참여자아닌사용자_예외발생() {
        // 제3자(id=99)가 채팅방에 메시지 전송 시도
        User stranger = spy(User.builder()
                .oauthProvider("google").oauthId("stranger")
                .nickname("모르는사람").email("stranger@test.com")
                .role(Role.USER).status(UserStatus.ACTIVE).build());
        doReturn(99L).when(stranger).getId();

        ChatRoom room = mockOpenRoom();
        when(userRepository.findById(99L)).thenReturn(Optional.of(stranger));
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.sendMessage(
                99L, 100L, new ChatMessageSendRequest("메시지")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("채팅방의 참여자가 아닙니다");
    }

    // ──────────────────────────────────────────────────────────────────────
    // confirmTrade 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmTrade_게시자_먼저확인_AWAITING_PARTNER_상대알림")
    void confirmTrade_게시자_먼저확인_AWAITING_PARTNER_상대알림() {
        ChatRoom room = realOpenRoom();
        stubLockedRoom(room);

        ChatRoomSummaryResponse response = chatService.confirmTrade(
                1L, 100L, new PosterConfirmRequest(45_000_000L));

        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.AWAITING_PARTNER);
        assertThat(room.getPosterConfirmedAt()).isNotNull();
        assertThat(room.getCounterpartyConfirmedAt()).isNull();
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(notificationService).send(eq(counterparty), any(), any(), any());
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("confirmTrade_상대방_먼저확인_AWAITING_PARTNER_게시자알림")
    void confirmTrade_상대방_먼저확인_AWAITING_PARTNER_게시자알림() {
        ChatRoom room = realOpenRoom();
        stubLockedRoom(room);

        chatService.confirmTrade(2L, 100L, new PosterConfirmRequest(null));

        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.AWAITING_PARTNER);
        assertThat(room.getCounterpartyConfirmedAt()).isNotNull();
        assertThat(room.getPosterConfirmedAt()).isNull();
        verify(notificationService).send(eq(poster), any(), any(), any());
    }

    @Test
    @DisplayName("confirmTrade_이미확인한사용자_재호출_예외발생")
    void confirmTrade_이미확인한사용자_재호출_예외발생() {
        ChatRoom room = realOpenRoom();
        room.recordParticipantConfirm(true, 45_000_000L);
        stubLockedRoom(room);

        assertThatThrownBy(() -> chatService.confirmTrade(
                1L, 100L, new PosterConfirmRequest(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 거래완료를 확인했습니다");
    }

    @Test
    @DisplayName("confirmTrade_양측확인_COMPLETED_TradeConfirmed생성")
    void confirmTrade_양측확인_COMPLETED_TradeConfirmed생성() {
        ChatRoom room = realOpenRoom();
        room.recordParticipantConfirm(true, 50_000_000L);
        stubLockedRoom(room);
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(chatRoomRepository.findByListingTypeAndListingId(ListingType.SELL, 1L))
                .thenReturn(List.of(room));
        when(tradeConfirmedRepository.saveAndFlush(any(TradeConfirmed.class))).thenReturn(mock(TradeConfirmed.class));
        when(tradeReviewRepository.save(any(TradeReview.class))).thenReturn(mock(TradeReview.class));

        ChatRoomSummaryResponse response = chatService.confirmTrade(2L, 100L, new PosterConfirmRequest(null));

        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.COMPLETED);
        verify(tradeConfirmedRepository).saveAndFlush(any(TradeConfirmed.class));
        verify(tradeReviewRepository, times(2)).save(any(TradeReview.class));
        verify(notificationService, times(4)).send(any(), any(), any(), any());
        verify(poster).incrementTradeCount();
        verify(counterparty).incrementTradeCount();
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("confirmTrade_동일조합다른채팅방COMPLETED_채팅방CLOSED_거래확정없음")
    void confirmTrade_동일조합다른채팅방COMPLETED_채팅방CLOSED_거래확정없음() {
        ChatRoom room = realOpenRoom();
        room.recordParticipantConfirm(false, null);
        stubLockedRoom(room);

        when(tradeListing.getStatus()).thenReturn(ListingStatus.SOLD);
        when(chatRoomRepository.countOtherCompletedRoom(
                eq("SELL"), eq(1L), eq(2L), eq(100L))).thenReturn(0L);

        ChatRoomSummaryResponse response = chatService.confirmTrade(1L, 100L, new PosterConfirmRequest(null));

        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
        assertThat(response.status()).isEqualTo(ChatRoomStatus.CLOSED);
        verify(tradeConfirmedRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("confirmTrade_이미완료된구매희망_채팅방CLOSED_거래확정없음")
    void confirmTrade_이미완료된구매희망_채팅방CLOSED_거래확정없음() {
        ChatRoom room = ChatRoom.builder()
                .listingType(ListingType.BUY)
                .listingId(5L)
                .initiationType(InitiationType.APPLY)
                .poster(poster)
                .counterparty(counterparty)
                .build();
        room = spy(room);
        doReturn(100L).when(room).getId();
        doReturn(LocalDateTime.now()).when(room).getCreatedAt();
        room.recordParticipantConfirm(false, null);
        stubLockedRoom(room);

        WantedListing wanted = mock(WantedListing.class);
        when(wanted.getStatus()).thenReturn(WantedStatus.PURCHASED);
        when(wantedListingRepository.findById(5L)).thenReturn(Optional.of(wanted));

        ChatRoomSummaryResponse response = chatService.confirmTrade(1L, 100L, new PosterConfirmRequest(null));

        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
        assertThat(response.status()).isEqualTo(ChatRoomStatus.CLOSED);
        verify(tradeConfirmedRepository, never()).saveAndFlush(any());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("confirmTrade_동일게시물의다른OPEN채팅방_자동CLOSED처리")
    void confirmTrade_동일게시물의다른OPEN채팅방_자동CLOSED처리() {
        ChatRoom room = realOpenRoom();
        room.recordParticipantConfirm(true, 50_000_000L);
        stubLockedRoom(room);
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(tradeConfirmedRepository.saveAndFlush(any())).thenReturn(mock(TradeConfirmed.class));
        when(tradeReviewRepository.save(any())).thenReturn(mock(TradeReview.class));

        ChatRoom otherRoom = mock(ChatRoom.class);
        when(otherRoom.getId()).thenReturn(200L);
        when(otherRoom.getStatus()).thenReturn(ChatRoomStatus.OPEN);
        when(otherRoom.getPoster()).thenReturn(poster);
        when(otherRoom.getCounterparty()).thenReturn(counterparty);
        when(chatRoomRepository.findByListingTypeAndListingId(ListingType.SELL, 1L))
                .thenReturn(List.of(room, otherRoom));

        chatService.confirmTrade(2L, 100L, new PosterConfirmRequest(null));

        verify(otherRoom).close();
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // private 헬퍼 — 채팅방 Mock 빌더
    // ──────────────────────────────────────────────────────────────────────

    /** confirmTrade용 채팅방 잠금 조회 stub */
    private void stubLockedRoom(ChatRoom room) {
        when(chatRoomRepository.findWithLockById(100L)).thenReturn(Optional.of(room));
        when(chatRoomRepository.saveAndFlush(room)).thenReturn(room);
        when(tradeConfirmedRepository.findByChatRoomId(100L)).thenReturn(Optional.empty());
        ListingType listingType = room.getListingType();
        Long listingId = room.getListingId();
        Long counterpartyId = room.getCounterparty().getId();
        Long roomId = room.getId();
        if (listingType == ListingType.SELL) {
            when(tradeListingRepository.findById(listingId)).thenReturn(Optional.of(tradeListing));
        }
        when(chatRoomRepository.countOtherCompletedRoom(
                eq(listingType.name()), eq(listingId), eq(counterpartyId), eq(roomId))).thenReturn(0L);
    }

    /** OPEN 상태 실제 채팅방 (id=100) */
    private ChatRoom realOpenRoom() {
        ChatRoom room = ChatRoom.builder()
                .listingType(ListingType.SELL)
                .listingId(1L)
                .initiationType(InitiationType.NEGOTIATE)
                .poster(poster)
                .counterparty(counterparty)
                .build();
        room = spy(room);
        doReturn(100L).when(room).getId();
        doReturn(LocalDateTime.now()).when(room).getCreatedAt();
        return room;
    }

    /** OPEN 상태 채팅방 Mock */
    private ChatRoom mockOpenRoom() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getListingType()).thenReturn(ListingType.SELL);
        when(room.getListingId()).thenReturn(1L);
        when(room.getInitiationType()).thenReturn(InitiationType.NEGOTIATE);
        when(room.getStatus()).thenReturn(ChatRoomStatus.OPEN);
        when(room.getFinalPrice()).thenReturn(null);
        when(room.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(room.getPoster()).thenReturn(poster);
        when(room.getCounterparty()).thenReturn(counterparty);
        return room;
    }

    /** AWAITING_PARTNER 상태 채팅방 Mock (finalPrice 있음) */
    private ChatRoom mockPosterConfirmedRoom() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getListingType()).thenReturn(ListingType.SELL);
        when(room.getListingId()).thenReturn(1L);
        when(room.getInitiationType()).thenReturn(InitiationType.NEGOTIATE);
        when(room.getStatus()).thenReturn(ChatRoomStatus.AWAITING_PARTNER);
        when(room.getFinalPrice()).thenReturn(50_000_000L); // finalPrice 있으므로 DB 가격 조회 불필요
        when(room.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(room.getPoster()).thenReturn(poster);
        when(room.getCounterparty()).thenReturn(counterparty);
        return room;
    }
}
