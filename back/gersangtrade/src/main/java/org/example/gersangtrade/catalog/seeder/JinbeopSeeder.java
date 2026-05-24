package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.DeckBuffSourceRepository;
import org.example.gersangtrade.domain.catalog.DeckBuff;
import org.example.gersangtrade.domain.catalog.DeckBuffSource;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.DeckBuffSourceType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 진법 40개 초기 데이터 시딩.
 * 바람/뇌/물/불 4속성 × 1~10강. 1강마다 속성값 1 상승.
 * sourceId 규칙: 바람 1~10, 뇌 11~20, 물 21~30, 불 31~40.
 */
@Slf4j
@Component
@Order(11)
@RequiredArgsConstructor
public class JinbeopSeeder implements ApplicationRunner {

    private final DeckBuffSourceRepository deckBuffSourceRepository;

    private record Formation(String name, Element element, long sourceIdBase) {}

    private static final Formation[] FORMATIONS = {
            new Formation("바람의진법", Element.WIND,     1),
            new Formation("뇌의진법",   Element.THUNDER, 11),
            new Formation("물의진법",   Element.WATER,   21),
            new Formation("불의진법",   Element.FIRE,    31),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!deckBuffSourceRepository.findBySourceType(DeckBuffSourceType.JINBEOP).isEmpty()) {
            log.debug("진법 시딩 skip: 이미 존재");
            return;
        }

        log.info("진법 시딩 시작");
        for (Formation f : FORMATIONS) {
            for (int level = 1; level <= 10; level++) {
                DeckBuffSource source = DeckBuffSource.builder()
                        .sourceType(DeckBuffSourceType.JINBEOP)
                        .sourceId(f.sourceIdBase() + level - 1)
                        .name(f.name() + " " + level + "강")
                        .build();
                source.getBuffs().add(DeckBuff.builder()
                        .source(source)
                        .statType(StatType.ELEMENT_VALUE)
                        .element(f.element())
                        .valueType(BuffValueType.FLAT)
                        .value(level)
                        .target(BuffTarget.ALLY)
                        .build());
                deckBuffSourceRepository.save(source);
            }
        }
        log.info("진법 시딩 완료 (40개)");
    }
}
