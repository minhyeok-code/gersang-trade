package org.example.gersangtrade.listing.service;

import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.RitualApplicability;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.listing.BundleEquipmentDetail;
import org.example.gersangtrade.domain.listing.BundleLine;
import org.example.gersangtrade.domain.listing.ListingBundle;
import org.example.gersangtrade.domain.listing.TradeListing;
import org.example.gersangtrade.domain.listing.enums.BundleType;
import org.example.gersangtrade.domain.listing.enums.ListingStatus;
import org.example.gersangtrade.domain.listing.enums.RitualOutcome;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.listing.dto.request.*;
import org.example.gersangtrade.listing.dto.response.ListingDetailResponse;
import org.example.gersangtrade.listing.dto.response.ListingSummaryResponse;
import org.example.gersangtrade.home.service.PriceWatchCacheEvictor;
import org.example.gersangtrade.listing.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * ListingService 단위 테스트.
 * Mockito로 레포지토리를 Mock 처리하여 서비스 로직만 검증한다.
 * BeforeEach에서 공통 Mock stub을 설정하므로 LENIENT 모드를 사용한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListingServiceTest {

    @Mock
    private TradeListingRepository tradeListingRepository;
    @Mock
    private ListingBundleRepository listingBundleRepository;
    @Mock
    private BundleLineRepository bundleLineRepository;
    @Mock
    private BundleEquipmentDetailRepository bundleEquipmentDetailRepository;
    @Mock
    private BundleEquipmentRitualRepository bundleEquipmentRitualRepository;
    @Mock
    private ListingQueryRepository listingQueryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private EquipmentItemRepository equipmentItemRepository;
    @Mock
    private RitualApplicabilityRepository ritualApplicabilityRepository;
    @Mock
    private ListingBundleTitleService listingBundleTitleService;
    @Mock
    private PriceWatchCacheEvictor priceWatchCacheEvictor;

    @InjectMocks
    private ListingService listingService;

    // ── 공통 픽스처 ──────────────────────────────────────────────────────────

    /** 정상 활성 사용자 */
    private User activeUser;
    /** 차단된 사용자 */
    private User blockedUser;
    /** 재료 아이템 (Mock) */
    private Item materialItem;
    /** 일반 장비 아이템 (Mock) */
    private Item normalEquipItem;
    /** 외변 장비 아이템 (Mock) */
    private Item appearanceEquipItem;
    /** 일반 장비의 EquipmentItem 정보 (Mock) */
    private EquipmentItem normalEquipmentItem;
    /** 외변 장비의 EquipmentItem 정보 (Mock) */
    private EquipmentItem appearanceEquipmentItem;

    @BeforeEach
    void setUp() {
        // 활성 사용자 생성
        activeUser = User.builder()
                .oauthProvider("google")
                .oauthId("google-user-1")
                .nickname("테스터")
                .email("test@example.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        // 차단 사용자 생성
        blockedUser = User.builder()
                .oauthProvider("google")
                .oauthId("google-blocked-1")
                .nickname("차단사용자")
                .email("blocked@example.com")
                .role(Role.USER)
                .status(UserStatus.BLOCKED)
                .build();

        // 재료 아이템 Mock 생성 — getType() 반환값 설정
        materialItem = mock(Item.class);
        when(materialItem.getId()).thenReturn(1L);
        when(materialItem.getType()).thenReturn(ItemType.MATERIAL);
        when(materialItem.getName()).thenReturn("강화석");

        // 일반 장비 아이템 Mock 생성 — getType() 반환값 설정
        normalEquipItem = mock(Item.class);
        when(normalEquipItem.getId()).thenReturn(2L);
        when(normalEquipItem.getType()).thenReturn(ItemType.EQUIPMENT);
        when(normalEquipItem.getName()).thenReturn("전설의 검");

        // 외변 장비 아이템 Mock 생성
        appearanceEquipItem = mock(Item.class);
        when(appearanceEquipItem.getId()).thenReturn(3L);
        when(appearanceEquipItem.getType()).thenReturn(ItemType.EQUIPMENT);
        when(appearanceEquipItem.getName()).thenReturn("용사의 갑옷");

        // 일반 EquipmentItem Mock 생성 — @MapsId로 itemId가 null이 되는 것을 방지
        normalEquipmentItem = mock(EquipmentItem.class);
        when(normalEquipmentItem.getItemId()).thenReturn(2L);
        when(normalEquipmentItem.getEquipmentKind()).thenReturn(EquipmentKind.NORMAL);

        // 외변 EquipmentItem Mock 생성
        appearanceEquipmentItem = mock(EquipmentItem.class);
        when(appearanceEquipmentItem.getItemId()).thenReturn(3L);
        when(appearanceEquipmentItem.getEquipmentKind()).thenReturn(EquipmentKind.APPEARANCE);
    }

    // ── createListing 테스트 ────────────────────────────────────────────────

    @Test
    @DisplayName("createListing_정상_재료아이템_등록글ID반환")
    void createListing_정상_재료아이템_등록글ID반환() {
        // 준비: 재료 번들 라인 요청 생성
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(
                1L, 10, 0, null // equipmentDetail=null → 재료 아이템
        );
        BundleCreateRequest bundleReq = new BundleCreateRequest(
                BundleType.MATERIAL_BUNDLE, null, List.of(lineReq)
        );
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 10000L, "메모", List.of(bundleReq)
        );

        // Mock 설정: 사용자, 아이템, TradeListing, Bundle, BundleLine 저장 결과 설정
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(materialItem));

        TradeListing savedListing = mock(TradeListing.class);
        when(savedListing.getId()).thenReturn(100L);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);

        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);

        BundleLine savedLine = mock(BundleLine.class);
        when(bundleLineRepository.save(any(BundleLine.class))).thenReturn(savedLine);

        // 실행
        Long resultId = listingService.createListing(1L, request);

        // 검증: 반환된 ID가 저장된 등록글의 ID와 일치
        assertThat(resultId).isEqualTo(100L);
        verify(tradeListingRepository).save(any(TradeListing.class));
        verify(listingBundleRepository).save(any(ListingBundle.class));
        verify(bundleLineRepository).save(any(BundleLine.class));
        // 재료 아이템이므로 장비 상세 저장이 호출되지 않아야 함
        verify(bundleEquipmentDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("createListing_정상_장비아이템_주술없음_등록글ID반환")
    void createListing_정상_장비아이템_주술없음_등록글ID반환() {
        // 준비: 주술 없는 장비 번들 라인 요청 생성
        EquipmentDetailRequest detailReq = new EquipmentDetailRequest(
                10, false, Collections.emptyList()
        );
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(
                2L, 1, 0, detailReq
        );
        BundleCreateRequest bundleReq = new BundleCreateRequest(
                BundleType.EQUIPMENT_SINGLE, null, List.of(lineReq)
        );
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 50000L, null, List.of(bundleReq)
        );

        // Mock 설정: normalEquipItem.getId()=2L이므로 findWithItemByItemId(2L)로 호출됨
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));

        TradeListing savedListing = mock(TradeListing.class);
        when(savedListing.getId()).thenReturn(101L);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);

        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);

        BundleLine savedLine = mock(BundleLine.class);
        when(bundleLineRepository.save(any(BundleLine.class))).thenReturn(savedLine);

        BundleEquipmentDetail savedDetail = mock(BundleEquipmentDetail.class);
        when(bundleEquipmentDetailRepository.save(any(BundleEquipmentDetail.class)))
                .thenReturn(savedDetail);

        // 실행
        Long resultId = listingService.createListing(1L, request);

        // 검증: 장비 상세 저장은 호출되어야 하고, 주술 저장은 호출되지 않아야 함
        assertThat(resultId).isEqualTo(101L);
        verify(bundleEquipmentDetailRepository).save(any(BundleEquipmentDetail.class));
        verify(bundleEquipmentRitualRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("createListing_정상_장비아이템_주술있음_등록글ID반환")
    void createListing_정상_장비아이템_주술있음_등록글ID반환() {
        // 준비: 주술 포함 장비 번들 라인 요청 생성
        RitualResultRequest ritualReq = new RitualResultRequest(10L, RitualOutcome.GREAT_SUCCESS);
        EquipmentDetailRequest detailReq = new EquipmentDetailRequest(
                8, true, List.of(ritualReq)
        );
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(
                2L, 1, 0, detailReq
        );
        BundleCreateRequest bundleReq = new BundleCreateRequest(
                BundleType.EQUIPMENT_SINGLE, null, List.of(lineReq)
        );
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 100000L, null, List.of(bundleReq)
        );

        // Mock 설정: 주술 적용 가능 매핑에서 ID=10인 ritual을 반환하도록 설정
        Ritual ritualWithId = mock(Ritual.class);
        when(ritualWithId.getId()).thenReturn(10L);
        when(ritualWithId.getSuccessMark()).thenReturn("00");
        when(ritualWithId.getGreatSuccessMark()).thenReturn("**");

        RitualApplicability applicability = mock(RitualApplicability.class);
        when(applicability.getRitual()).thenReturn(ritualWithId);

        // normalEquipmentItem.getItemId()=2L이므로 findWithItemByItemId(2L)로 호출됨
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(2L))
                .thenReturn(List.of(applicability));

        TradeListing savedListing = mock(TradeListing.class);
        when(savedListing.getId()).thenReturn(102L);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);

        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);

        BundleLine savedLine = mock(BundleLine.class);
        when(bundleLineRepository.save(any(BundleLine.class))).thenReturn(savedLine);

        BundleEquipmentDetail savedDetail = mock(BundleEquipmentDetail.class);
        when(bundleEquipmentDetailRepository.save(any(BundleEquipmentDetail.class)))
                .thenReturn(savedDetail);

        // 실행
        Long resultId = listingService.createListing(1L, request);

        // 검증: 주술 저장까지 호출되어야 함
        assertThat(resultId).isEqualTo(102L);
        verify(bundleEquipmentDetailRepository).save(any(BundleEquipmentDetail.class));
        verify(bundleEquipmentRitualRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("createListing_외변장비_강화수치5아닌경우_예외발생")
    void createListing_외변장비_강화수치5아닌경우_예외발생() {
        // 준비: 외변 장비인데 강화 수치가 5가 아닌 요청 생성
        EquipmentDetailRequest detailReq = new EquipmentDetailRequest(
                3, false, Collections.emptyList() // 외변에서 3강은 정책 위반
        );
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(
                3L, 1, 0, detailReq
        );
        BundleCreateRequest bundleReq = new BundleCreateRequest(
                BundleType.EQUIPMENT_SINGLE, null, List.of(lineReq)
        );
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 30000L, null, List.of(bundleReq)
        );

        // Mock 설정: 외변 장비 아이템 반환 (appearanceEquipItem.getId()=3L → findWithItemByItemId(3L))
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(3L)).thenReturn(Optional.of(appearanceEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(3L))
                .thenReturn(Optional.of(appearanceEquipmentItem));

        TradeListing savedListing = mock(TradeListing.class);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);

        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);

        BundleLine savedLine = mock(BundleLine.class);
        when(bundleLineRepository.save(any(BundleLine.class))).thenReturn(savedLine);

        // 실행 및 검증: 외변 장비 정책 위반 예외 발생
        assertThatThrownBy(() -> listingService.createListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("외변 장비의 강화 수치는 5이어야 합니다");
    }

    @Test
    @DisplayName("createListing_차단된사용자_예외발생")
    void createListing_차단된사용자_예외발생() {
        // 준비: 간단한 재료 등록 요청 생성
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(1L, 1, 0, null);
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 10000L, null,
                List.of(new BundleCreateRequest(BundleType.MATERIAL_BUNDLE, null, List.of(lineReq)))
        );

        // Mock 설정: 차단된 사용자 반환
        when(userRepository.findById(2L)).thenReturn(Optional.of(blockedUser));

        // 실행 및 검증: 차단 계정 예외 발생
        assertThatThrownBy(() -> listingService.createListing(2L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차단된 계정은 거래 등록글을 등록할 수 없습니다");
    }

    @Test
    @DisplayName("createListing_존재하지않는사용자_예외발생")
    void createListing_존재하지않는사용자_예외발생() {
        // 준비: 간단한 재료 등록 요청 생성
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(1L, 1, 0, null);
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 10000L, null,
                List.of(new BundleCreateRequest(BundleType.MATERIAL_BUNDLE, null, List.of(lineReq)))
        );

        // Mock 설정: 사용자 존재하지 않음
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // 실행 및 검증: 존재하지 않는 사용자 예외 발생
        assertThatThrownBy(() -> listingService.createListing(999L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자입니다");
    }

    @Test
    @DisplayName("createListing_존재하지않는아이템_예외발생")
    void createListing_존재하지않는아이템_예외발생() {
        // 준비: 존재하지 않는 아이템 ID를 포함한 요청 생성
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(999L, 1, 0, null);
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 10000L, null,
                List.of(new BundleCreateRequest(BundleType.MATERIAL_BUNDLE, null, List.of(lineReq)))
        );

        // Mock 설정: 사용자는 존재하지만 아이템은 존재하지 않음
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        TradeListing savedListing = mock(TradeListing.class);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);
        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);

        // 실행 및 검증: 존재하지 않는 아이템 예외 발생
        assertThatThrownBy(() -> listingService.createListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 아이템입니다");
    }

    @Test
    @DisplayName("createListing_주술일관성위반_예외발생")
    void createListing_주술일관성위반_예외발생() {
        // 준비: hasRitual=true인데 rituals 목록이 비어 있는 요청 생성
        EquipmentDetailRequest detailReq = new EquipmentDetailRequest(
                5, true, Collections.emptyList() // hasRitual=true이지만 목록 없음
        );
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(2L, 1, 0, detailReq);
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 50000L, null,
                List.of(new BundleCreateRequest(BundleType.EQUIPMENT_SINGLE, null, List.of(lineReq)))
        );

        // Mock 설정 (normalEquipItem.getId()=2L → findWithItemByItemId(2L))
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));

        TradeListing savedListing = mock(TradeListing.class);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);
        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);
        BundleLine savedLine = mock(BundleLine.class);
        when(bundleLineRepository.save(any(BundleLine.class))).thenReturn(savedLine);

        // 실행 및 검증: 주술 일관성 위반 예외 발생
        assertThatThrownBy(() -> listingService.createListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주술 적용(hasRitual=true) 시 주술 목록은 1개 이상이어야 합니다");
    }

    @Test
    @DisplayName("createListing_적용불가능한주술_예외발생")
    void createListing_적용불가능한주술_예외발생() {
        // 준비: 해당 장비에 적용 불가능한 ritualId를 포함한 요청 생성
        RitualResultRequest ritualReq = new RitualResultRequest(999L, RitualOutcome.SUCCESS); // 존재하지 않는 ritualId
        EquipmentDetailRequest detailReq = new EquipmentDetailRequest(
                5, true, List.of(ritualReq)
        );
        BundleLineCreateRequest lineReq = new BundleLineCreateRequest(2L, 1, 0, detailReq);
        ListingCreateRequest request = new ListingCreateRequest(
                "서버1", 50000L, null,
                List.of(new BundleCreateRequest(BundleType.EQUIPMENT_SINGLE, null, List.of(lineReq)))
        );

        // Mock 설정: 적용 가능한 주술 목록이 비어 있음 (normalEquipItem.getId()=2L)
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(2L))
                .thenReturn(Collections.emptyList()); // 적용 가능한 주술 없음

        TradeListing savedListing = mock(TradeListing.class);
        when(tradeListingRepository.save(any(TradeListing.class))).thenReturn(savedListing);
        ListingBundle savedBundle = mock(ListingBundle.class);
        when(listingBundleRepository.save(any(ListingBundle.class))).thenReturn(savedBundle);
        BundleLine savedLine = mock(BundleLine.class);
        when(bundleLineRepository.save(any(BundleLine.class))).thenReturn(savedLine);
        when(bundleEquipmentDetailRepository.save(any(BundleEquipmentDetail.class)))
                .thenReturn(mock(BundleEquipmentDetail.class));

        // 실행 및 검증: 적용 불가능한 주술 예외 발생
        assertThatThrownBy(() -> listingService.createListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 장비에 적용 불가능한 주술입니다");
    }

    // ── getListings 테스트 ──────────────────────────────────────────────────

    @Test
    @DisplayName("getListings_정상조회_요약목록반환")
    void getListings_정상조회_요약목록반환() {
        // 준비: Mock TradeListing과 ListingBundle 설정
        ListingSearchCondition cond = new ListingSearchCondition(null, null, null, null, null, 0, 20);

        TradeListing listing = mock(TradeListing.class);
        when(listing.getId()).thenReturn(1L);
        when(listing.getSeller()).thenReturn(activeUser);
        when(listing.getServer()).thenReturn("서버1");
        when(listing.getStatus()).thenReturn(ListingStatus.ACTIVE);
        when(listing.getPrice()).thenReturn(10000L);
        when(listing.getCreatedAt()).thenReturn(null);

        ListingBundle bundle = mock(ListingBundle.class);
        when(bundle.getListing()).thenReturn(listing);
        when(bundle.getBundleType()).thenReturn(BundleType.MATERIAL_BUNDLE);
        when(bundle.getTitleOverride()).thenReturn(null);

        when(bundle.getId()).thenReturn(10L);
        when(listingQueryRepository.search(cond)).thenReturn(List.of(listing));
        when(listingBundleRepository.findByListingIdIn(List.of(1L))).thenReturn(List.of(bundle));
        when(bundleLineRepository.findByBundleIdIn(List.of(10L))).thenReturn(List.of());
        when(listingBundleTitleService.buildSummaries(anyList(), anyMap())).thenReturn(List.of());

        // 실행
        List<ListingSummaryResponse> result = listingService.getListings(cond);

        // 검증: 요약 목록이 1건 반환되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).server()).isEqualTo("서버1");
    }

    @Test
    @DisplayName("getListings_결과없음_빈목록반환")
    void getListings_결과없음_빈목록반환() {
        // 준비: 검색 결과 없는 조건
        ListingSearchCondition cond = new ListingSearchCondition("없는서버", null, null, null, null, 0, 20);

        when(listingQueryRepository.search(cond)).thenReturn(Collections.emptyList());

        // 실행
        List<ListingSummaryResponse> result = listingService.getListings(cond);

        // 검증: 빈 목록 반환 (bundleRepository 호출 없음)
        assertThat(result).isEmpty();
        verify(listingBundleRepository, never()).findByListingIdIn(anyList());
    }

    // ── getDetail 테스트 ────────────────────────────────────────────────────

    @Test
    @DisplayName("getDetail_정상조회_상세응답반환")
    void getDetail_정상조회_상세응답반환() {
        // 준비: Mock TradeListing 및 하위 엔티티 설정
        TradeListing listing = mock(TradeListing.class);
        when(listing.getId()).thenReturn(1L);
        when(listing.getSeller()).thenReturn(activeUser);
        when(listing.getServer()).thenReturn("서버1");
        when(listing.getStatus()).thenReturn(ListingStatus.ACTIVE);
        when(listing.getPrice()).thenReturn(10000L);
        when(listing.getNote()).thenReturn(null);
        when(listing.getCreatedAt()).thenReturn(null);
        when(listing.getUpdatedAt()).thenReturn(null);

        ListingBundle bundle = mock(ListingBundle.class);
        when(bundle.getId()).thenReturn(10L);
        when(bundle.getBundleType()).thenReturn(BundleType.MATERIAL_BUNDLE);
        when(bundle.getTitleOverride()).thenReturn(null);

        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn(1L);
        when(mockItem.getName()).thenReturn("강화석");

        BundleLine line = mock(BundleLine.class);
        when(line.getId()).thenReturn(100L);
        when(line.getBundle()).thenReturn(bundle);
        when(line.getItem()).thenReturn(mockItem);
        when(line.getQuantity()).thenReturn(10);
        when(line.getSortOrder()).thenReturn(0);

        when(tradeListingRepository.findActiveById(1L)).thenReturn(Optional.of(listing));
        when(listingBundleRepository.findByListingIdOrderByIdAsc(1L)).thenReturn(List.of(bundle));
        when(bundleLineRepository.findByBundleIdIn(List.of(10L))).thenReturn(List.of(line));
        when(bundleEquipmentDetailRepository.findByBundleLineIdIn(List.of(100L)))
                .thenReturn(Collections.emptyList());
        when(bundleEquipmentRitualRepository.findWithRitualByBundleLineIdIn(List.of(100L)))
                .thenReturn(Collections.emptyList());
        when(listingBundleTitleService.resolveDetailTitle(any(), anyList())).thenReturn("강화석");

        // 실행
        ListingDetailResponse result = listingService.getDetail(1L);

        // 검증: 상세 응답이 정상 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.server()).isEqualTo("서버1");
        assertThat(result.bundles()).hasSize(1);
    }

    @Test
    @DisplayName("getDetail_존재하지않는등록글_예외발생")
    void getDetail_존재하지않는등록글_예외발생() {
        // Mock 설정: 등록글 존재하지 않음
        when(tradeListingRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // 실행 및 검증: 존재하지 않는 등록글 예외 발생
        assertThatThrownBy(() -> listingService.getDetail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 등록글입니다");
    }

    // ── cancelListing 테스트 ────────────────────────────────────────────────

    @Test
    @DisplayName("cancelListing_정상취소_상태변경")
    void cancelListing_정상취소_상태변경() {
        // 준비: ACTIVE 상태의 등록글 Mock 생성
        TradeListing listing = mock(TradeListing.class);
        when(listing.getSeller()).thenReturn(activeUser);
        when(listing.getStatus()).thenReturn(ListingStatus.ACTIVE);

        // activeUser의 ID를 설정하기 위해 spy 방식 사용
        User sellerSpy = spy(activeUser);
        doReturn(1L).when(sellerSpy).getId();
        when(listing.getSeller()).thenReturn(sellerSpy);

        when(tradeListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행
        listingService.cancelListing(1L, 1L);

        // 검증: cancel()과 softDelete()가 호출되어야 함
        verify(listing).cancel();
        verify(listing).softDelete();
    }

    @Test
    @DisplayName("cancelListing_본인아닌경우_예외발생")
    void cancelListing_본인아닌경우_예외발생() {
        // 준비: 다른 사용자 소유의 등록글 Mock 생성
        User owner = spy(activeUser);
        doReturn(1L).when(owner).getId(); // 소유자 ID = 1

        TradeListing listing = mock(TradeListing.class);
        when(listing.getSeller()).thenReturn(owner);

        when(tradeListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행 및 검증: 본인 아닌 경우 예외 발생 (요청자 ID = 2, 소유자 ID = 1)
        assertThatThrownBy(() -> listingService.cancelListing(2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 등록글만 취소할 수 있습니다");
    }

    @Test
    @DisplayName("cancelListing_이미취소된경우_예외발생")
    void cancelListing_이미취소된경우_예외발생() {
        // 준비: 이미 CANCELLED 상태인 등록글 Mock 생성
        User owner = spy(activeUser);
        doReturn(1L).when(owner).getId();

        TradeListing listing = mock(TradeListing.class);
        when(listing.getSeller()).thenReturn(owner);
        when(listing.getStatus()).thenReturn(ListingStatus.CANCELLED);

        when(tradeListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행 및 검증: 이미 취소된 등록글 예외 발생
        assertThatThrownBy(() -> listingService.cancelListing(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료되거나 취소된 거래 등록글입니다");
    }

    @Test
    @DisplayName("cancelListing_SOLD상태_예외발생")
    void cancelListing_SOLD상태_예외발생() {
        // 준비: SOLD 상태인 등록글 Mock 생성
        User owner = spy(activeUser);
        doReturn(1L).when(owner).getId();

        TradeListing listing = mock(TradeListing.class);
        when(listing.getSeller()).thenReturn(owner);
        when(listing.getStatus()).thenReturn(ListingStatus.SOLD);

        when(tradeListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행 및 검증: SOLD(거래 완료) 상태는 취소 불가 예외 발생
        assertThatThrownBy(() -> listingService.cancelListing(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료되거나 취소된 거래 등록글입니다");
    }

    // ── hideListing / unhideListing 테스트 ─────────────────────────────────

    @Test
    @DisplayName("hideListing_정상숨김_hide호출")
    void hideListing_정상숨김_hide호출() {
        // 준비: 삭제되지 않은 등록글 Mock
        TradeListing listing = mock(TradeListing.class);
        when(tradeListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행
        listingService.hideListing(1L);

        // 검증: hide()가 호출되어야 함
        verify(listing).hide();
    }

    @Test
    @DisplayName("hideListing_존재하지않는등록글_예외발생")
    void hideListing_존재하지않는등록글_예외발생() {
        // Mock 설정: 등록글 없음 (삭제됐거나 미존재)
        when(tradeListingRepository.findNotDeletedById(999L)).thenReturn(Optional.empty());

        // 실행 및 검증
        assertThatThrownBy(() -> listingService.hideListing(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 등록글입니다");
    }

    @Test
    @DisplayName("unhideListing_정상숨김해제_unhide호출")
    void unhideListing_정상숨김해제_unhide호출() {
        // 준비: 삭제되지 않은 등록글 Mock
        TradeListing listing = mock(TradeListing.class);
        when(tradeListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행
        listingService.unhideListing(1L);

        // 검증: unhide()가 호출되어야 함
        verify(listing).unhide();
    }

    @Test
    @DisplayName("unhideListing_존재하지않는등록글_예외발생")
    void unhideListing_존재하지않는등록글_예외발생() {
        // Mock 설정: 등록글 없음 (삭제됐거나 미존재)
        when(tradeListingRepository.findNotDeletedById(999L)).thenReturn(Optional.empty());

        // 실행 및 검증
        assertThatThrownBy(() -> listingService.unhideListing(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 등록글입니다");
    }
}
