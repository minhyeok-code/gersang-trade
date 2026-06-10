package org.example.gersangtrade.hunt.service;

import org.example.gersangtrade.domain.hunt.ClearTimeRecordStatus;
import org.example.gersangtrade.domain.user.UserClearTimeRepository;
import org.example.gersangtrade.hunt.config.HuntHubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("HuntHubAccessService")
class HuntHubAccessServiceTest {

    @Mock private UserClearTimeRepository clearTimeRepository;
    @Mock private HuntHubProperties huntHubProperties;

    @InjectMocks
    private HuntHubAccessService accessService;

    @BeforeEach
    void setUp() {
        given(huntHubProperties.getUnlockRequiredDistinctMonsters()).willReturn(3);
    }

    @Test
    @DisplayName("distinct 2 — 미해금")
    void twoMonsters_notUnlocked() {
        given(clearTimeRepository.countDistinctMonsterIdByUserIdAndStatus(1L, ClearTimeRecordStatus.ACTIVE))
                .willReturn(2L);
        assertThat(accessService.isUnlocked(1L)).isFalse();
    }

    @Test
    @DisplayName("distinct 3 — 해금")
    void threeMonsters_unlocked() {
        given(clearTimeRepository.countDistinctMonsterIdByUserIdAndStatus(1L, ClearTimeRecordStatus.ACTIVE))
                .willReturn(3L);
        assertThat(accessService.isUnlocked(1L)).isTrue();
        accessService.requireUnlocked(1L);
    }

    @Test
    @DisplayName("미해금 requireUnlocked — 403")
    void requireUnlocked_throws403() {
        given(clearTimeRepository.countDistinctMonsterIdByUserIdAndStatus(1L, ClearTimeRecordStatus.ACTIVE))
                .willReturn(1L);
        assertThatThrownBy(() -> accessService.requireUnlocked(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("3종");
    }
}
