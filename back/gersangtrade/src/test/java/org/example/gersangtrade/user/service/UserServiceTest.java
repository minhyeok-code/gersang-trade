package org.example.gersangtrade.user.service;

import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.user.dto.response.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * UserService 단위 테스트.
 * 프로필 조회·내 등록글 조회·회원 탈퇴 서비스 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TradeListingRepository tradeListingRepository;
    @Mock private ListingBundleRepository listingBundleRepository;

    @InjectMocks
    private UserService userService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .oauthProvider("google")
                .oauthId("oauth-id-1")
                .nickname("테스트유저")
                .email("test@example.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // getUserProfile
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("활성 사용자_프로필_정상_반환")
        void 활성사용자_프로필_정상반환() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));

            UserProfileResponse response = userService.getUserProfile(1L);

            assertThat(response.nickname()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("존재하지_않는_사용자_NoSuchElementException")
        void 존재하지않는사용자_예외() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("탈퇴한_사용자_IllegalStateException")
        void 탈퇴한사용자_예외() {
            // softDelete() 직접 호출 대신 리플렉션이 필요하므로 스텁으로 검증
            User deletedUser = spy(activeUser);
            given(deletedUser.getDeletedAt()).willReturn(LocalDateTime.now().minusDays(1));
            given(userRepository.findById(2L)).willReturn(Optional.of(deletedUser));

            assertThatThrownBy(() -> userService.getUserProfile(2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("탈퇴한 사용자");
        }

        @Test
        @DisplayName("차단된_사용자_IllegalStateException")
        void 차단된사용자_예외() {
            User blockedUser = User.builder()
                    .oauthProvider("google")
                    .oauthId("oauth-id-2")
                    .nickname("차단유저")
                    .email("blocked@example.com")
                    .role(Role.USER)
                    .status(UserStatus.BLOCKED)
                    .build();
            given(userRepository.findById(3L)).willReturn(Optional.of(blockedUser));

            assertThatThrownBy(() -> userService.getUserProfile(3L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("차단된 사용자");
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // getMyListings
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyListings")
    class GetMyListings {

        @Test
        @DisplayName("등록글_없으면_빈_목록_반환")
        void 등록글없으면_빈목록() {
            given(tradeListingRepository.findActivesBySellerId(1L)).willReturn(List.of());

            var result = userService.getMyListings(1L);

            assertThat(result).isEmpty();
            verify(listingBundleRepository, never()).findByListingIdOrderByIdAsc(any());
        }

        @Test
        @DisplayName("등록글_존재하면_번들_조회후_반환")
        void 등록글있으면_번들조회() {
            User seller = mock(User.class);
            given(seller.getNickname()).willReturn("테스트유저");

            TradeListing listing = mock(TradeListing.class);
            given(listing.getId()).willReturn(10L);
            given(listing.getPrice()).willReturn(5000L);
            given(listing.getServer()).willReturn("한양");
            given(listing.getStatus()).willReturn(
                    org.example.gersangtrade.domain.listing.enums.ListingStatus.ACTIVE);
            given(listing.getSeller()).willReturn(seller);

            given(tradeListingRepository.findActivesBySellerId(1L))
                    .willReturn(List.of(listing));
            given(listingBundleRepository.findByListingIdOrderByIdAsc(10L))
                    .willReturn(List.of());

            var result = userService.getMyListings(1L);

            assertThat(result).hasSize(1);
            verify(listingBundleRepository).findByListingIdOrderByIdAsc(10L);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // withdrawal
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("withdrawal")
    class Withdrawal {

        @Test
        @DisplayName("정상_탈퇴_softDelete_호출")
        void 정상탈퇴_softDelete호출() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));

            userService.withdrawal(1L);

            // deletedAt이 설정되었는지 확인 (softDelete가 호출됨)
            assertThat(activeUser.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지_않는_사용자_NoSuchElementException")
        void 존재하지않는사용자_탈퇴시_예외() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.withdrawal(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("이미_탈퇴한_사용자_IllegalStateException")
        void 이미탈퇴한사용자_예외() {
            // 먼저 탈퇴 처리
            activeUser.softDelete();
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> userService.withdrawal(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 탈퇴 처리된 계정");
        }
    }
}
