package org.example.gersangtrade.calculator.overlay;

import org.example.gersangtrade.calculator.dto.request.BonusStatTarget;
import org.example.gersangtrade.hunt.dto.DeckSnapshotContent.CharacteristicSelection;

import java.util.List;

/**
 * 가성비 시나리오 overlay — merge 명세만 담는 불변 레코드.
 * 아이템 시나리오와 용병 시나리오는 상호 배타 (한 쪽만 non-null).
 * 가격(price)은 DpsEvaluationRequest 최상위 필드로 관리하며 overlay에 포함하지 않는다.
 * merge 로직은 {@link org.example.gersangtrade.calculator.service.DeckStateMerger}가 담당한다.
 */
public record DpsScenarioOverlay(
        ItemScenarioOverlay item,
        MercenaryScenarioOverlay mercenary
) {

    /** affectedMemberId 슬롯에 적용할 장비 변경 명세 */
    public record ItemScenarioOverlay(
            ScenarioItemType type,
            Long setId,
            Long affectedMemberId,
            List<ScenarioLine> lines
    ) {}

    /**
     * 용병 추가·교체 명세.
     * 가격은 DpsValueEvaluationService가 별도 관리 (overlay 범위 외).
     */
    public record MercenaryScenarioOverlay(
            MercenaryMode mode,
            Long mercenaryId,
            Long affectedMemberId,
            int level,
            BonusStatTarget bonusTarget,
            int bonusAmount,
            List<CharacteristicSelection> characteristics
    ) {}
}
