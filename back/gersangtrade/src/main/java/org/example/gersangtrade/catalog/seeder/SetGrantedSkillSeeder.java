package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.SetGrantedSkillRepository;
import org.example.gersangtrade.domain.catalog.SetGrantedSkill;
import org.example.gersangtrade.domain.catalog.enums.SkillBehaviorType;
import org.example.gersangtrade.domain.catalog.enums.StatSource;
import org.example.gersangtrade.domain.catalog.enums.TriggerSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 세트 부여 스킬 초기 데이터 시딩.
 * 전설장수 전용장비 10강 7종 세트효과 스킬을 등록한다.
 *
 * <p>SkillCoeffSeeder(MAX_VALUE)가 set_granted 타입 계수를 읽기 전에 실행되어야 한다.
 */
@Slf4j
@Component
@Order(13)
@RequiredArgsConstructor
public class SetGrantedSkillSeeder implements ApplicationRunner {

    private final SetGrantedSkillRepository setGrantedSkillRepository;

    private record SkillRow(String skillKey, String skillName, SkillBehaviorType behaviorType,
                            StatSource statSource, TriggerSource triggerSource,
                            Integer triggerEveryN, String triggerBaseSkillKey, String note) {}

    private static final List<SkillRow> SKILL_ROWS = List.of(
            // 홍길동 전용장비 10강 7종 → 분신. 트리거 기준: 용오름 소환(dyddhfmathghks).
            new SkillRow("qnstls", "분신",
                    SkillBehaviorType.TRIGGER, StatSource.SELF, TriggerSource.SELF,
                    null, "dyddhfmathghks",
                    "홍길동 전용장비 10강 7종 세트효과. triggerEveryN 미확정"),

            // 화목란 전용장비 10강 7종 → 뇌조돌격. 트리거 기준: 우뢰폭발(dnfhlvhrqkf).
            new SkillRow("noejo_dolgyeok", "뇌조돌격",
                    SkillBehaviorType.TRIGGER, StatSource.SELF, TriggerSource.SELF,
                    null, "dnfhlvhrqkf",
                    "화목란 전용장비 10강 7종 세트효과. triggerEveryN 미확정. 스킬 계수 별도 입력 필요")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int seeded = 0;
        for (SkillRow row : SKILL_ROWS) {
            if (setGrantedSkillRepository.findBySkillKey(row.skillKey()).isPresent()) continue;
            setGrantedSkillRepository.save(SetGrantedSkill.builder()
                    .skillKey(row.skillKey())
                    .skillName(row.skillName())
                    .skillBehaviorType(row.behaviorType())
                    .statSource(row.statSource())
                    .triggerSource(row.triggerSource())
                    .triggerEveryN(row.triggerEveryN())
                    .triggerBaseSkillKey(row.triggerBaseSkillKey())
                    .note(row.note())
                    .build());
            seeded++;
        }
        if (seeded > 0) log.info("세트 부여 스킬 시딩 완료 ({}건)", seeded);
        else log.debug("세트 부여 스킬 시딩 skip: 이미 존재");
    }
}
