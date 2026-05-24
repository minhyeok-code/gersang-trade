package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.CharacteristicApplyType;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.MercenaryType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 명왕 5마리 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행되며, 이미 존재하면 건너뛴다.
 * 크롤링 없음 — 게임 데이터 하드코딩.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class MyeongwangSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryRepository.findByName("항삼세명왕").isPresent()) {
            log.debug("명왕 시딩 skip: 이미 존재");
            return;
        }

        log.info("명왕 시딩 시작");
        seedHangsamse();
        seedGundari();
        seedDaewidok();
        seedBudong();
        seedGeumgangyacha();
        log.info("명왕 시딩 완료");
    }

    // ── 항삼세명왕 (FIRE) — STRENGTH 기본 이전율 10% ──────────────────────────

    private void seedHangsamse() {
        Mercenary m = upsertMercenary("항삼세명왕", "hangsamse", Nature.FIRE);

        // 특성1: 파괴 — 이전되는 힘
        MercenaryCharacteristic pakoe = upsertChar(m, "myeongwang-hangsamse-pakoe", "파괴");
        seedLevels(pakoe, "이전되는 힘",
                new float[]{1, 2, 3, 4, 5, 6, 8, 10, 12, 15}, StatType.STRENGTH);

        // 특성2: 화신
        MercenaryCharacteristic hwasin = upsertChar(m, "myeongwang-hangsamse-hwasin", "화신");
        seedLevels(hwasin, "힘, 생명력",
                new float[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, null);
        seedLevels(hwasin, "흡수 피해",
                new float[]{1, 2, 3, 4, 5, 6, 8, 10, 12, 15}, null);
    }

    // ── 군다리명왕 (THUNDER) — DEXTERITY 기본 이전율 10% ─────────────────────

    private void seedGundari() {
        Mercenary m = upsertMercenary("군다리명왕", "goondari", Nature.THUNDER);

        // 특성1: 신속 — 이전되는 민첩
        MercenaryCharacteristic sinsok = upsertChar(m, "myeongwang-gundari-sinsok", "신속");
        seedLevels(sinsok, "이전되는 민첩",
                new float[]{2, 4, 6, 8, 10, 12, 15, 18, 21, 25}, StatType.DEXTERITY);

        // 특성2: 우뢰
        MercenaryCharacteristic uroe = upsertChar(m, "myeongwang-gundari-uroe", "우뢰");
        seedLevels(uroe, "우뢰 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, null);
        seedLevelsSec(uroe, "유지시간",
                new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    }

    // ── 대위덕명왕 (WIND) — VITALITY 기본 이전율 5% ──────────────────────────

    private void seedDaewidok() {
        Mercenary m = upsertMercenary("대위덕명왕", "daewideok", Nature.WIND);

        // 특성1: 끈기 — 이전되는 생명력
        MercenaryCharacteristic kkeungi = upsertChar(m, "myeongwang-daewidok-kkeungi", "끈기");
        seedLevels(kkeungi, "이전되는 생명력",
                new float[]{1, 2, 3, 4, 5, 6, 8, 10, 12, 15}, StatType.VITALITY);

        // 특성2: 돌풍
        MercenaryCharacteristic dolpung = upsertChar(m, "myeongwang-daewidok-dolpung", "돌풍");
        seedLevels(dolpung, "돌풍 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, null);
        seedLevelsTile(dolpung, "회오리 범위",
                new float[]{1, 1, 1, 2, 2, 3, 3, 4, 4, 5});
    }

    // ── 부동명왕 (EARTH) — 스탯 이전 없음 ────────────────────────────────────

    private void seedBudong() {
        Mercenary m = upsertMercenary("부동명왕", "boodong", Nature.EARTH);

        // 특성1: 집중
        MercenaryCharacteristic jipjung = upsertChar(m, "myeongwang-budong-jipjung", "집중");
        seedLevelsSec(jipjung, "지속시간",
                new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        // 특성2: 압박
        MercenaryCharacteristic apbak = upsertChar(m, "myeongwang-budong-apbak", "압박");
        seedLevels(apbak, "상아감옥 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, null);
        seedLevels(apbak, "속박 확률",
                new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, null);
    }

    // ── 금강야차명왕 (WATER) — INTELLECT 기본 이전율 5% ──────────────────────

    private void seedGeumgangyacha() {
        Mercenary m = upsertMercenary("금강야차명왕", "geumgangyacha", Nature.WATER);

        // 특성1: 신비 — 이전되는 지력 + 회복 마법력
        MercenaryCharacteristic sinbi = upsertChar(m, "myeongwang-geumgangyacha-sinbi", "신비");
        seedLevels(sinbi, "이전되는 지력",
                new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, StatType.INTELLECT);
        seedLevels(sinbi, "회복 마법력",
                new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, null);

        // 특성2: 가호
        MercenaryCharacteristic gaho = upsertChar(m, "myeongwang-geumgangyacha-gaho", "가호");
        seedLevels(gaho, "냉기 데미지",
                new float[]{20, 40, 60, 80, 100, 120, 140, 160, 180, 200}, null);
        seedLevels(gaho, "데미지 흡수율",
                new float[]{5, 10, 15, 20, 25, 30, 35, 40, 45, 50}, null);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Mercenary upsertMercenary(String name, String key, Nature nature) {
        return mercenaryRepository.findByName(name).map(existing -> {
            existing.updateKeyIfAbsent(key);
            return existing;
        }).orElseGet(() ->
                mercenaryRepository.save(Mercenary.builder()
                        .name(name)
                        .key(key)
                        .category(MercenaryCategory.MYEONG_KING)
                        .mercenaryType(MercenaryType.MYUNGWANG)
                        .nature(nature)
                        .comingSoon(false)
                        .build()));
    }

    private MercenaryCharacteristic upsertChar(Mercenary mercenary, String key, String name) {
        return characteristicRepository.findByKey(key).orElseGet(() ->
                characteristicRepository.save(MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(name)
                        .point(1)
                        .requiredCharacteristicKey(null)
                        .applyType(CharacteristicApplyType.NORMAL)
                        .build()));
    }

    /** % 단위 레벨 수치 저장 */
    private void seedLevels(MercenaryCharacteristic characteristic, String label,
                             float[] values, StatType statType) {
        for (int i = 0; i < values.length; i++) {
            int level = i + 1;
            float value = values[i];
            if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                    characteristic.getId(), label, level).isPresent()) {
                continue;
            }
            levelRepository.save(MercenaryCharacteristicLevel.builder()
                    .characteristic(characteristic)
                    .label(label)
                    .level(level)
                    .amount(formatAmount(value))
                    .amountValue(value)
                    .statType(statType)
                    .build());
        }
    }

    /** 초(秒) 단위 레벨 수치 저장 — statType=null */
    private void seedLevelsSec(MercenaryCharacteristic characteristic, String label,
                                float[] seconds) {
        for (int i = 0; i < seconds.length; i++) {
            int level = i + 1;
            float value = seconds[i];
            if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                    characteristic.getId(), label, level).isPresent()) {
                continue;
            }
            int intVal = (int) value;
            String amount = intVal == value ? intVal + "초" : value + "초";
            levelRepository.save(MercenaryCharacteristicLevel.builder()
                    .characteristic(characteristic)
                    .label(label)
                    .level(level)
                    .amount(amount)
                    .amountValue(value)
                    .statType(null)
                    .build());
        }
    }

    /** 칸(tile) 단위 레벨 수치 저장 — statType=null */
    private void seedLevelsTile(MercenaryCharacteristic characteristic, String label,
                                 float[] tiles) {
        for (int i = 0; i < tiles.length; i++) {
            int level = i + 1;
            float value = tiles[i];
            if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                    characteristic.getId(), label, level).isPresent()) {
                continue;
            }
            int intVal = (int) value;
            String amount = intVal == value ? intVal + "칸" : value + "칸";
            levelRepository.save(MercenaryCharacteristicLevel.builder()
                    .characteristic(characteristic)
                    .label(label)
                    .level(level)
                    .amount(amount)
                    .amountValue(value)
                    .statType(null)
                    .build());
        }
    }

    private String formatAmount(float value) {
        int intVal = (int) value;
        return intVal == value ? intVal + "%" : value + "%";
    }
}
