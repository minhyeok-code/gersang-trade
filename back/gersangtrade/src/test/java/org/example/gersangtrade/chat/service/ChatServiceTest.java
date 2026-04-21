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
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.trade.TradeReview;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.notification.service.NotificationService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        when(chatRoomRepository.existsByListingTypeAndListingIdAndCounterpartyIdAndStatus(
                any(), any(), any(), any())).thenReturn(false);

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
        when(chatRoomRepository.existsByListingTypeAndListingIdAndCounterpartyIdAndStatus(
                any(), any(), any(), any())).thenReturn(true);

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
    @DisplayName("sendMessage_정상_OPEN상태_메시지저장및알림")
    void sendMessage_정상_OPEN상태_메시지저장및알림() {
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
        verify(notificationService).send(any(), any(), any(), any()); // 상대방 알림
    }

    @Test
    @DisplayName("sendMessage_POSTER_CONFIRMED상태_메시지전송가능")
    void sendMessage_POSTER_CONFIRMED상태_메시지전송가능() {
        // POSTER_CONFIRMED 상태에서도 메시지 전송 허용
        ChatRoom room = mock(ChatRoom.class);
        when(room.getStatus()).thenReturn(ChatRoomStatus.POSTER_CONFIRMED);
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
    // posterConfirm 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("posterConfirm_정상_OPEN에서POSTER_CONFIRMED전환")
    void posterConfirm_정상_OPEN에서POSTER_CONFIRMED전환() {
        ChatRoom room = mockOpenRoom();
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        ChatRoomSummaryResponse response = chatService.posterConfirm(
                1L, 100L, new PosterConfirmRequest(45_000_000L));

        // posterConfirm() 상태전이 메서드 호출 확인
        verify(room).posterConfirm(45_000_000L);
        verify(chatMessageRepository).save(any(ChatMessage.class)); // 시스템 메시지
        verify(notificationService).send(any(), any(), any(), any()); // 상대방 알림
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("posterConfirm_게시자아닌사용자_예외발생")
    void posterConfirm_게시자아닌사용자_예외발생() {
        ChatRoom room = mockOpenRoom();
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        // counterparty(id=2)가 posterConfirm 호출 시도
        assertThatThrownBy(() -> chatService.posterConfirm(
                2L, 100L, new PosterConfirmRequest(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("게시자만 거래완료를 먼저 요청할 수 있습니다");
    }

    @Test
    @DisplayName("posterConfirm_POSTER_CONFIRMED상태재호출_예외발생")
    void posterConfirm_POSTER_CONFIRMED상태재호출_예외발생() {
        // 이미 POSTER_CONFIRMED 상태인 채팅방에 재호출
        ChatRoom room = mock(ChatRoom.class);
        when(room.getPoster()).thenReturn(poster);
        when(room.getCounterparty()).thenReturn(counterparty);
        when(room.getStatus()).thenReturn(ChatRoomStatus.POSTER_CONFIRMED);
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.posterConfirm(
                1L, 100L, new PosterConfirmRequest(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("현재 상태에서는 거래완료를 요청할 수 없습니다");
    }

    // ──────────────────────────────────────────────────────────────────────
    // counterpartyConfirm 테스트
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("counterpartyConfirm_정상_COMPLETED전환_TradeConfirmed생성_평가2건_알림4건")
    void counterpartyConfirm_정상_COMPLETED전환_TradeConfirmed생성_평가2건_알림4건() {
        ChatRoom room = mockPosterConfirmedRoom();
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(chatRoomRepository.findByListingTypeAndListingId(ListingType.SELL, 1L))
                .thenReturn(List.of(room)); // 다른 채팅방 없음

        TradeConfirmed savedConfirmed = mock(TradeConfirmed.class);
        when(tradeConfirmedRepository.save(any(TradeConfirmed.class))).thenReturn(savedConfirmed);
        when(tradeReviewRepository.save(any(TradeReview.class))).thenReturn(mock(TradeReview.class));

        ChatRoomSummaryResponse response = chatService.counterpartyConfirm(2L, 100L);

        assertThat(response).isNotNull();
        // 상태 전이
        verify(room).counterpartyConfirm();
        // TradeConfirmed 생성
        verify(tradeConfirmedRepository).save(any(TradeConfirmed.class));
        // 거래 평가 2건 생성 (poster→counterparty, counterparty→poster)
        verify(tradeReviewRepository, times(2)).save(any(TradeReview.class));
        // 알림 4건: poster TRADE_COMPLETED, counterparty TRADE_COMPLETED, poster REVIEW_REQUESTED, counterparty REVIEW_REQUESTED
        verify(notificationService, times(4)).send(any(), any(), any(), any());
        // 거래 횟수 증가
        verify(poster).incrementTradeCount();
        verify(counterparty).incrementTradeCount();
    }

    @Test
    @DisplayName("counterpartyConfirm_상대방아닌사용자_예외발생")
    void counterpartyConfirm_상대방아닌사용자_예외발생() {
        ChatRoom room = mockPosterConfirmedRoom();
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        // poster(id=1)가 counterpartyConfirm 호출 시도
        assertThatThrownBy(() -> chatService.counterpartyConfirm(1L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상대방만 거래완료를 최종 확인할 수 있습니다");
    }

    @Test
    @DisplayName("counterpartyConfirm_OPEN상태에서호출_예외발생")
    void counterpartyConfirm_OPEN상태에서호출_예외발생() {
        // 게시자 확인 없이 상대방이 바로 counterpartyConfirm 호출
        ChatRoom room = mockOpenRoom();
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.counterpartyConfirm(2L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("게시자가 먼저 거래완료를 요청해야 합니다");
    }

    @Test
    @DisplayName("counterpartyConfirm_동일게시물의다른OPEN채팅방_자동CLOSED처리")
    void counterpartyConfirm_동일게시물의다른OPEN채팅방_자동CLOSED처리() {
        ChatRoom room = mockPosterConfirmedRoom();
        when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(tradeListingRepository.findById(1L)).thenReturn(Optional.of(tradeListing));
        when(tradeConfirmedRepository.save(any())).thenReturn(mock(TradeConfirmed.class));
        when(tradeReviewRepository.save(any())).thenReturn(mock(TradeReview.class));

        // 동일 게시물에 다른 OPEN 채팅방이 존재
        ChatRoom otherRoom = mock(ChatRoom.class);
        when(otherRoom.getId()).thenReturn(200L);
        when(otherRoom.getStatus()).thenReturn(ChatRoomStatus.OPEN);

        when(chatRoomRepository.findByListingTypeAndListingId(ListingType.SELL, 1L))
                .thenReturn(List.of(room, otherRoom)); // 완료된 방 + 다른 OPEN 방

        chatService.counterpartyConfirm(2L, 100L);

        // 다른 OPEN 채팅방이 close() 처리되어야 함
        verify(otherRoom).close();
        // 종료 시스템 메시지 저장 확인 (완료방 시스템메시지 없음 + 타방 1건)
        verify(chatMessageRepository, atLeastOnce()).save(any(ChatMessage.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // private 헬퍼 — 채팅방 Mock 빌더
    // ──────────────────────────────────────────────────────────────────────

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

    /** POSTER_CONFIRMED 상태 채팅방 Mock (finalPrice 있음) */
    private ChatRoom mockPosterConfirmedRoom() {
        ChatRoom room = mock(ChatRoom.class);
        when(room.getId()).thenReturn(100L);
        when(room.getListingType()).thenReturn(ListingType.SELL);
        when(room.getListingId()).thenReturn(1L);
        when(room.getInitiationType()).thenReturn(InitiationType.NEGOTIATE);
        when(room.getStatus()).thenReturn(ChatRoomStatus.POSTER_CONFIRMED);
        when(room.getFinalPrice()).thenReturn(50_000_000L); // finalPrice 있으므로 DB 가격 조회 불필요
        when(room.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(room.getPoster()).thenReturn(poster);
        when(room.getCounterparty()).thenReturn(counterparty);
        return room;
    }
}
