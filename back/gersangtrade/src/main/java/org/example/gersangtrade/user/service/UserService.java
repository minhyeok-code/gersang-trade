package org.example.gersangtrade.user.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.Server;
import org.example.gersangtrade.domain.chat.enums.ListingType;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.trade.TradeConfirmed;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserClearTime;
import org.example.gersangtrade.domain.user.UserClearTimeRepository;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.listing.repository.BundleLineRepository;
import org.example.gersangtrade.listing.repository.ListingBundleRepository;
import org.example.gersangtrade.listing.repository.TradeListingRepository;
import org.example.gersangtrade.trade.dto.response.TradeHistoryResponse;
import org.example.gersangtrade.trade.repository.TradeConfirmedRepository;
import org.example.gersangtrade.user.dto.request.ClearTimeRequest;
import org.example.gersangtrade.user.dto.request.UserProfileUpdateRequest;
import org.example.gersangtrade.user.dto.request.UserServerUpdateRequest;
import org.example.gersangtrade.user.dto.response.ClearTimeResponse;
import org.example.gersangtrade.user.dto.response.MyGradeResponse;
import org.example.gersangtrade.user.dto.response.PublicUserProfileResponse;
import org.example.gersangtrade.user.dto.response.UserProfileResponse;
import org.example.gersangtrade.user.util.ExpGradeCalculator;
import org.example.gersangtrade.wanted.repository.WantedItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 사용자 프로필·등급 관련 서비스.
 * 마이페이지 조회, 내 등록글 목록, 회원 탈퇴를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final long CLEAR_TIME_EXP = 5L;

    private final UserRepository userRepository;
    private final TradeListingRepository tradeListingRepository;
    private final ListingBundleRepository listingBundleRepository;
    private final BundleLineRepository bundleLineRepository;
    private final WantedItemRepository wantedItemRepository;
    private final TradeConfirmedRepository tradeConfirmedRepository;
    private final ServerRepository serverRepository;
    private final MonsterRepository monsterRepository;
    private final UserClearTimeRepository clearTimeRepository;

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
        if (listings.isEmpty()) return List.of();

        List<Long> listingIds = listings.stream().map(TradeListing::getId).toList();

        List<ListingBundle> allBundles = listingBundleRepository.findByListingIdIn(listingIds);
        Map<Long, List<ListingBundle>> bundlesByListingId = allBundles.stream()
                .collect(Collectors.groupingBy(b -> b.getListing().getId()));

        List<Long> bundleIds = allBundles.stream().map(ListingBundle::getId).toList();
        Map<Long, List<BundleLine>> linesByBundleId = bundleLineRepository.findByBundleIdIn(bundleIds)
                .stream()
                .collect(Collectors.groupingBy(l -> l.getBundle().getId()));

        return listings.stream()
                .map(listing -> ListingSummaryResponse.from(
                        listing,
                        bundlesByListingId.getOrDefault(listing.getId(), List.of()),
                        linesByBundleId))
                .toList();
    }

    /**
     * 내 거래 확정 내역 조회 (판매자·구매자 양쪽 포함).
     * TradeConfirmed 기준으로 조회하므로 상대방(구매자)도 자신의 거래내역을 볼 수 있다.
     *
     * @param userId 조회 사용자 ID
     * @return 거래 확정 내역 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<TradeHistoryResponse> getMyTradeHistory(Long userId) {
        List<TradeConfirmed> trades = tradeConfirmedRepository.findByUserId(userId);
        return trades.stream()
                .map(tc -> {
                    String role = tc.getSeller() != null && tc.getSeller().getId().equals(userId)
                            ? "판매" : "구매";
                    String displayName = resolveTradeDisplayName(tc);
                    return new TradeHistoryResponse(
                            tc.getId(),
                            role,
                            tc.getListingType(),
                            displayName,
                            tc.getConfirmedPrice(),
                            tc.getServerSnapshot(),
                            tc.getConfirmedAt()
                    );
                })
                .toList();
    }

    /**
     * 거래 확정 레코드에서 물품 표시명을 조회한다.
     * chatRoom → listing → bundle → line 순서로 탐색하며, 실패 시 listingType으로 폴백한다.
     */
    private String resolveTradeDisplayName(TradeConfirmed tc) {
        if (tc.getChatRoom() == null) {
            return tc.getListingType().name();
        }
        Long listingId = tc.getChatRoom().getListingId();
        try {
            if (tc.getListingType() == ListingType.SELL) {
                List<ListingBundle> bundles = listingBundleRepository.findByListingIdOrderByIdAsc(listingId);
                if (!bundles.isEmpty()) {
                    ListingBundle first = bundles.get(0);
                    if (first.getTitleOverride() != null && !first.getTitleOverride().isBlank()) {
                        return first.getTitleOverride();
                    }
                    List<BundleLine> lines = bundleLineRepository.findByBundleIdOrderBySortOrderAsc(first.getId());
                    if (!lines.isEmpty()) {
                        String name = lines.get(0).getItem().getName();
                        return lines.size() > 1 ? name + " 외 " + (lines.size() - 1) + "개" : name;
                    }
                }
            } else {
                List<WantedItem> items = wantedItemRepository.findByWantedListingIdOrderBySortOrderAsc(listingId);
                if (!items.isEmpty()) {
                    String name = items.get(0).getItem().getName();
                    return items.size() > 1 ? name + " 외 " + (items.size() - 1) + "개 구매희망" : name + " 구매희망";
                }
            }
        } catch (Exception ignored) {
            // 게시물 삭제 등으로 조회 실패 시 폴백
        }
        return tc.getListingType().name() + " #" + listingId;
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
        if (request.profileImageUrl() != null) {
            // 빈 문자열이면 null로 저장 (프로필 사진 삭제)
            user.updateProfileImageUrl(request.profileImageUrl().isBlank() ? null : request.profileImageUrl());
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

    /**
     * 클리어타임 저장 및 EXP 지급.
     * 데이터 기여 보상으로 {@value CLEAR_TIME_EXP} EXP를 지급한다.
     *
     * @param userId  유저 ID
     * @param request 클리어타임 저장 요청
     * @return 저장된 클리어타임 + 지급된 EXP
     */
    @Transactional
    public ClearTimeResponse saveClearTime(Long userId, ClearTimeRequest request) {
        User user = loadActiveUser(userId);
        Monster monster = monsterRepository.findById(request.monsterId())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 몬스터입니다."));

        UserClearTime clearTime = UserClearTime.builder()
                .user(user)
                .monster(monster)
                .deckId(request.deckId())
                .clearTimeSeconds(request.clearTimeSeconds())
                .build();
        clearTimeRepository.save(clearTime);

        // EXP 지급 및 등급·호봉 재계산
        ExpGradeCalculator.GradeAndStep result =
                ExpGradeCalculator.calculate(user.getTotalExp(), CLEAR_TIME_EXP);
        user.applyExp(CLEAR_TIME_EXP, result.grade(), result.step());

        return ClearTimeResponse.of(clearTime, CLEAR_TIME_EXP);
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
