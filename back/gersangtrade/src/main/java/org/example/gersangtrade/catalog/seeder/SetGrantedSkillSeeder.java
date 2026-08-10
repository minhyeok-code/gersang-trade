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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
    public void run(ApplicationArguments args) {
        // 메서드 전체를 하나의 트랜잭션으로 묶지 않는다.
        // 각 save를 개별 커밋해, 동시 시딩 경쟁으로 한 건이 유니크 제약에 걸려도
        // 나머지 시딩이 롤백되지 않고 계속되도록 한다.
        int seeded = 0;
        for (SkillRow row : SKILL_ROWS) {
            // exists로 존재 확인 — 중복 데이터가 있어도 NonUniqueResult 예외가 나지 않는다.
            if (setGrantedSkillRepository.existsBySkillKey(row.skillKey())) continue;
            try {
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
            } catch (DataIntegrityViolationException e) {
                // 블루-그린 동시 기동 등 경쟁으로 다른 인스턴스가 먼저 삽입한 경우 — 무시하고 계속
                log.debug("세트 부여 스킬 이미 존재(동시 삽입): {}", row.skillKey());
            }
        }
        if (seeded > 0) log.info("세트 부여 스킬 시딩 완료 ({}건)", seeded);
        else log.debug("세트 부여 스킬 시딩 skip: 이미 존재");
    }
}
