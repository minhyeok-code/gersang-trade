package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.catalog.repository.EquipmentItemRepository;
import org.example.gersangtrade.catalog.repository.EquipmentSetRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ServerRepository;
import org.example.gersangtrade.domain.catalog.EquipmentSet;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserWatchTarget;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.Role;
import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.domain.user.enums.WatchTargetType;
import org.example.gersangtrade.user.repository.UserWatchTargetRepository;
import org.example.gersangtrade.watchlist.dto.request.WatchTargetAddRequest;
import org.example.gersangtrade.watchlist.dto.response.WatchTargetResponse;
import org.example.gersangtrade.watchlist.exception.WatchlistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WatchlistServiceTest {

    @Mock private UserWatchTargetRepository watchTargetRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private EquipmentItemRepository equipmentItemRepository;
    @Mock private EquipmentSetRepository equipmentSetRepository;
    @Mock private ServerRepository serverRepository;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private WatchlistService watchlistService;

    private User user;
    private Item materialItem;
    private Item equipmentItem;
    private EquipmentSet equipmentSet;

    @BeforeEach
    void setUp() {
        user = spy(User.builder()
                .oauthProvider("google").oauthId("g1")
                .nickname("테스터").email("t@t.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(1L).when(user).getId();

        materialItem = mock(Item.class);
        when(materialItem.getId()).thenReturn(10L);
        when(materialItem.getType()).thenReturn(ItemType.MATERIAL);

        equipmentItem = mock(Item.class);
        when(equipmentItem.getId()).thenReturn(20L);
        when(equipmentItem.getType()).thenReturn(ItemType.EQUIPMENT);

        equipmentSet = mock(EquipmentSet.class);
        when(equipmentSet.getId()).thenReturn(100L);
        when(equipmentSet.getName()).thenReturn("각성광목천왕");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(any())).thenReturn(cache);
        when(serverRepository.findAll()).thenReturn(List.of());
    }

    // ── add: ITEM ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add_ITEM타입_재료_주술없음_정상저장")
    void add_재료아이템_주술없음_저장() {
        when(watchTargetRepository.countByUserId(1L)).thenReturn(0L);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(materialItem));
        when(watchTargetRepository.existsByUserIdAndWatchKey(1L, "ITEM:10")).thenReturn(false);

        UserWatchTarget saved = spy(UserWatchTarget.builder()
                .user(user).targetType(WatchTargetType.ITEM)
                .watchKey("ITEM:10").item(materialItem).build());
        doReturn(1L).when(saved).getId();
        when(watchTargetRepository.save(any())).thenReturn(saved);

        WatchTargetResponse response = watchlistService.add(1L,
                new WatchTargetAddRequest(WatchTargetType.ITEM, 10L, null, null, null, null));

        assertThat(response).isNotNull();
        verify(watchTargetRepository).save(any());
    }

    @Test
    @DisplayName("add_재료아이템에_주술등록_예외발생")
    void add_재료아이템_주술등록_예외() {
        when(watchTargetRepository.countByUserId(1L)).thenReturn(0L);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(materialItem));

        assertThatThrownBy(() -> watchlistService.add(1L,
                new WatchTargetAddRequest(WatchTargetType.ITEM, 10L, "<개양>", null, null, null)))
                .isInstanceOf(WatchlistException.class)
                .hasMessageContaining("재료 아이템");
    }

    @Test
    @DisplayName("add_최대5개초과_예외발생")
    void add_한도초과_예외() {
        when(watchTargetRepository.countByUserId(1L)).thenReturn(5L);

        assertThatThrownBy(() -> watchlistService.add(1L,
                new WatchTargetAddRequest(WatchTargetType.ITEM, 10L, null, null, null, null)))
                .isInstanceOf(WatchlistException.class)
                .extracting("errorCode").isEqualTo("WATCH_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("add_중복watchKey_예외발생")
    void add_중복_예외() {
        when(watchTargetRepository.countByUserId(1L)).thenReturn(1L);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(materialItem));
        when(watchTargetRepository.existsByUserIdAndWatchKey(1L, "ITEM:10")).thenReturn(true);

        assertThatThrownBy(() -> watchlistService.add(1L,
                new WatchTargetAddRequest(WatchTargetType.ITEM, 10L, null, null, null, null)))
                .isInstanceOf(WatchlistException.class)
                .extracting("errorCode").isEqualTo("DUPLICATE_WATCH_ITEM");
    }

    // ── add: SET ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add_SET타입_BANSSANG이면_ritual_강제초기화")
    void add_SET_BANSSANG_ritual_초기화() {
        when(watchTargetRepository.countByUserId(1L)).thenReturn(0L);
        when(equipmentSetRepository.findById(100L)).thenReturn(Optional.of(equipmentSet));
        // BANSSANG → ritualCount·ritualMark 무시 → watchKey = SET:100:COMP:BANSSANG:RC:0:MARK:NONE
        when(watchTargetRepository.existsByUserIdAndWatchKey(1L,
                "SET:100:COMP:BANSSANG:RC:0:MARK:NONE")).thenReturn(false);

        UserWatchTarget saved = spy(UserWatchTarget.builder()
                .user(user).targetType(WatchTargetType.SET)
                .watchKey("SET:100:COMP:BANSSANG:RC:0:MARK:NONE")
                .equipmentSet(equipmentSet)
                .composition(SetComposition.BANSSANG).build());
        doReturn(2L).when(saved).getId();
        when(watchTargetRepository.save(any())).thenReturn(saved);

        watchlistService.add(1L,
                new WatchTargetAddRequest(WatchTargetType.SET, null, "<개양>", 100L, SetComposition.BANSSANG, 3));

        verify(watchTargetRepository).existsByUserIdAndWatchKey(1L,
                "SET:100:COMP:BANSSANG:RC:0:MARK:NONE");
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("remove_타인항목삭제_403예외")
    void remove_타인항목_403() {
        UserWatchTarget target = spy(UserWatchTarget.builder()
                .user(user).targetType(WatchTargetType.ITEM)
                .watchKey("ITEM:10").item(materialItem).build());
        doReturn(99L).when(target).getId();

        User other = spy(User.builder()
                .oauthProvider("google").oauthId("g2")
                .nickname("타인").email("o@t.com")
                .role(Role.USER).status(UserStatus.ACTIVE)
                .build());
        doReturn(99L).when(other).getId();
        // target의 소유자는 user(id=1), 요청자는 other(id=99)
        UserWatchTarget foreignTarget = spy(UserWatchTarget.builder()
                .user(other).targetType(WatchTargetType.ITEM)
                .watchKey("ITEM:10").item(materialItem).build());
        doReturn(5L).when(foreignTarget).getId();

        when(watchTargetRepository.findById(5L)).thenReturn(Optional.of(foreignTarget));

        assertThatThrownBy(() -> watchlistService.remove(5L, 1L))
                .isInstanceOf(WatchlistException.class)
                .extracting("errorCode").isEqualTo("WATCH_FORBIDDEN");
    }

    @Test
    @DisplayName("remove_본인항목_정상삭제")
    void remove_본인항목_삭제() {
        UserWatchTarget target = spy(UserWatchTarget.builder()
                .user(user).targetType(WatchTargetType.ITEM)
                .watchKey("ITEM:10").item(materialItem).build());
        doReturn(1L).when(target).getId();
        // target.getUser().getId() = 1L (user.getId())
        when(watchTargetRepository.findById(1L)).thenReturn(Optional.of(target));

        watchlistService.remove(1L, 1L);

        verify(watchTargetRepository).delete(target);
    }
}
