package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 명왕 전용 무기의 잘못된 SELF scope 속성값 스탯 정리.
 *
 * <p>명왕 무기 속성값은 착용자 본인(SELF)이 아니라 사천왕(ALLY_HEAVENLY_KING) 또는
 * 동속아군 %(ALLY_SAME_ELEMENT)으로 적재되어야 한다. 초기 크롤 오적재 행을 삭제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyeongwangWeaponStatCleanupService {

    private final ItemStatRepository itemStatRepository;

    public record CleanupResult(int deletedCount, List<String> itemNames) {}

    /**
     * 명왕 무기(WEAPON)의 ELEMENT_VALUE + scope=SELF 행을 삭제한다.
     *
     * @return 삭제 건수 및 대상 아이템명 목록
     */
    @Transactional
    public CleanupResult removeSelfElementValueStats() {
        List<ItemStat> targets = itemStatRepository.findMyeongwangWeaponSelfElementValueStats();
        if (targets.isEmpty()) {
            return new CleanupResult(0, List.of());
        }

        List<String> itemNames = new ArrayList<>();
        for (ItemStat stat : targets) {
            String name = stat.getItem().getName();
            itemStatRepository.delete(stat);
            if (!itemNames.contains(name)) {
                itemNames.add(name);
            }
            log.info("[MyeongwangWeaponStatCleanup] 삭제: {} ELEMENT_VALUE element={} scope=SELF value={}",
                    name, stat.getElement(), stat.getValue());
        }
        return new CleanupResult(targets.size(), itemNames);
    }
}
