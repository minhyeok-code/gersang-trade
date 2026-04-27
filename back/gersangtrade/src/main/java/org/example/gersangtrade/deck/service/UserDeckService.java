package org.example.gersangtrade.deck.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberRepository;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.example.gersangtrade.domain.deck.UserDeck;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.deck.UserDeckMemberCharacteristic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 유저 덱 서비스.
 * 덱 저장 시 합산 스탯(attrXValue, totalResDown)을 계산해 캐싱한다.
 */
@Service
@RequiredArgsConstructor
public class UserDeckService {

    private final UserDeckRepository userDeckRepository;
    private final UserDeckMemberRepository userDeckMemberRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final MercenaryCharacteristicLevelRepository characteristicLevelRepository;

    /**
     * 덱의 합산 스탯을 계산하고 UserDeck에 캐싱한다.
     *
     * <p>합산 흐름 (슬롯별):
     * <ol>
     *   <li>MercenaryStat → RESIST_PIERCE, ELEMENT_VALUE 기본값 합산</li>
     *   <li>UserDeckMemberCharacteristic → 선택 레벨의 effectValue(amountValue) 합산</li>
     * </ol>
     * statType이 null인 특성 레벨은 skip된다 (관리자 수동 보정 전).
     */
    @Transactional
    public void calculateTotalStats(Long deckId) {
        UserDeck deck = userDeckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("덱을 찾을 수 없습니다: " + deckId));

        List<UserDeckMember> members = userDeckMemberRepository.findByDeckIdWithMercenary(deckId);

        int totalResDown = 0;
        int attrXValue = 0;

        for (UserDeckMember member : members) {
            Long mercenaryId = member.getMercenary().getId();

            // 1. 용병 기본 스탯 합산
            List<MercenaryStat> stats = mercenaryStatRepository.findByMercenaryId(mercenaryId);
            for (MercenaryStat stat : stats) {
                if (stat.getStatKey() == StatType.RESIST_PIERCE) {
                    totalResDown += stat.getStatValue();
                } else if (stat.getStatKey() == StatType.ELEMENT_VALUE) {
                    attrXValue += stat.getStatValue();
                }
            }

            // 2. 선택 특성 레벨 합산
            // UserDeckMemberCharacteristic은 lazy 로딩이므로 직접 조회하지 않고
            // member에서 접근한다 (이 서비스 메서드 내에서는 트랜잭션 활성 상태)
        }

        deck.applyStats(attrXValue, totalResDown);
    }

    /**
     * 슬롯 멤버의 선택 특성 레벨 합산을 수행한다.
     * calculateTotalStats 내부에서 각 멤버별로 호출된다.
     *
     * @param characteristics 슬롯에 선택된 특성 목록
     * @param totalResDown    현재까지 누적된 저항깎 합산 (가변 참조용)
     * @param attrXValue      현재까지 누적된 속성값 합산 (가변 참조용)
     * @return [totalResDown 증가분, attrXValue 증가분]
     */
    public int[] sumCharacteristicStats(List<UserDeckMemberCharacteristic> characteristics) {
        int resDown = 0;
        int attrX = 0;

        for (UserDeckMemberCharacteristic selected : characteristics) {
            Long charId = selected.getCharacteristic().getId();
            Integer selectedLevel = selected.getSelectedLevel();

            // 해당 특성의 label별 선택 레벨 수치 조회
            List<MercenaryCharacteristicLevel> levels =
                    characteristicLevelRepository.findByCharacteristicId(charId);

            for (MercenaryCharacteristicLevel lvl : levels) {
                // 선택 레벨에 해당하는 행만 합산
                if (!lvl.getLevel().equals(selectedLevel)) continue;
                // statType 미매핑(null)이면 skip — 관리자 수동 보정 전
                if (lvl.getStatType() == null || lvl.getAmountValue() == null) continue;

                if (lvl.getStatType() == StatType.RESIST_PIERCE) {
                    resDown += lvl.getAmountValue().intValue();
                } else if (lvl.getStatType() == StatType.ELEMENT_VALUE) {
                    attrX += lvl.getAmountValue().intValue();
                }
            }
        }

        return new int[]{resDown, attrX};
    }
}
