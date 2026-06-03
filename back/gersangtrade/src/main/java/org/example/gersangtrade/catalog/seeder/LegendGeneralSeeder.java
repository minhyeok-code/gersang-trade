package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.LegendGeneralCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.LegendGeneralPassiveRepository;
import org.example.gersangtrade.catalog.repository.LegendGeneralRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenarySkillRepository;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenarySkill;
import org.example.gersangtrade.domain.catalog.CharacteristicEffect;
import org.example.gersangtrade.domain.catalog.LegendGeneral;
import org.example.gersangtrade.domain.catalog.LegendGeneralCharacteristic;
import org.example.gersangtrade.domain.catalog.LegendGeneralPassive;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.enums.BuffTarget;
import org.example.gersangtrade.domain.catalog.enums.BuffValueType;
import org.example.gersangtrade.domain.catalog.enums.CharacteristicApplyType;
import org.example.gersangtrade.domain.catalog.enums.Element;
import org.example.gersangtrade.domain.catalog.enums.LegendGeneralType;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.MercenaryType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전설장수 15마리 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행. 이미 존재하면 건너뛴다.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class LegendGeneralSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final LegendGeneralRepository legendGeneralRepository;
    private final LegendGeneralPassiveRepository passiveRepository;
    private final LegendGeneralCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicRepository mercenaryCharacteristicRepository;
    private final MercenarySkillRepository skillRepository;

    /** 시딩 실행 중에만 유효한 임시 상태 — run() 시작 시점에 패시브가 있는 LG ID 집합 */
    private Set<Long> existingPassiveLgIds = new HashSet<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 속성 정정·스텁 보장·데이터 정정은 시딩 여부와 무관하게 항상 실행 (기존 DB에도 적용)
        correctNatures();
        ensureCharacteristicStubs();
        correctBajiraoCharacteristics();

        long passiveCount = passiveRepository.count();
        long charCount = characteristicRepository.count();
        log.info("전설장수 시딩 상태 — passives={}, characteristics={}", passiveCount, charCount);

        // 패시브·특성 둘 다 존재할 때만 skip (패시브만 있고 특성이 없는 부분 시딩 상태도 처리)
        if (passiveCount > 0 && charCount > 0) {
            log.info("전설장수 시딩 skip: 이미 존재");
            return;
        }

        // 시딩 시작 전 기존 패시브 보유 LG ID 캡처 — addPassive에서 재저장 방지
        existingPassiveLgIds = passiveRepository.findAll().stream()
                .map(p -> p.getLegendGeneral().getId())
                .collect(Collectors.toSet());

        log.info("전설장수 시딩 시작");
        // 타입 A
        seedJumong();
        seedMaenghwak();
        seedNobutsuna();
        seedBajirao();
        seedChoseon();
        // 타입 B
        seedBokuten();
        seedAkbareu();
        seedHonggildong();
        seedYeopo();
        seedChiyome();
        seedRejina();
        seedHwamokran();
        seedManseonya();
        seedMajo();
        seedChoimuseon();
        log.info("전설장수 시딩 완료 (15마리)");
    }

    // ── 타입 A ─────────────────────────────────────────────────────────────────

    private void seedJumong() {
        LegendGeneral lg = saveLg("joomong", "주몽", Nature.THUNDER, LegendGeneralType.TYPE_A);
        addSkill(lg.getMercenary(), "뇌전탄", "shofjstks");

        addPassive(lg, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                100, 10f, 2, 1f, null);

        // 특성 0: 폭뢰 — 마법저항 ALLY
        float[] c0 = {1, 2, 3, 4, 5, 6, 8, 10, 12, 15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "폭뢰", lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 고취 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "고취", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "폭뢰");
        addCharacteristicStub(lg, 1, "고취");
    }

    private void seedMaenghwak() {
        LegendGeneral lg = saveLg("maenghwaek", "맹획", Nature.WIND, LegendGeneralType.TYPE_A);
        addSkill(lg.getMercenary(), "야수돌진", "dktnehfwls");

        addPassive(lg, StatType.HITTING_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                100, 10f, 2, 1f, null);

        // 특성 0: 돌입 — 타격저항 ALLY
        float[] c0 = {1, 2, 3, 4, 5, 6, 8, 10, 12, 15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "돌입", lvl);
            addEffect(row, StatType.HITTING_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 고무 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "고무", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "돌입");
        addCharacteristicStub(lg, 1, "고무");
    }

    private void seedNobutsuna() {
        LegendGeneral lg = saveLg("nobootsuna", "노부츠나", Nature.FIRE, LegendGeneralType.TYPE_A);
        addSkill(lg.getMercenary(), "습격", "tbrwjr");

        // 미확정 — null 저장
        addPassive(lg, StatType.MIN_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                100, null, null, null, null);
        addPassive(lg, StatType.MAX_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                100, null, null, null, null);

        // 특성 0: 격렬 — MIN_POWER + MAX_POWER ALLY
        float[] c0 = {50, 100, 150, 200, 250, 300, 350, 400, 450, 500};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "격렬", lvl);
            addEffect(row, StatType.MIN_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY, c0[lvl-1]);
            addEffect(row, StatType.MAX_POWER, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 기습 — 스킬 데미지 + 크리티컬확률 SELF
        float[] sdPct = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] critPct = {4, 8, 12, 16, 20, 24, 28, 32, 36, 40};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "기습", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, sdPct[lvl-1]);
            addEffect(row, StatType.CRITICAL_CHANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, critPct[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "격렬");
        addCharacteristicStub(lg, 1, "기습");
    }

    private void seedBajirao() {
        LegendGeneral lg = saveLg("bajirao", "바지라오", Nature.EARTH, LegendGeneralType.TYPE_A);
        addSkill(lg.getMercenary(), "광기의칼날", "rhkdrldlekfsk");

        addPassive(lg, StatType.DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY,
                200, 1.0f, 10, 0.5f, null);

        // 특성 0: 고양 — DAMAGE_PERCENT NONE ALLY
        float[] c0 = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 6.0f};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "고양", lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 광분 — 스킬 데미지 SELF + 지속성 데미지증가(EARTH) ALLY
        float[] c1skill = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] c1earth = {1, 1, 1, 2, 2, 3, 3, 4, 4, 5};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "광분", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE,  BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1skill[lvl-1]);
            addEffect(row, StatType.DAMAGE_PERCENT,       Element.EARTH, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c1earth[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "고양");
        addCharacteristicStub(lg, 1, "광분");
    }

    private void seedChoseon() {
        LegendGeneral lg = saveLg("chosun", "초선", Nature.WATER, LegendGeneralType.TYPE_A);
        addSkill(lg.getMercenary(), "빙화", "dlsghk");

        // 패시브: 마력회복 — 계산기 대상 아님, 저장만
        addPassive(lg, StatType.MP_RECOVERY, Element.NONE, BuffValueType.FLAT, BuffTarget.ALLY,
                100, 10f, 2, 1f, null);

        // 특성 0: 결빙 — 마력회복 관련, 계산기 제외 (행 없음)

        // 특성 1: 지능 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "지능", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "결빙");
        addCharacteristicStub(lg, 1, "지능");
    }

    // ── 타입 B ─────────────────────────────────────────────────────────────────

    private void seedBokuten() {
        LegendGeneral lg = saveLg("bokuten", "보쿠텐", Nature.WIND, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "신토류", "tlsxhfdn");

        // 특성 0: 발도 — 크리티컬확률 ALLY
        float[] c0 = {1, 2, 2, 3, 3, 4, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "발도", lvl);
            addEffect(row, StatType.CRITICAL_CHANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 연마 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "연마", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "발도");
        addCharacteristicStub(lg, 1, "연마");
    }

    private void seedAkbareu() {
        LegendGeneral lg = saveLg("akbar", "악바르", Nature.EARTH, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "모래병사 소환", "qhfowdydtk thghks");

        // 특성 0: 사풍 — 속성값 ADAPTIVE ENEMY (음수)
        float[] c0 = {-1, -1, -2, -2, -2, -3, -3, -4, -4, -5};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "사풍", lvl);
            addEffect(row, StatType.ELEMENT_VALUE, Element.ADAPTIVE, BuffValueType.FLAT, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 부호 — 속성값 ADAPTIVE ALLY (양수)
        float[] c1 = {1, 1, 2, 2, 2, 3, 3, 4, 4, 5};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "부호", lvl);
            addEffect(row, StatType.ELEMENT_VALUE, Element.ADAPTIVE, BuffValueType.FLAT, BuffTarget.ALLY, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "사풍");
        addCharacteristicStub(lg, 1, "부호");
    }

    private void seedHonggildong() {
        LegendGeneral lg = saveLg("honggildong", "홍길동", Nature.WIND, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "용오름 소환", "dhdghfmq thghks");
        addSkill(lg.getMercenary(), "주인의 영역", "wndldml dudrur");

        // 특성 0: 풍주 — 데미지증가 WIND ALLY
        float[] c0 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "풍주", lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.WIND, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 용오름 — 스킬 데미지 SELF + 타격저항 ENEMY (음수)
        float[] sd  = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] hit = {-1, -1, -2, -2, -3, -3, -5, -5, -7, -10};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "용오름", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF,  sd[lvl-1]);
            addEffect(row, StatType.HITTING_RESISTANCE,   Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, hit[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "풍주");
        addCharacteristicStub(lg, 1, "용오름");
    }

    private void seedYeopo() {
        LegendGeneral lg = saveLg("yeopo", "여포", Nature.FIRE, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "흑염격노", "gigdurwjrsh");
        addSkill(lg.getMercenary(), "무력지대", "qhfrurdlem");

        // 특성 0: 화주 — 데미지증가 FIRE ALLY
        float[] c0 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "화주", lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.FIRE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 무쌍 — 스킬 데미지 SELF + 마법저항 ENEMY (음수)
        float[] sd  = {10, 20, 30, 40, 50,  60,  70,  80,  90, 100};
        float[] mag = {-1, -2, -3, -4, -5,  -6,  -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "무쌍", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF,  sd[lvl-1]);
            addEffect(row, StatType.MAGIC_RESISTANCE,     Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, mag[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "화주");
        addCharacteristicStub(lg, 1, "무쌍");
    }

    private void seedChiyome() {
        LegendGeneral lg = saveLg("mochizki-chiyome", "치요메", Nature.WATER, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "호령진언", "ghfrurdwlsehs");
        addSkill(lg.getMercenary(), "저혼주문", "wjrghnwnjqs");

        // 특성 0: 현성 — 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "현성", lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 수주 — 데미지증가 WATER ALLY
        float[] c1 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "수주", lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.WATER, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "현성");
        addCharacteristicStub(lg, 1, "수주");
    }

    private void seedRejina() {
        LegendGeneral lg = saveLg("fpwlsktnfxksk", "레지나", Nature.EARTH, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "대지포식", "eowlvhdtlr");
        addSkill(lg.getMercenary(), "포식", "vhdtlr");

        // 특성 0: 지주 — 지상데미지증가 + 공중데미지증가 ALLY
        float[] ground = {1, 1, 2, 2, 3,  3,  4,  5,  6,  7};
        float[] air    = {1, 2, 3, 4, 5,  6,  7,  8,  9, 10};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "지주", lvl);
            addEffect(row, StatType.DAMAGE_PERCENT_GROUND, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, ground[lvl-1]);
            addEffect(row, StatType.DAMAGE_PERCENT_AIR,    Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, air[lvl-1]);
        }

        // 특성 1: 속사 — 스킬 데미지 SELF + 공격속도 ALLY
        float[] sd     = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        float[] aspeed = { 2,  3,  4,  5,  7, 12, 15, 30, 40,  50};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "속사", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, sd[lvl-1]);
            addEffect(row, StatType.ATTACK_SPEED,         Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, aspeed[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "지주");
        addCharacteristicStub(lg, 1, "속사");
    }

    private void seedHwamokran() {
        LegendGeneral lg = saveLg("hwamokran", "화목란", Nature.THUNDER, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "우뢰폭발", "dnfhlvhrqkf");
        addSkill(lg.getMercenary(), "우뢰탄", "dnfhltks");

        // 특성 0: 집중 — 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "집중", lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 결의 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "결의", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "집중");
        addCharacteristicStub(lg, 1, "결의");
    }

    private void seedManseonya() {
        LegendGeneral lg = saveLg("tjsdlsakstjsdi", "만선야", Nature.THUNDER, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "용의분노", "dhdrdlqssh");

        // 특성 0: 뇌주 — 데미지증가 THUNDER ALLY
        float[] c0 = {1, 1, 2, 2, 3, 3, 4, 5, 6, 7};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "뇌주", lvl);
            addEffect(row, StatType.DAMAGE_PERCENT, Element.THUNDER, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
        }

        // 특성 1: 분노 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "분노", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "뇌주");
        addCharacteristicStub(lg, 1, "분노");
    }

    private void seedMajo() {
        LegendGeneral lg = saveLg("majo", "마조", Nature.WATER, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "팔괘진", "vkfrhwlwls");

        // 특성 0: 혼란 — 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "혼란", lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 가호 — 스킬 데미지 SELF
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 90, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "가호", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "혼란");
        addCharacteristicStub(lg, 1, "가호");
    }

    private void seedChoimuseon() {
        LegendGeneral lg = saveLg("choimoosun", "최무선", Nature.FIRE, LegendGeneralType.TYPE_B);
        addSkill(lg.getMercenary(), "비격진천뢰", "dlrwjrwlscjsfhl");

        // 특성 0: 포화 — 마법저항 ENEMY (음수)
        float[] c0 = {-1, -2, -3, -4, -5, -6, -8, -10, -12, -15};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 0, "포화", lvl);
            addEffect(row, StatType.MAGIC_RESISTANCE, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ENEMY, c0[lvl-1]);
        }

        // 특성 1: 집중 — 스킬 데미지 SELF (레벨 8 값이 95로 다름)
        float[] c1 = {10, 20, 30, 40, 50, 60, 75, 95, 120, 150};
        for (int lvl = 1; lvl <= 10; lvl++) {
            LegendGeneralCharacteristic row = buildChar(lg, 1, "집중", lvl);
            addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1[lvl-1]);
        }
        addCharacteristicStub(lg, 0, "포화");
        addCharacteristicStub(lg, 1, "집중");
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    /**
     * MercenaryCharacteristic 스텁이 없는 전설장수에만 스텁을 생성한다.
     * 시딩 여부와 무관하게 항상 실행 — 기존 DB에 스텁이 누락된 경우에도 보완된다.
     */
    private void ensureCharacteristicStubs() {
        record LgStub(String key, String char0, String char1) {}
        var stubs = List.of(
                new LgStub("joomong",          "폭뢰",   "고취"),
                new LgStub("maenghwaek",       "돌입",   "고무"),
                new LgStub("nobootsuna",       "격렬",   "기습"),
                new LgStub("bajirao",          "고양",   "광분"),
                new LgStub("chosun",           "결빙",   "지능"),
                new LgStub("bokuten",          "발도",   "연마"),
                new LgStub("akbar",            "사풍",   "부호"),
                new LgStub("honggildong",      "풍주",   "용오름"),
                new LgStub("yeopo",            "화주",   "무쌍"),
                new LgStub("mochizki-chiyome", "현성",   "수주"),
                new LgStub("fpwlsktnfxksk",    "지주",   "속사"),
                new LgStub("hwamokran",        "집중",   "결의"),
                new LgStub("tjsdlsakstjsdi",   "뇌주",   "분노"),
                new LgStub("majo",             "혼란",   "가호"),
                new LgStub("choimoosun",       "포화",   "집중")
        );
        for (var s : stubs) {
            mercenaryRepository.findByKey(s.key()).ifPresent(mercenary -> {
                createStubIfAbsent(mercenary, 0, s.char0());
                createStubIfAbsent(mercenary, 1, s.char1());
            });
        }
    }

    /**
     * 바지라오 특성 데이터 정정: 광분(char1)에 DAMAGE_PERCENT EARTH ALLY 효과가 없으면
     * 기존 LGC 행을 전부 삭제하고 올바른 값으로 재삽입한다.
     */
    private void correctBajiraoCharacteristics() {
        mercenaryRepository.findByKey("bajirao").ifPresent(mercenary ->
                legendGeneralRepository.findByMercenaryId(mercenary.getId()).ifPresent(lg -> {
                    boolean needsCorrection = characteristicRepository
                            .findWithEffectsByLegendGeneralId(lg.getId()).stream()
                            .filter(c -> c.getCharacteristicIndex() == 1)
                            .flatMap(c -> c.getEffects().stream())
                            .noneMatch(e -> e.getStatType() == StatType.DAMAGE_PERCENT
                                    && e.getElement() == Element.EARTH);
                    if (!needsCorrection) return;

                    log.info("바지라오 특성 데이터 정정 시작");
                    characteristicRepository.deleteByLegendGeneralId(lg.getId());
                    characteristicRepository.flush();

                    float[] c0 = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 6.0f};
                    for (int lvl = 1; lvl <= 10; lvl++) {
                        LegendGeneralCharacteristic row = buildChar(lg, 0, "고양", lvl);
                        addEffect(row, StatType.DAMAGE_PERCENT, Element.NONE, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c0[lvl-1]);
                    }

                    float[] c1skill = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
                    float[] c1earth = {1, 1, 1, 2, 2, 3, 3, 4, 4, 5};
                    for (int lvl = 1; lvl <= 10; lvl++) {
                        LegendGeneralCharacteristic row = buildChar(lg, 1, "광분", lvl);
                        addEffect(row, StatType.SKILL_DAMAGE_PERCENT, Element.NONE,  BuffValueType.PERCENT_ADD, BuffTarget.SELF, c1skill[lvl-1]);
                        addEffect(row, StatType.DAMAGE_PERCENT,       Element.EARTH, BuffValueType.PERCENT_ADD, BuffTarget.ALLY, c1earth[lvl-1]);
                    }
                    log.info("바지라오 특성 데이터 정정 완료");
                })
        );
    }

    private void createStubIfAbsent(Mercenary mercenary, int index, String name) {
        String key = mercenary.getKey() + "_char" + index;
        if (mercenaryCharacteristicRepository.findByKey(key).isPresent()) return;
        log.info("전설장수 특성 스텁 생성: {}", key);
        mercenaryCharacteristicRepository.save(MercenaryCharacteristic.builder()
                .mercenary(mercenary)
                .key(key)
                .name(name)
                .point(null)
                .applyType(CharacteristicApplyType.NORMAL)
                .build());
    }

    /** 잘못 저장된 전설장수 속성을 정정한다. 시딩 완료 여부와 무관하게 항상 실행된다. */
    private void correctNatures() {
        record KeyNature(String key, Nature nature) {}
        var corrections = List.of(
                new KeyNature("joomong",          Nature.THUNDER),
                new KeyNature("maenghwaek",       Nature.WIND),
                new KeyNature("nobootsuna",       Nature.FIRE),
                new KeyNature("bajirao",          Nature.EARTH),
                new KeyNature("chosun",           Nature.WATER),
                new KeyNature("bokuten",          Nature.WIND),
                new KeyNature("akbar",            Nature.EARTH),
                new KeyNature("honggildong",      Nature.WIND),
                new KeyNature("yeopo",            Nature.FIRE),
                new KeyNature("mochizki-chiyome", Nature.WATER),
                new KeyNature("fpwlsktnfxksk",    Nature.EARTH),
                new KeyNature("hwamokran",        Nature.THUNDER),
                new KeyNature("tjsdlsakstjsdi",   Nature.THUNDER),
                new KeyNature("majo",             Nature.WATER),
                new KeyNature("choimoosun",       Nature.FIRE)
        );
        for (var kn : corrections) {
            mercenaryRepository.findByKey(kn.key()).ifPresent(m -> {
                if (m.getNature() != kn.nature()) {
                    log.info("전설장수 속성 정정: {} {} → {}", kn.key(), m.getNature(), kn.nature());
                    m.updateInfo(null, null, null, kn.nature(), m.getNatureValue(), m.isComingSoon());
                }
            });
        }
    }

    private LegendGeneral saveLg(String key, String name, Nature nature, LegendGeneralType type) {
        // 기존 용병이 있으면 재사용, 없으면 새로 생성
        Mercenary mercenary = mercenaryRepository.findByName(name)
                .orElseGet(() -> mercenaryRepository.save(
                        Mercenary.builder()
                                .key(key)
                                .name(name)
                                .nature(nature)
                                .mercenaryType(MercenaryType.LEGEND_GENERAL)
                                .category(MercenaryCategory.LEGENDARY_GENERAL)
                                .comingSoon(false)
                                .build()
                ));
        // 기존 LG가 있으면 재사용, 없으면 새로 생성 (중복 LG 생성 방지)
        return legendGeneralRepository.findByMercenaryId(mercenary.getId())
                .orElseGet(() -> legendGeneralRepository.save(
                        LegendGeneral.builder()
                                .mercenary(mercenary)
                                .type(type)
                                .build()
                ));
    }

    /** MercenaryCharacteristic 스텁을 생성한다 — 내부적으로 createStubIfAbsent를 위임한다. */
    private void addCharacteristicStub(LegendGeneral lg, int index, String name) {
        createStubIfAbsent(lg.getMercenary(), index, name);
    }

    // saveChar는 저장하지 않고 빌드만 한다. addEffect 이후 save 호출.
    private LegendGeneralCharacteristic buildChar(LegendGeneral lg, int charIndex, String name, int level) {
        return LegendGeneralCharacteristic.builder()
                .legendGeneral(lg)
                .characteristicIndex(charIndex)
                .name(name)
                .level(level)
                .build();
    }

    private void addSkill(Mercenary mercenary, String skillName, String skillKey) {
        if (!skillRepository.existsByMercenaryIdAndSkillName(mercenary.getId(), skillName)) {
            skillRepository.save(MercenarySkill.builder()
                    .mercenary(mercenary)
                    .skillName(skillName)
                    .skillKey(skillKey)
                    .build());
        }
    }

    private void addPassive(LegendGeneral lg, StatType statType, Element element,
                             BuffValueType valueType, BuffTarget target,
                             Integer startLevel, Float startValue,
                             Integer incrementPerLevels, Float incrementValue, Float maxValue) {
        // 시딩 시작 시점에 이미 패시브가 있던 LG는 재저장하지 않는다
        if (existingPassiveLgIds.contains(lg.getId())) return;
        lg.getPassives().add(
                LegendGeneralPassive.builder()
                        .legendGeneral(lg)
                        .statType(statType)
                        .element(element)
                        .valueType(valueType)
                        .target(target)
                        .startLevel(startLevel)
                        .startValue(startValue)
                        .incrementPerLevels(incrementPerLevels)
                        .incrementValue(incrementValue)
                        .maxValue(maxValue)
                        .build()
        );
        // 패시브를 추가한 뒤 저장해야 cascade가 발동된다.
        legendGeneralRepository.save(lg);
    }

    // effects를 컬렉션에 추가한 뒤 저장해야 cascade가 발동된다.
    private void addEffect(LegendGeneralCharacteristic row, StatType statType, Element element,
                            BuffValueType valueType, BuffTarget target, float value) {
        row.getEffects().add(
                CharacteristicEffect.builder()
                        .characteristic(row)
                        .statType(statType)
                        .element(element)
                        .valueType(valueType)
                        .target(target)
                        .value(value)
                        .build()
        );
        characteristicRepository.save(row);
    }
}
