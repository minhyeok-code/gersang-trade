package org.example.gersangtrade.calculator.service;

import org.example.gersangtrade.calculator.dto.request.ResistanceType;
import org.example.gersangtrade.calculator.overlay.LoadedMember;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.MercenaryType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.deck.UserDeckMember;

import java.util.List;

/**
 * 덱 구성에 따른 DPS 저항 종류 자동 판별.
 *
 * <p>사천왕·각성사천왕({@link MercenaryType#HEAVENLY_KING},
 * {@link MercenaryType#AWAKENED_HEAVENLY_KING}) 중 <strong>풍속성</strong>이 편성되어 있으면
 * 몬스터 <strong>타격저항</strong> 기준, 그 외에는 <strong>마법저항</strong> 기준으로 계산한다.
 */
public final class DeckResistanceTypeResolver {

    private DeckResistanceTypeResolver() {
    }

    /**
     * @param members 덱에 편성된 멤버 목록 (용병 fetch 포함)
     * @return 풍속성 사천왕·각성사천왕이 있으면 {@link ResistanceType#HITTING}, 아니면 {@link ResistanceType#MAGIC}
     */
    public static ResistanceType resolve(List<UserDeckMember> members) {
        boolean hasWindHeavenlyKing = members.stream()
                .map(UserDeckMember::getMercenary)
                .filter(DeckResistanceTypeResolver::isHeavenlyKing)
                .anyMatch(m -> m.getNature() == Nature.WIND);
        return hasWindHeavenlyKing ? ResistanceType.HITTING : ResistanceType.MAGIC;
    }

    /** {@link LoadedMember} 오버로드 — overlay 파이프라인용. */
    public static ResistanceType resolveLoaded(List<LoadedMember> members) {
        boolean hasWindHeavenlyKing = members.stream()
                .map(LoadedMember::mercenary)
                .filter(DeckResistanceTypeResolver::isHeavenlyKing)
                .anyMatch(m -> m.getNature() == Nature.WIND);
        return hasWindHeavenlyKing ? ResistanceType.HITTING : ResistanceType.MAGIC;
    }

    private static boolean isHeavenlyKing(Mercenary mercenary) {
        MercenaryType type = mercenary.getMercenaryType();
        return type == MercenaryType.HEAVENLY_KING || type == MercenaryType.AWAKENED_HEAVENLY_KING;
    }
}
