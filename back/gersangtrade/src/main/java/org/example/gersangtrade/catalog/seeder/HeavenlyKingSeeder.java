package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.enums.MercenaryCategory;
import org.example.gersangtrade.domain.catalog.enums.MercenaryType;
import org.example.gersangtrade.domain.catalog.enums.Nature;
import org.example.gersangtrade.domain.catalog.enums.StatType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 사천왕 4마리 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행되며, 이미 존재하면 건너뛴다.
 * 크롤링 없음 — 게임 데이터 하드코딩.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeavenlyKingSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryRepository.findByName("지국천왕").isPresent()) {
            log.debug("일반 사천왕 시딩 skip: 이미 존재");
            return;
        }

        log.info("일반 사천왕 시딩 시작");
        seedJiguk();
        seedGwangmok();
        seedJeungjang();
        seedDamun();
        log.info("일반 사천왕 시딩 완료");
    }

    // ── 지국천왕 (FIRE) ───────────────────────────────────────────────────────

    private void seedJiguk() {
        Mercenary mercenary = upsertMercenary("지국천왕", Nature.FIRE);

        // 특성1: 겁화 — 염룡살진 데미지 (계산기 스킵)
        MercenaryCharacteristic geophwa = upsertCharacteristic(
                mercenary, "heavenlyKing-jiguk-geophwa", "겁화");
        seedLevels(geophwa, "염룡살진 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 75, 95, 120, 150}, null);

        // 특성2: 화상 — label 2개 (화상 데미지 null + 마법저항력 ENEMY 음수)
        MercenaryCharacteristic hwasang = upsertCharacteristic(
                mercenary, "heavenlyKing-jiguk-hwasang", "화상");
        seedLevels(hwasang, "화상 데미지",
                new float[]{20, 40, 60, 80, 100, 120, 140, 160, 180, 200}, null);
        seedLevels(hwasang, "마법 저항력",
                new float[]{-1, -2, -3, -4, -5, -6, -7, -9, -12, -15},
                StatType.MAGIC_RESISTANCE);
    }

    // ── 광목천왕 (WIND) ───────────────────────────────────────────────────────

    private void seedGwangmok() {
        Mercenary mercenary = upsertMercenary("광목천왕", Nature.WIND);

        // 특성1: 광풍 — 풍룡섬 데미지 (계산기 스킵)
        MercenaryCharacteristic gwangpung = upsertCharacteristic(
                mercenary, "heavenlyKing-gwangmok-gwangpung", "광풍");
        seedLevels(gwangpung, "풍룡섬 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 75, 95, 120, 150}, null);

        // 특성2: 충돌 — 타격저항력 ENEMY 음수
        MercenaryCharacteristic chungdol = upsertCharacteristic(
                mercenary, "heavenlyKing-gwangmok-chungdol", "충돌");
        seedLevels(chungdol, "타격 저항력",
                new float[]{-1, -2, -3, -4, -5, -6, -7, -9, -12, -15},
                StatType.HITTING_RESISTANCE);
    }

    // ── 증장천왕 (THUNDER) ────────────────────────────────────────────────────

    private void seedJeungjang() {
        Mercenary mercenary = upsertMercenary("증장천왕", Nature.THUNDER);

        // 특성1: 감전 — 뇌룡격 데미지 (계산기 스킵)
        MercenaryCharacteristic gamjeon = upsertCharacteristic(
                mercenary, "heavenlyKing-jeungjang-gamjeon", "감전");
        seedLevels(gamjeon, "뇌룡격 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 75, 95, 120, 150}, null);

        // 특성2: 충격파 — 3배 피해 확률 (계산기 스킵)
        MercenaryCharacteristic chunggyeokpa = upsertCharacteristic(
                mercenary, "heavenlyKing-jeungjang-chunggyeokpa", "충격파");
        seedLevels(chunggyeokpa, "3배 피해 확률",
                new float[]{4, 8, 12, 16, 20, 24, 30, 40, 55, 70}, null);
    }

    // ── 다문천왕 (WATER) ──────────────────────────────────────────────────────

    private void seedDamun() {
        Mercenary mercenary = upsertMercenary("다문천왕", Nature.WATER);

        // 특성1: 강화 — 흑귀 능력치 (소환수 강화, 계산기 스킵)
        MercenaryCharacteristic ganghwa = upsertCharacteristic(
                mercenary, "heavenlyKing-damun-ganghwa", "강화");
        seedLevels(ganghwa, "흑귀 능력치",
                new float[]{10, 20, 30, 40, 50, 60, 75, 95, 120, 150}, null);

        // 특성2: 집중 — 눈사태 데미지 (계산기 스킵)
        MercenaryCharacteristic jipjung = upsertCharacteristic(
                mercenary, "heavenlyKing-damun-jipjung", "집중");
        seedLevels(jipjung, "눈사태 데미지",
                new float[]{10, 20, 30, 40, 50, 60, 75, 95, 120, 150}, null);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Mercenary upsertMercenary(String name, Nature nature) {
        return mercenaryRepository.findByName(name).orElseGet(() ->
                mercenaryRepository.save(Mercenary.builder()
                        .name(name)
                        .category(MercenaryCategory.FOUR_HEAVENLY_KINGS)
                        .mercenaryType(MercenaryType.HEAVENLY_KING)
                        .nature(nature)
                        .comingSoon(false)
                        .build()));
    }

    private MercenaryCharacteristic upsertCharacteristic(
            Mercenary mercenary, String key, String name) {
        return characteristicRepository.findByKey(key).orElseGet(() ->
                characteristicRepository.save(MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(name)
                        .point(1)
                        .requiredCharacteristicKey(null)
                        .build()));
    }

    private void seedLevels(MercenaryCharacteristic characteristic, String label,
                             float[] values, StatType statType) {
        for (int i = 0; i < values.length; i++) {
            int level = i + 1;
            float value = values[i];
            if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                    characteristic.getId(), label, level).isPresent()) {
                continue;
            }
            String amount = formatAmount(value);
            levelRepository.save(MercenaryCharacteristicLevel.builder()
                    .characteristic(characteristic)
                    .label(label)
                    .level(level)
                    .amount(amount)
                    .amountValue(value)
                    .statType(statType)
                    .build());
        }
    }

    private String formatAmount(float value) {
        int intVal = (int) value;
        return intVal == value ? intVal + "%" : value + "%";
    }
}
