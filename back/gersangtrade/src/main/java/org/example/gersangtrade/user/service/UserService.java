package org.example.gersangtrade.user.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.user.dto.request.UserProfileUpdateRequest;
import org.example.gersangtrade.user.dto.request.UserServerUpdateRequest;
import org.example.gersangtrade.user.dto.response.MyGradeResponse;
import org.example.gersangtrade.user.dto.response.PublicUserProfileResponse;
import org.example.gersangtrade.user.dto.response.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 사용자 프로필·등급 관련 서비스.
 * 마이페이지 조회, 내 등록글 목록, 회원 탈퇴를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TradeListingRepository tradeListingRepository;
    private final ListingBundleRepository listingBundleRepository;
    private final ServerRepository serverRepository;

    /**
     * 내 프로필 조회.
     * 소프트 삭제 또는 차단된 계정은 조회되지 않는다.
     *
     * @param userId 조회 대상 사용자 ID
     * @return 사용자 프로필 응답 DTO
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = loadActiveUser(userId);
        return UserProfileResponse.from(user);
    }

    /**
     * 내 활성 등록글 목록 조회.
     * 소프트 삭제되지 않은 등록글을 최신순으로 반환한다.
     *
     * @param userId 판매자 사용자 ID
     * @return 등록글 요약 응답 목록
     */
    @Transactional(readOnly = true)
    public List<ListingSummaryResponse> getMyListings(Long userId) {
        List<TradeListing> listings = tradeListingRepository.findActivesBySellerId(userId);
        return listings.stream()
                .map(listing -> {
                    var bundles = listingBundleRepository.findByListingIdOrderByIdAsc(listing.getId());
                    return ListingSummaryResponse.from(listing, bundles);
                })
                .toList();
    }

    /**
     * 프로필 수정.
     * 각 필드가 null이 아닌 경우에만 해당 필드를 업데이트한다.
     *
     * @param userId  수정할 사용자 ID
     * @param request 수정 요청 DTO (null 필드는 변경하지 않음)
     * @return 변경 반영된 프로필 응답 DTO
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = loadActiveUser(userId);
        if (request.nickname() != null) {
            user.updateNickname(request.nickname());
        }
        if (request.gameNickname() != null) {
            user.updateGameNickname(request.gameNickname());
        }
        if (request.gameAccessTime() != null) {
            user.updateGameAccessTime(request.gameAccessTime());
        }
        return UserProfileResponse.from(user);
    }

    /**
     * 내 등급·경험치 조회.
     *
     * @param userId 사용자 ID
     * @return 등급·EXP·매너점수·거래 횟수 응답 DTO
     */
    @Transactional(readOnly = true)
    public MyGradeResponse getMyGrade(Long userId) {
        User user = loadActiveUser(userId);
        return MyGradeResponse.from(user);
    }

    /**
     * 공개 프로필 조회.
     * 이메일·OAuth 정보를 제외한 공개 필드만 반환한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 공개 프로필 응답 DTO
     */
    @Transactional(readOnly = true)
    public PublicUserProfileResponse getPublicProfile(Long userId) {
        User user = loadActiveUser(userId);
        return PublicUserProfileResponse.from(user);
    }

    /**
     * 기본 서버 변경.
     * serverId가 null이면 선택 해제(전체 서버 조회 모드)로 전환된다.
     *
     * @param userId  변경할 사용자 ID
     * @param request 서버 ID (null 허용)
     * @return 변경 반영된 프로필 응답 DTO
     */
    @Transactional
    public UserProfileResponse updateServer(Long userId, UserServerUpdateRequest request) {
        User user = loadActiveUser(userId);
        Server server = null;
        if (request.serverId() != null) {
            server = serverRepository.findById(request.serverId())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 서버입니다."));
        }
        user.updateServer(server);
        return UserProfileResponse.from(user);
    }

    /**
     * 회원 탈퇴 처리 (소프트 삭제).
     * 이미 탈퇴한 계정에는 예외를 발생시킨다.
     * 실제 하드딜리트는 1년 후 배치 Job이 수행한다.
     *
     * @param userId 탈퇴할 사용자 ID
     */
    @Transactional
    public void withdrawal(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("이미 탈퇴 처리된 계정입니다.");
        }
        user.softDelete();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("탈퇴한 사용자입니다.");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("차단된 사용자입니다.");
        }
        return user;
    }
}
