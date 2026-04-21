package org.example.gersangtrade.wanted.service;

import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.RitualApplicabilityRepository;
import org.example.gersangtrade.domain.catalog.EquipmentItem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Ritual;
import org.example.gersangtrade.domain.catalog.RitualApplicability;
import org.example.gersangtrade.domain.catalog.enums.EquipmentKind;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.wanted.WantedEquipmentCondition;
import org.example.gersangtrade.domain.wanted.WantedItem;
import org.example.gersangtrade.domain.wanted.WantedListing;
import org.example.gersangtrade.domain.wanted.WantedRitualCondition;
import org.example.gersangtrade.domain.wanted.enums.PreferredOutcome;
import org.example.gersangtrade.domain.wanted.enums.WantedStatus;
import org.example.gersangtrade.wanted.dto.request.*;
import org.example.gersangtrade.wanted.dto.response.WantedListingDetailResponse;
import org.example.gersangtrade.wanted.dto.response.WantedListingSummaryResponse;
import org.example.gersangtrade.wanted.repository.*;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * WantedListingService 단위 테스트.
 * Mockito로 레포지토리를 Mock 처리하여 서비스 로직만 검증한다.
 * BeforeEach에서 공통 Mock stub을 설정하므로 LENIENT 모드를 사용한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WantedListingServiceTest {

    @Mock
    private WantedListingRepository wantedListingRepository;
    @Mock
    private WantedItemRepository wantedItemRepository;
    @Mock
    private WantedEquipmentConditionRepository wantedEquipmentConditionRepository;
    @Mock
    private WantedRitualConditionRepository wantedRitualConditionRepository;
    @Mock
    private WantedListingQueryRepository wantedListingQueryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private EquipmentItemRepository equipmentItemRepository;
    @Mock
    private RitualApplicabilityRepository ritualApplicabilityRepository;

    @InjectMocks
    private WantedListingService wantedListingService;

    // ── 공통 픽스처 ──────────────────────────────────────────────────────────

    /** 정상 활성 구매자 */
    private User activeBuyer;
    /** 차단된 구매자 */
    private User blockedBuyer;
    /** 재료 아이템 (Mock) */
    private Item materialItem;
    /** 일반 장비 아이템 (Mock) */
    private Item normalEquipItem;
    /** 일반 장비의 EquipmentItem 정보 (Mock) */
    private EquipmentItem normalEquipmentItem;
    /** 외변 장비 아이템 (Mock) */
    private Item appearanceEquipItem;
    /** 외변 장비의 EquipmentItem 정보 (Mock) */
    private EquipmentItem appearanceEquipmentItem;

    @BeforeEach
    void setUp() {
        // 활성 구매자 생성
        activeBuyer = User.builder()
                .oauthProvider("google")
                .oauthId("google-buyer-1")
                .nickname("구매자")
                .email("buyer@example.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        // 차단된 구매자 생성
        blockedBuyer = User.builder()
                .oauthProvider("google")
                .oauthId("google-blocked-buyer-1")
                .nickname("차단구매자")
                .email("blockedbuyer@example.com")
                .role(Role.USER)
                .status(UserStatus.BLOCKED)
                .build();

        // 재료 아이템 Mock 생성 — getType() 반환값 설정
        materialItem = mock(Item.class);
        when(materialItem.getId()).thenReturn(1L);
        when(materialItem.getType()).thenReturn(ItemType.MATERIAL);
        when(materialItem.getName()).thenReturn("마력의 결정");

        // 일반 장비 아이템 Mock 생성 — getType() 반환값 설정
        normalEquipItem = mock(Item.class);
        when(normalEquipItem.getId()).thenReturn(2L);
        when(normalEquipItem.getType()).thenReturn(ItemType.EQUIPMENT);
        when(normalEquipItem.getName()).thenReturn("용의 방패");

        // 일반 EquipmentItem Mock 생성 — @MapsId로 itemId가 null이 되는 것을 방지
        normalEquipmentItem = mock(EquipmentItem.class);
        when(normalEquipmentItem.getItemId()).thenReturn(2L);
        when(normalEquipmentItem.getEquipmentKind()).thenReturn(EquipmentKind.NORMAL);

        // 외변 장비 아이템 Mock 생성
        appearanceEquipItem = mock(Item.class);
        when(appearanceEquipItem.getId()).thenReturn(3L);
        when(appearanceEquipItem.getType()).thenReturn(ItemType.EQUIPMENT);
        when(appearanceEquipItem.getName()).thenReturn("외변 갑옷");

        appearanceEquipmentItem = mock(EquipmentItem.class);
        when(appearanceEquipmentItem.getItemId()).thenReturn(3L);
        when(appearanceEquipmentItem.getEquipmentKind()).thenReturn(EquipmentKind.APPEARANCE);
    }

    // ── createWantedListing 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("createWantedListing_정상_재료아이템_등록글ID반환")
    void createWantedListing_정상_재료아이템_등록글ID반환() {
        // 준비: 재료 아이템 구매 희망 요청 생성
        WantedItemRequest itemReq = new WantedItemRequest(
                1L, 10, 0, null // equipmentCondition=null → 재료 아이템
        );
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 5000L, "메모", List.of(itemReq)
        );

        // Mock 설정
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(materialItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(200L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);

        WantedItem savedItem = mock(WantedItem.class);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(savedItem);

        // 실행
        Long resultId = wantedListingService.createWantedListing(1L, request);

        // 검증: 반환된 ID가 저장된 등록글의 ID와 일치
        assertThat(resultId).isEqualTo(200L);
        verify(wantedListingRepository).save(any(WantedListing.class));
        verify(wantedItemRepository).save(any(WantedItem.class));
        // 재료 아이템이므로 장비 조건 저장이 호출되지 않아야 함
        verify(wantedEquipmentConditionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWantedListing_정상_장비아이템_주술없음_등록글ID반환")
    void createWantedListing_정상_장비아이템_주술없음_등록글ID반환() {
        // 준비: 주술 없는 장비 구매 희망 요청 생성
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, false, Collections.emptyList() // 주술 없음
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 40000L, null, List.of(itemReq)
        );

        // Mock 설정 (normalEquipItem.getId()=2L → findWithItemByItemId(2L))
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(201L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);

        WantedItem savedItem = mock(WantedItem.class);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(savedItem);

        WantedEquipmentCondition savedCond = mock(WantedEquipmentCondition.class);
        when(wantedEquipmentConditionRepository.save(any(WantedEquipmentCondition.class)))
                .thenReturn(savedCond);

        // 실행
        Long resultId = wantedListingService.createWantedListing(1L, request);

        // 검증: 장비 조건 저장은 호출되어야 하고, 주술 조건 저장은 호출되지 않아야 함
        assertThat(resultId).isEqualTo(201L);
        verify(wantedEquipmentConditionRepository).save(any(WantedEquipmentCondition.class));
        verify(wantedRitualConditionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("createWantedListing_정상_장비아이템_주술있음_등록글ID반환")
    void createWantedListing_정상_장비아이템_주술있음_등록글ID반환() {
        // 준비: 주술 포함 장비 구매 희망 요청 생성
        WantedRitualConditionRequest ritualCondReq = new WantedRitualConditionRequest(
                10L, PreferredOutcome.ANY
        );
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, true, List.of(ritualCondReq) // 주술 포함
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 80000L, null, List.of(itemReq)
        );

        // Mock 설정: 주술 적용 가능 매핑에서 ID=10인 ritual 반환 (normalEquipItem.getId()=2L)
        Ritual ritualWithId = mock(Ritual.class);
        when(ritualWithId.getId()).thenReturn(10L);

        RitualApplicability applicability = mock(RitualApplicability.class);
        when(applicability.getRitual()).thenReturn(ritualWithId);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(2L))
                .thenReturn(List.of(applicability));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(202L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);

        WantedItem savedItem = mock(WantedItem.class);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(savedItem);

        WantedEquipmentCondition savedCond = mock(WantedEquipmentCondition.class);
        when(wantedEquipmentConditionRepository.save(any(WantedEquipmentCondition.class)))
                .thenReturn(savedCond);

        // 실행
        Long resultId = wantedListingService.createWantedListing(1L, request);

        // 검증: 주술 조건 저장까지 호출되어야 함
        assertThat(resultId).isEqualTo(202L);
        verify(wantedEquipmentConditionRepository).save(any(WantedEquipmentCondition.class));
        verify(wantedRitualConditionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("createWantedListing_차단된사용자_예외발생")
    void createWantedListing_차단된사용자_예외발생() {
        // 준비: 간단한 재료 구매 희망 요청 생성
        WantedItemRequest itemReq = new WantedItemRequest(1L, 1, 0, null);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 5000L, null, List.of(itemReq)
        );

        // Mock 설정: 차단된 구매자 반환
        when(userRepository.findById(2L)).thenReturn(Optional.of(blockedBuyer));

        // 실행 및 검증: 차단 계정 예외 발생
        assertThatThrownBy(() -> wantedListingService.createWantedListing(2L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차단된 계정은 구매 희망 등록글을 등록할 수 없습니다");
    }

    @Test
    @DisplayName("createWantedListing_존재하지않는사용자_예외발생")
    void createWantedListing_존재하지않는사용자_예외발생() {
        // 준비: 간단한 재료 구매 희망 요청 생성
        WantedItemRequest itemReq = new WantedItemRequest(1L, 1, 0, null);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 5000L, null, List.of(itemReq)
        );

        // Mock 설정: 사용자 존재하지 않음
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // 실행 및 검증: 존재하지 않는 사용자 예외 발생
        assertThatThrownBy(() -> wantedListingService.createWantedListing(999L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사용자입니다");
    }

    @Test
    @DisplayName("createWantedListing_존재하지않는아이템_예외발생")
    void createWantedListing_존재하지않는아이템_예외발생() {
        // 준비: 존재하지 않는 아이템 ID를 포함한 요청 생성
        WantedItemRequest itemReq = new WantedItemRequest(999L, 1, 0, null);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 5000L, null, List.of(itemReq)
        );

        // Mock 설정: 사용자는 존재하지만 아이템은 존재하지 않음
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        WantedListing savedListing = mock(WantedListing.class);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);

        // 실행 및 검증: 존재하지 않는 아이템 예외 발생
        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 아이템입니다");
    }

    @Test
    @DisplayName("createWantedListing_주술일관성위반_예외발생")
    void createWantedListing_주술일관성위반_예외발생() {
        // 준비: hasRitual=true인데 ritualConditions 목록이 비어 있는 요청 생성
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, true, Collections.emptyList() // hasRitual=true이지만 조건 목록 없음
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 40000L, null, List.of(itemReq)
        );

        // Mock 설정 (normalEquipItem.getId()=2L → findWithItemByItemId(2L))
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        WantedItem savedItem = mock(WantedItem.class);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(savedItem);

        // 실행 및 검증: 주술 일관성 위반 예외 발생
        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주술 조건(hasRitual=true) 시 주술 조건 목록은 1개 이상이어야 합니다");
    }

    @Test
    @DisplayName("createWantedListing_적용불가능한주술_예외발생")
    void createWantedListing_적용불가능한주술_예외발생() {
        // 준비: 해당 장비에 적용 불가능한 ritualId를 포함한 요청 생성
        WantedRitualConditionRequest ritualCondReq = new WantedRitualConditionRequest(
                999L, PreferredOutcome.ANY // 존재하지 않는 ritualId
        );
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, true, List.of(ritualCondReq)
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 40000L, null, List.of(itemReq)
        );

        // Mock 설정: 적용 가능한 주술 목록이 비어 있음 (normalEquipItem.getId()=2L)
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(2L))
                .thenReturn(Collections.emptyList()); // 적용 가능한 주술 없음

        WantedListing savedListing = mock(WantedListing.class);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        WantedItem savedItem = mock(WantedItem.class);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(savedItem);
        when(wantedEquipmentConditionRepository.save(any(WantedEquipmentCondition.class)))
                .thenReturn(mock(WantedEquipmentCondition.class));

        // 실행 및 검증: 적용 불가능한 주술 예외 발생
        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 장비에 적용 불가능한 주술입니다");
    }

    // ── getWantedListings 테스트 ────────────────────────────────────────────

    @Test
    @DisplayName("getWantedListings_정상조회_요약목록반환")
    void getWantedListings_정상조회_요약목록반환() {
        // 준비: Mock WantedListing과 WantedItem 설정
        WantedSearchCondition cond = new WantedSearchCondition(null, null, null, 0, 20);

        WantedListing listing = mock(WantedListing.class);
        when(listing.getId()).thenReturn(1L);
        when(listing.getBuyer()).thenReturn(activeBuyer);
        when(listing.getServer()).thenReturn("서버1");
        when(listing.getStatus()).thenReturn(WantedStatus.OPEN);
        when(listing.getOfferedPrice()).thenReturn(5000L);
        when(listing.getCreatedAt()).thenReturn(null);

        Item mockItem = mock(Item.class);
        when(mockItem.getName()).thenReturn("마력의 결정");

        WantedItem wantedItem = mock(WantedItem.class);
        when(wantedItem.getWantedListing()).thenReturn(listing);
        when(wantedItem.getItem()).thenReturn(mockItem);

        when(wantedListingQueryRepository.search(cond)).thenReturn(List.of(listing));
        when(wantedItemRepository.findByWantedListingIdIn(List.of(1L))).thenReturn(List.of(wantedItem));

        // 실행
        List<WantedListingSummaryResponse> result = wantedListingService.getWantedListings(cond);

        // 검증: 요약 목록이 1건 반환되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).server()).isEqualTo("서버1");
        assertThat(result.get(0).itemNames()).contains("마력의 결정");
    }

    @Test
    @DisplayName("getWantedListings_결과없음_빈목록반환")
    void getWantedListings_결과없음_빈목록반환() {
        // 준비: 검색 결과 없는 조건
        WantedSearchCondition cond = new WantedSearchCondition("없는서버", null, null, 0, 20);

        when(wantedListingQueryRepository.search(cond)).thenReturn(Collections.emptyList());

        // 실행
        List<WantedListingSummaryResponse> result = wantedListingService.getWantedListings(cond);

        // 검증: 빈 목록 반환 (wantedItemRepository 호출 없음)
        assertThat(result).isEmpty();
        verify(wantedItemRepository, never()).findByWantedListingIdIn(anyList());
    }

    // ── getDetail 테스트 ────────────────────────────────────────────────────

    @Test
    @DisplayName("getDetail_정상조회_상세응답반환")
    void getDetail_정상조회_상세응답반환() {
        // 준비: Mock WantedListing 및 하위 엔티티 설정
        WantedListing listing = mock(WantedListing.class);
        when(listing.getId()).thenReturn(1L);
        when(listing.getBuyer()).thenReturn(activeBuyer);
        when(listing.getServer()).thenReturn("서버1");
        when(listing.getStatus()).thenReturn(WantedStatus.OPEN);
        when(listing.getOfferedPrice()).thenReturn(5000L);
        when(listing.getNote()).thenReturn(null);
        when(listing.getCreatedAt()).thenReturn(null);
        when(listing.getUpdatedAt()).thenReturn(null);

        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn(1L);
        when(mockItem.getName()).thenReturn("마력의 결정");

        WantedItem wantedItem = mock(WantedItem.class);
        when(wantedItem.getId()).thenReturn(10L);
        when(wantedItem.getItem()).thenReturn(mockItem);
        when(wantedItem.getQuantity()).thenReturn(5);
        when(wantedItem.getSortOrder()).thenReturn(0);

        when(wantedListingRepository.findActiveById(1L)).thenReturn(Optional.of(listing));
        when(wantedItemRepository.findByWantedListingIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(wantedItem));
        when(wantedEquipmentConditionRepository.findByWantedItemIdIn(List.of(10L)))
                .thenReturn(Collections.emptyList());
        when(wantedRitualConditionRepository.findWithRitualByWantedItemIdIn(List.of(10L)))
                .thenReturn(Collections.emptyList());

        // 실행
        WantedListingDetailResponse result = wantedListingService.getDetail(1L);

        // 검증: 상세 응답이 정상 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.server()).isEqualTo("서버1");
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getDetail_존재하지않는등록글_예외발생")
    void getDetail_존재하지않는등록글_예외발생() {
        // Mock 설정: 등록글 존재하지 않음
        when(wantedListingRepository.findActiveById(999L)).thenReturn(Optional.empty());

        // 실행 및 검증: 존재하지 않는 등록글 예외 발생
        assertThatThrownBy(() -> wantedListingService.getDetail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 구매 희망 등록글입니다");
    }

    // ── 강화 수치 경계값 테스트 ──────────────────────────────────────────────

    @Test
    @DisplayName("외변장비_minEnhanceLevel_null이면_통과")
    void 외변장비_minEnhanceLevel_null이면_통과() {
        // null은 강화 수치 무관을 의미하므로 외변 장비에 허용
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                null, false, Collections.emptyList()
        );
        WantedItemRequest itemReq = new WantedItemRequest(3L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 10000L, null, List.of(itemReq)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(3L)).thenReturn(Optional.of(appearanceEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(3L))
                .thenReturn(Optional.of(appearanceEquipmentItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(300L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(mock(WantedItem.class));
        when(wantedEquipmentConditionRepository.save(any()))
                .thenReturn(mock(WantedEquipmentCondition.class));

        Long resultId = wantedListingService.createWantedListing(1L, request);

        // 예외 없이 정상 등록
        assertThat(resultId).isEqualTo(300L);
    }

    @Test
    @DisplayName("외변장비_minEnhanceLevel_5이면_통과")
    void 외변장비_minEnhanceLevel_5이면_통과() {
        // 5강은 외변 장비의 유일한 유효 강화 수치
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, false, Collections.emptyList()
        );
        WantedItemRequest itemReq = new WantedItemRequest(3L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 10000L, null, List.of(itemReq)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(3L)).thenReturn(Optional.of(appearanceEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(3L))
                .thenReturn(Optional.of(appearanceEquipmentItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(301L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(mock(WantedItem.class));
        when(wantedEquipmentConditionRepository.save(any()))
                .thenReturn(mock(WantedEquipmentCondition.class));

        Long resultId = wantedListingService.createWantedListing(1L, request);

        assertThat(resultId).isEqualTo(301L);
    }

    @Test
    @DisplayName("외변장비_minEnhanceLevel_5아닌경우_예외발생")
    void 외변장비_minEnhanceLevel_5아닌경우_예외발생() {
        // 외변 장비에 3강 조건 → 정책 위반
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                3, false, Collections.emptyList()
        );
        WantedItemRequest itemReq = new WantedItemRequest(3L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 10000L, null, List.of(itemReq)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(3L)).thenReturn(Optional.of(appearanceEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(3L))
                .thenReturn(Optional.of(appearanceEquipmentItem));
        when(wantedListingRepository.save(any())).thenReturn(mock(WantedListing.class));
        when(wantedItemRepository.save(any())).thenReturn(mock(WantedItem.class));

        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("외변 장비의 최소 강화 수치는 null 또는 5이어야 합니다");
    }

    @Test
    @DisplayName("일반장비_minEnhanceLevel_경계값0_통과")
    void 일반장비_minEnhanceLevel_경계값0_통과() {
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                0, false, Collections.emptyList()
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 40000L, null, List.of(itemReq)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(302L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(mock(WantedItem.class));
        when(wantedEquipmentConditionRepository.save(any()))
                .thenReturn(mock(WantedEquipmentCondition.class));

        Long resultId = wantedListingService.createWantedListing(1L, request);

        assertThat(resultId).isEqualTo(302L);
    }

    @Test
    @DisplayName("일반장비_minEnhanceLevel_경계값20_통과")
    void 일반장비_minEnhanceLevel_경계값20_통과() {
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                20, false, Collections.emptyList()
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 40000L, null, List.of(itemReq)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(303L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(mock(WantedItem.class));
        when(wantedEquipmentConditionRepository.save(any()))
                .thenReturn(mock(WantedEquipmentCondition.class));

        Long resultId = wantedListingService.createWantedListing(1L, request);

        assertThat(resultId).isEqualTo(303L);
    }

    // ── 복수 주술 조건 및 중복 검사 ───────────────────────────────────────────

    @Test
    @DisplayName("createWantedListing_주술조건2개_모두저장")
    void createWantedListing_주술조건2개_모두저장() {
        // 주술 조건 2개를 동시에 등록하는 경우
        WantedRitualConditionRequest ritual1 = new WantedRitualConditionRequest(10L, PreferredOutcome.ANY);
        WantedRitualConditionRequest ritual2 = new WantedRitualConditionRequest(20L, PreferredOutcome.ANY);
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, true, List.of(ritual1, ritual2)
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 80000L, null, List.of(itemReq)
        );

        // 두 주술 모두 적용 가능하도록 설정
        Ritual r1 = mock(Ritual.class);
        when(r1.getId()).thenReturn(10L);
        Ritual r2 = mock(Ritual.class);
        when(r2.getId()).thenReturn(20L);

        RitualApplicability ap1 = mock(RitualApplicability.class);
        when(ap1.getRitual()).thenReturn(r1);
        RitualApplicability ap2 = mock(RitualApplicability.class);
        when(ap2.getRitual()).thenReturn(r2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(2L))
                .thenReturn(List.of(ap1, ap2));

        WantedListing savedListing = mock(WantedListing.class);
        when(savedListing.getId()).thenReturn(304L);
        when(wantedListingRepository.save(any(WantedListing.class))).thenReturn(savedListing);
        when(wantedItemRepository.save(any(WantedItem.class))).thenReturn(mock(WantedItem.class));
        when(wantedEquipmentConditionRepository.save(any()))
                .thenReturn(mock(WantedEquipmentCondition.class));

        Long resultId = wantedListingService.createWantedListing(1L, request);

        assertThat(resultId).isEqualTo(304L);
        // 2개의 주술 조건이 saveAll로 저장되어야 함
        verify(wantedRitualConditionRepository).saveAll(argThat(list ->
                ((List<?>) list).size() == 2
        ));
    }

    @Test
    @DisplayName("createWantedListing_중복ritualId_예외발생")
    void createWantedListing_중복ritualId_예외발생() {
        // 동일한 ritualId 2개 포함 → 중복 검사에서 예외
        WantedRitualConditionRequest ritual1 = new WantedRitualConditionRequest(10L, PreferredOutcome.ANY);
        WantedRitualConditionRequest ritual2 = new WantedRitualConditionRequest(10L, PreferredOutcome.GREAT_SUCCESS);
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, true, List.of(ritual1, ritual2)
        );
        WantedItemRequest itemReq = new WantedItemRequest(2L, 1, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 80000L, null, List.of(itemReq)
        );

        Ritual r1 = mock(Ritual.class);
        when(r1.getId()).thenReturn(10L);
        RitualApplicability ap1 = mock(RitualApplicability.class);
        when(ap1.getRitual()).thenReturn(r1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(ritualApplicabilityRepository.findByEquipmentItemIdWithRitual(2L))
                .thenReturn(List.of(ap1));
        when(wantedListingRepository.save(any())).thenReturn(mock(WantedListing.class));
        when(wantedItemRepository.save(any())).thenReturn(mock(WantedItem.class));
        when(wantedEquipmentConditionRepository.save(any()))
                .thenReturn(mock(WantedEquipmentCondition.class));

        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복된 ritualId");
    }

    // ── 수량·sortOrder 경계값 테스트 ─────────────────────────────────────────

    @Test
    @DisplayName("createWantedListing_장비수량2이상_예외발생")
    void createWantedListing_장비수량2이상_예외발생() {
        WantedEquipmentConditionRequest condReq = new WantedEquipmentConditionRequest(
                5, false, Collections.emptyList()
        );
        // 장비인데 quantity=2 입력
        WantedItemRequest itemReq = new WantedItemRequest(2L, 2, 0, condReq);
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 40000L, null, List.of(itemReq)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(normalEquipItem));
        when(equipmentItemRepository.findWithItemByItemId(2L))
                .thenReturn(Optional.of(normalEquipmentItem));
        when(wantedListingRepository.save(any())).thenReturn(mock(WantedListing.class));
        when(wantedItemRepository.save(any())).thenReturn(mock(WantedItem.class));

        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("장비 아이템의 수량은 1이어야 합니다");
    }

    @Test
    @DisplayName("createWantedListing_sortOrder중복_예외발생")
    void createWantedListing_sortOrder중복_예외발생() {
        // 두 아이템이 sortOrder=0 으로 동일
        WantedItemRequest item1 = new WantedItemRequest(1L, 10, 0, null);
        WantedItemRequest item2 = new WantedItemRequest(2L, 1, 0, null); // sortOrder 중복
        WantedListingCreateRequest request = new WantedListingCreateRequest(
                "서버1", 5000L, null, List.of(item1, item2)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeBuyer));

        assertThatThrownBy(() -> wantedListingService.createWantedListing(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sortOrder 값이 중복");
    }

    // ── cancelWantedListing 테스트 ──────────────────────────────────────────

    @Test
    @DisplayName("cancelWantedListing_정상취소_상태변경")
    void cancelWantedListing_정상취소_상태변경() {
        // 준비: OPEN 상태의 등록글 Mock 생성
        User buyerSpy = spy(activeBuyer);
        doReturn(1L).when(buyerSpy).getId();

        WantedListing listing = mock(WantedListing.class);
        when(listing.getBuyer()).thenReturn(buyerSpy);
        when(listing.getStatus()).thenReturn(WantedStatus.OPEN);

        when(wantedListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행
        wantedListingService.cancelWantedListing(1L, 1L);

        // 검증: cancel()과 softDelete()가 호출되어야 함
        verify(listing).cancel();
        verify(listing).softDelete();
    }

    @Test
    @DisplayName("cancelWantedListing_본인아닌경우_예외발생")
    void cancelWantedListing_본인아닌경우_예외발생() {
        // 준비: 다른 사용자 소유의 등록글 Mock 생성
        User owner = spy(activeBuyer);
        doReturn(1L).when(owner).getId(); // 소유자 ID = 1

        WantedListing listing = mock(WantedListing.class);
        when(listing.getBuyer()).thenReturn(owner);

        when(wantedListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행 및 검증: 본인 아닌 경우 예외 발생 (요청자 ID = 2, 소유자 ID = 1)
        assertThatThrownBy(() -> wantedListingService.cancelWantedListing(2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 등록글만 취소할 수 있습니다");
    }

    @Test
    @DisplayName("cancelWantedListing_이미취소된경우_예외발생")
    void cancelWantedListing_이미취소된경우_예외발생() {
        // 준비: 이미 CANCELLED 상태인 등록글 Mock 생성
        User owner = spy(activeBuyer);
        doReturn(1L).when(owner).getId();

        WantedListing listing = mock(WantedListing.class);
        when(listing.getBuyer()).thenReturn(owner);
        when(listing.getStatus()).thenReturn(WantedStatus.CANCELLED);

        when(wantedListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행 및 검증: 이미 취소된 등록글 예외 발생
        assertThatThrownBy(() -> wantedListingService.cancelWantedListing(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료되거나 취소된 구매 희망 등록글입니다");
    }

    @Test
    @DisplayName("cancelWantedListing_PURCHASED상태_예외발생")
    void cancelWantedListing_PURCHASED상태_예외발생() {
        // 준비: PURCHASED 상태인 등록글 Mock 생성
        User owner = spy(activeBuyer);
        doReturn(1L).when(owner).getId();

        WantedListing listing = mock(WantedListing.class);
        when(listing.getBuyer()).thenReturn(owner);
        when(listing.getStatus()).thenReturn(WantedStatus.PURCHASED);

        when(wantedListingRepository.findNotDeletedById(1L)).thenReturn(Optional.of(listing));

        // 실행 및 검증: PURCHASED(구매 완료) 상태는 취소 불가 예외 발생
        assertThatThrownBy(() -> wantedListingService.cancelWantedListing(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 완료되거나 취소된 구매 희망 등록글입니다");
    }
}
