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
 * 각성 사천왕 4마리 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행되며, 이미 존재하면 건너뛴다.
 * 크롤링 없음 — 게임 데이터 하드코딩.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AwakenedHeavenlyKingSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryRepository.findByName("각성 지국천왕").isPresent()) {
            log.debug("각성 사천왕 시딩 skip: 이미 존재");
            return;
        }

        log.info("각성 사천왕 시딩 시작");
        seedGakJiguk();
        seedGakGwangmok();
        seedGakJeungjang();
        seedGakDamun();
        log.info("각성 사천왕 시딩 완료");
    }

    // ── 각성 지국천왕 (FIRE) ──────────────────────────────────────────────────

    private void seedGakJiguk() {
        Mercenary m = upsertMercenary("각성 지국천왕", "gakJigook", Nature.FIRE);

        // 각성 특성: ELEMENT_VALUE FIRE +20 영구 적용
        upsertAwakening(m, "awakenedHK-jiguk-awakening", "각성");

        // 하위: 겁화 (point=1)
        MercenaryCharacteristic geophwa = upsertChar(m, "awakenedHK-jiguk-geophwa", "겁화", 1, null);
        seedLevels(geophwa, "염화무극진 데미지", new float[]{20, 40, 70, 100, 150}, null);

        // 상위: 경안 (point=2, required=겁화)
        MercenaryCharacteristic gyeongan = upsertChar(m, "awakenedHK-jiguk-gyeongan", "경안", 2, "awakenedHK-jiguk-geophwa");
        seedLevels(gyeongan, "데미지",  new float[]{5, 10, 15, 20, 40}, null);
        seedLevels(gyeongan, "기절",    new float[]{2,  4,  6,  8, 10}, null);

        // 하위: 화상 (point=1) — label 2개
        MercenaryCharacteristic hwasang = upsertChar(m, "awakenedHK-jiguk-hwasang", "화상", 1, null);
        seedLevels(hwasang, "화상 데미지", new float[]{20, 50, 100, 150, 200}, null);
        seedLevels(hwasang, "마법저항력",  new float[]{-1,  -3,  -6, -10, -15}, StatType.MAGIC_RESISTANCE);

        // 상위: 층화 (point=2, required=화상) — "지속시간" label은 "초" 단위
        MercenaryCharacteristic cheunghwa = upsertChar(m, "awakenedHK-jiguk-cheunghwa", "층화", 2, "awakenedHK-jiguk-hwasang");
        seedLevels(cheunghwa, "중첩 데미지 증가율", new float[]{2,  5, 20,  50, 100}, null);
        seedLevelsSec(cheunghwa, "지속시간",         new float[]{2f, 3f, 5f,  7f,  10f});
    }

    // ── 각성 광목천왕 (WIND) ──────────────────────────────────────────────────

    private void seedGakGwangmok() {
        Mercenary m = upsertMercenary("각성 광목천왕", "gakGwangmok", Nature.WIND);

        // 각성 특성: ELEMENT_VALUE WIND +20 영구 적용
        upsertAwakening(m, "awakenedHK-gwangmok-awakening", "각성");

        // 하위: 광풍 (point=1)
        MercenaryCharacteristic gwangpung = upsertChar(m, "awakenedHK-gwangmok-gwangpung", "광풍", 1, null);
        seedLevels(gwangpung, "풍극진멸 데미지", new float[]{20, 40, 70, 100, 150}, null);

        // 상위: 연파 (point=2, required=광풍)
        MercenaryCharacteristic yeonpa = upsertChar(m, "awakenedHK-gwangmok-yeonpa", "연파", 2, "awakenedHK-gwangmok-gwangpung");
        seedLevels(yeonpa, "생명력 증가",    new float[]{ 2,  5, 10,  15,  30}, null);
        seedLevels(yeonpa, "고정 추가 피해", new float[]{ 3,  8, 15,  25,  50}, null);

        // 하위: 충돌 (point=1)
        MercenaryCharacteristic chungdol = upsertChar(m, "awakenedHK-gwangmok-chungdol", "충돌", 1, null);
        seedLevels(chungdol, "타격저항력", new float[]{-1, -3, -6, -10, -15}, StatType.HITTING_RESISTANCE);

        // 상위: 신보 (point=2, required=충돌)
        MercenaryCharacteristic sinbo = upsertChar(m, "awakenedHK-gwangmok-sinbo", "신보", 2, "awakenedHK-gwangmok-chungdol");
        seedLevels(sinbo, "기본 데미지", new float[]{ 2,  5,  10,  20,  40}, null);
        seedLevels(sinbo, "추가 데미지", new float[]{10, 25,  50, 150, 200}, null);
    }

    // ── 각성 증장천왕 (THUNDER) ───────────────────────────────────────────────

    private void seedGakJeungjang() {
        Mercenary m = upsertMercenary("각성 증장천왕", "gakJeungjang", Nature.THUNDER);

        // 각성 특성: ELEMENT_VALUE THUNDER +20 영구 적용
        upsertAwakening(m, "awakenedHK-jeungjang-awakening", "각성");

        // 하위: 감전 (point=1)
        MercenaryCharacteristic gamjeon = upsertChar(m, "awakenedHK-jeungjang-gamjeon", "감전", 1, null);
        seedLevels(gamjeon, "천궁뇌격 데미지", new float[]{20, 50, 70, 100, 150}, null);

        // 상위: 전폭 (point=2, required=감전) — label 3개
        MercenaryCharacteristic jeonpok = upsertChar(m, "awakenedHK-jeungjang-jeonpok", "전폭", 2, "awakenedHK-jeungjang-gamjeon");
        seedLevels(jeonpok, "전격 확률",   new float[]{  2,   3,   5,  15,  50}, null);
        seedLevels(jeonpok, "전폭 피해",   new float[]{100, 150, 200, 300, 400}, null);
        seedLevels(jeonpok, "주변 데미지", new float[]{ 50,  75, 100, 150, 200}, null);

        // 하위: 충격파 (point=1) — 공중 몬스터 마법저항 감소 (항상 적용 가정)
        MercenaryCharacteristic chunggyeokpa = upsertChar(m, "awakenedHK-jeungjang-chunggyeokpa", "충격파", 1, null);
        seedLevels(chunggyeokpa, "3배 피해 확률",         new float[]{ 4, 10, 20,  40,  70}, StatType.CRITICAL_CHANCE);
        seedLevels(chunggyeokpa, "마법저항 (공중 몬스터)", new float[]{-1, -3, -6, -10, -15}, StatType.MAGIC_RESISTANCE);

        // 상위: 원격 (point=2, required=충격파)
        MercenaryCharacteristic wongyeok = upsertChar(m, "awakenedHK-jeungjang-wongyeok", "원격", 2, "awakenedHK-jeungjang-chunggyeokpa");
        seedLevels(wongyeok, "치명타 피해", new float[]{2, 3, 5, 20, 50}, StatType.CRITICAL_DAMAGE);
        // 레벨 5 전용: 기본 데미지 2배 (100% 가산 = ×2)
        seedSingleLevel(wongyeok, "기본 데미지 배율", 5, 100.0f, "100%", StatType.BASE_DAMAGE_MULTIPLIER);
    }

    // ── 각성 다문천왕 (WATER) ─────────────────────────────────────────────────

    private void seedGakDamun() {
        Mercenary m = upsertMercenary("각성 다문천왕", "gakDamoon", Nature.WATER);

        // 각성 특성: ELEMENT_VALUE WATER +20 영구 적용
        upsertAwakening(m, "awakenedHK-damun-awakening", "각성");

        // 하위: 강화 (point=1) — 덱버프(마법저항 감소) + 소환수 강화 동시 보유
        MercenaryCharacteristic ganghwa = upsertChar(m, "awakenedHK-damun-ganghwa", "강화", 1, null);
        seedLevels(ganghwa, "소환수 스탯", new float[]{20, 40, 70, 100, 150}, null);
        seedLevels(ganghwa, "마법저항력",  new float[]{-1,  -3,  -6, -10, -15}, StatType.MAGIC_RESISTANCE);

        // 상위: 안식 (point=2, required=강화)
        MercenaryCharacteristic ansik = upsertChar(m, "awakenedHK-damun-ansik", "안식", 2, "awakenedHK-damun-ganghwa");
        seedLevels(ansik, "청빙격류 데미지", new float[]{ 2,  3,  5, 30, 80}, null);
        seedLevels(ansik, "피해",           new float[]{ 1,  2,  3,  7, 10}, null);

        // 하위: 집중 (point=1)
        MercenaryCharacteristic jipjung = upsertChar(m, "awakenedHK-damun-jipjung", "집중", 1, null);
        seedLevels(jipjung, "청빙격류 데미지", new float[]{20, 50, 70, 100, 150}, null);

        // 상위: 전심 (point=2, required=집중)
        MercenaryCharacteristic jeonshim = upsertChar(m, "awakenedHK-damun-jeonshim", "전심", 2, "awakenedHK-damun-jipjung");
        seedLevels(jeonshim, "데미지", new float[]{2, 3, 5, 10, 25}, null);
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
                        .category(MercenaryCategory.FOUR_HEAVENLY_KINGS_AWAKENING)
                        .mercenaryType(MercenaryType.AWAKENED_HEAVENLY_KING)
                        .nature(nature)
                        .comingSoon(false)
                        .build()));
    }

    /** 각성 특성: SELF_AUTO, 속성값 +20 고정 효과를 level=1 행으로 저장 */
    private void upsertAwakening(Mercenary mercenary, String key, String name) {
        MercenaryCharacteristic characteristic = characteristicRepository.findByKey(key)
                .orElseGet(() -> characteristicRepository.save(MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(name)
                        .point(null)
                        .requiredCharacteristicKey(null)
                        .applyType(CharacteristicApplyType.SELF_AUTO)
                        .build()));
        if (levelRepository.findByCharacteristicIdAndLabelAndLevel(characteristic.getId(), "속성값", 1).isEmpty()) {
            levelRepository.save(MercenaryCharacteristicLevel.builder()
                    .characteristic(characteristic)
                    .label("속성값")
                    .level(1)
                    .amount("20")
                    .amountValue(20.0f)
                    .statType(StatType.ELEMENT_VALUE)
                    .build());
        }
    }

    private MercenaryCharacteristic upsertChar(Mercenary mercenary, String key, String name,
                                               Integer point, String requiredKey) {
        return characteristicRepository.findByKey(key).orElseGet(() ->
                characteristicRepository.save(MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(name)
                        .point(point)
                        .requiredCharacteristicKey(requiredKey)
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

    /** 특정 레벨 단독 저장 — 레벨 5 전용 특수 효과 등에 사용 */
    private void seedSingleLevel(MercenaryCharacteristic characteristic, String label,
                                  int level, float value, String amount, StatType statType) {
        if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                characteristic.getId(), label, level).isPresent()) {
            return;
        }
        levelRepository.save(MercenaryCharacteristicLevel.builder()
                .characteristic(characteristic)
                .label(label)
                .level(level)
                .amount(amount)
                .amountValue(value)
                .statType(statType)
                .build());
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

    private String formatAmount(float value) {
        int intVal = (int) value;
        return intVal == value ? intVal + "%" : value + "%";
    }
}
