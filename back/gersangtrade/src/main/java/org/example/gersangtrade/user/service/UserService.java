package org.example.gersangtrade.user.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
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
