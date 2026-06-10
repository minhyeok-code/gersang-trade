package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.admin.service.MyeongwangWeaponStatCleanupService;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.ItemStatRepository;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.ItemStat;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ItemStat 데이터 correction.
 * 사인검의 ELEMENT_VALUE scope를 SELF → ALLY로 수정한다.
 * 사인검은 착용 주인공과 같은 속성을 가진 모든 아군 용병의 속성값을 올려주는 파티 버프 아이템이다.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class ItemStatCorrectionSeeder implements ApplicationRunner {

    private final ItemRepository itemRepository;
    private final ItemStatRepository itemStatRepository;
    private final MyeongwangWeaponStatCleanupService myeongwangWeaponStatCleanupService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        correctSainGumScope();
        myeongwangWeaponStatCleanupService.removeSelfElementValueStats();
    }

    /** 사인검 ELEMENT_VALUE: scope SELF → ALLY correction */
    private void correctSainGumScope() {
        List<Item> sainGumItems = itemRepository.findByNameContaining("사인검");
        if (sainGumItems.isEmpty()) return;

        for (Item item : sainGumItems) {
            List<ItemStat> stats = itemStatRepository.findByItemId(item.getId());
            for (ItemStat stat : stats) {
                if (stat.getStatType() != StatType.ELEMENT_VALUE) continue;
                if (stat.getScope() == BuffTarget.ALLY) continue; // 이미 수정됨

                // SELF → ALLY: 삭제 후 재생성
                itemStatRepository.delete(stat);
                itemStatRepository.save(ItemStat.builder()
                        .item(item)
                        .statType(stat.getStatType())
                        .element(stat.getElement())
                        .value(stat.getValue())
                        .statUnit(stat.getStatUnit())
                        .scope(BuffTarget.ALLY)
                        .build());
                log.info("[ItemStatCorrectionSeeder] 사인검 scope 수정: {} element={} SELF→ALLY",
                        item.getName(), stat.getElement());
            }
        }
    }
}
