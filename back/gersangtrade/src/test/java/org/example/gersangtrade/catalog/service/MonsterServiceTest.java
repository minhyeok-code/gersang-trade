package org.example.gersangtrade.catalog.service;

import org.example.gersangtrade.catalog.dto.response.MonsterResponse;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * MonsterService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonsterService")
class MonsterServiceTest {

    @Mock
    private MonsterRepository monsterRepository;

    @InjectMocks
    private MonsterService monsterService;

    private Monster waterMonster;
    private Monster fireMonster;

    @BeforeEach
    void setUp() {
        waterMonster = Monster.builder()
                .name("연못거북(水) (9급)")
                .pageUrl("https://www.gersangjjang.com/monster/test.asp")
                .hp(null)
                .hittingResistance(290)
                .magicResistance(290)
                .elementValue(20)
                .element(Element.WATER)
                .build();

        fireMonster = Monster.builder()
                .name("불꽃도깨비 (火) (7급)")
                .pageUrl("https://www.gersangjjang.com/monster/fire.asp")
                .hp(null)
                .hittingResistance(310)
                .magicResistance(280)
                .elementValue(15)
                .element(Element.FIRE)
                .build();
    }

    // ── getMonsters ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMonsters")
    class GetMonsters {

        @Test
        @DisplayName("element 없으면 전체 목록 반환")
        void element없으면_전체반환() {
            given(monsterRepository.findAll()).willReturn(List.of(waterMonster, fireMonster));

            List<MonsterResponse> result = monsterService.getMonsters(null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("element 필터 적용 시 해당 속성만 반환")
        void element필터_해당속성반환() {
            given(monsterRepository.findByElement(Element.WATER)).willReturn(List.of(waterMonster));

            List<MonsterResponse> result = monsterService.getMonsters(Element.WATER);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).element()).isEqualTo(Element.WATER);
            assertThat(result.get(0).name()).isEqualTo("연못거북(水) (9급)");
        }

        @Test
        @DisplayName("결과 없으면 빈 목록")
        void 결과없으면_빈목록() {
            given(monsterRepository.findByElement(Element.THUNDER)).willReturn(List.of());

            List<MonsterResponse> result = monsterService.getMonsters(Element.THUNDER);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("DTO 변환 — hp·저항·속성값 정확히 매핑")
        void DTO변환_정확히매핑() {
            given(monsterRepository.findAll()).willReturn(List.of(waterMonster));

            MonsterResponse response = monsterService.getMonsters(null).get(0);

            assertThat(response.hp()).isNull();
            assertThat(response.hittingResistance()).isEqualTo(290);
            assertThat(response.magicResistance()).isEqualTo(290);
            assertThat(response.elementValue()).isEqualTo(20);
            assertThat(response.element()).isEqualTo(Element.WATER);
        }
    }

    // ── getMonster ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMonster")
    class GetMonster {

        @Test
        @DisplayName("존재하는 ID — 단건 반환")
        void 존재하는ID_단건반환() {
            given(monsterRepository.findById(1L)).willReturn(Optional.of(waterMonster));

            MonsterResponse response = monsterService.getMonster(1L);

            assertThat(response.name()).isEqualTo("연못거북(水) (9급)");
        }

        @Test
        @DisplayName("존재하지 않는 ID — NoSuchElementException")
        void 존재않는ID_예외() {
            given(monsterRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> monsterService.getMonster(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("존재하지 않는 몬스터");
        }
    }
}
