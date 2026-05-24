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
 * 각성 명왕 4마리 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행되며, 이미 존재하면 건너뛴다.
 * 크롤링 없음 — 게임 데이터 하드코딩.
 * 공통 아군 버프(속성값 +5/+2)는 엔티티 저장 없이 계산기에서 하드코딩 처리.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class AwakenedMyeongwangSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryRepository.findByName("각성 항삼세명왕").isPresent()) {
            log.debug("각성 명왕 시딩 skip: 이미 존재");
            return;
        }

        log.info("각성 명왕 시딩 시작");
        seedGakHangsamse();
        seedGakGundari();
        seedGakDaewidok();
        seedGakGeumgangyacha();
        log.info("각성 명왕 시딩 완료");
    }

    // ── 각성 항삼세명왕 (FIRE) ────────────────────────────────────────────────

    private void seedGakHangsamse() {
        Mercenary m = upsertMercenary("각성 항삼세명왕", "gakHangsamse", Nature.FIRE);

        // 각성 특성: 자신 ELEMENT_VALUE FIRE +15
        upsertAwakening(m, "awakenedMyeongwang-hangsamse-awakening", "각성");

        // 하위: 파괴 (point=1) — 힘 이전율
        MercenaryCharacteristic pakoe = upsertChar(m, "awakenedMyeongwang-hangsamse-pakoe", "파괴", 1, null);
        seedLevels(pakoe, "힘 이전율", new float[]{2, 3, 7, 10, 30}, StatType.STRENGTH);

        // 상위: 잠식 (point=2, required=파괴)
        MercenaryCharacteristic jamsik = upsertChar(m, "awakenedMyeongwang-hangsamse-jamsik", "잠식", 2, "awakenedMyeongwang-hangsamse-pakoe");
        seedLevels(jamsik, "받는 데미지 증가", new float[]{1, 1, 1, 2, 2}, null);
        seedLevelsCount(jamsik, "중첩",          new float[]{0, 2, 3, 4, 5});

        // 하위: 화신 (point=1)
        MercenaryCharacteristic hwasin = upsertChar(m, "awakenedMyeongwang-hangsamse-hwasin", "화신", 1, null);
        seedLevels(hwasin, "화무신 생명력 증가율",  new float[]{ 5, 15, 30,  50, 100}, null);
        seedLevels(hwasin, "화무신 피해감소 증가율", new float[]{ 1,  2,  5,  10,  20}, null);

        // 상위: 어령 (point=2, required=화신)
        MercenaryCharacteristic eoryeong = upsertChar(m, "awakenedMyeongwang-hangsamse-eoryeong", "어령", 2, "awakenedMyeongwang-hangsamse-hwasin");
        seedLevels(eoryeong, "화무신 염랑 데미지 증가", new float[]{10, 30, 50, 150, 300}, null);
    }

    // ── 각성 군다리명왕 (THUNDER) ─────────────────────────────────────────────

    private void seedGakGundari() {
        Mercenary m = upsertMercenary("각성 군다리명왕", "gakGoondari", Nature.THUNDER);

        // 각성 특성: 자신 ELEMENT_VALUE THUNDER +15
        upsertAwakening(m, "awakenedMyeongwang-gundari-awakening", "각성");

        // 하위: 신속 (point=1) — 민첩성 이전율
        MercenaryCharacteristic sinsok = upsertChar(m, "awakenedMyeongwang-gundari-sinsok", "신속", 1, null);
        seedLevels(sinsok, "민첩성 이전율", new float[]{2, 3, 7, 15, 30}, StatType.DEXTERITY);

        // 상위: 쇄도 (point=2, required=신속) — 아군 데미지 증가율
        MercenaryCharacteristic swaedo = upsertChar(m, "awakenedMyeongwang-gundari-swaedo", "쇄도", 2, "awakenedMyeongwang-gundari-sinsok");
        seedLevels(swaedo, "아군 데미지 증가율", new float[]{1, 2, 3, 5, 8}, StatType.DAMAGE_PERCENT);

        // 하위: 우뢰 (point=1)
        MercenaryCharacteristic uroe = upsertChar(m, "awakenedMyeongwang-gundari-uroe", "우뢰", 1, null);
        seedLevels(uroe, "데미지 증가",        new float[]{ 50, 70, 100, 150, 200}, null);
        seedLevelsSec(uroe, "저항 감소 지속시간", new float[]{ 1,  2,   3,   4,   5});

        // 상위: 낙인 (point=2, required=우뢰)
        MercenaryCharacteristic nakin = upsertChar(m, "awakenedMyeongwang-gundari-nakin", "낙인", 2, "awakenedMyeongwang-gundari-uroe");
        seedLevels(nakin, "거리비례 데미지", new float[]{1, 3, 5, 7, 10}, null);
        seedLevels(nakin, "치명타 피해량",  new float[]{1, 3, 5, 7, 10}, null);
    }

    // ── 각성 대위덕명왕 (WIND) ────────────────────────────────────────────────

    private void seedGakDaewidok() {
        Mercenary m = upsertMercenary("각성 대위덕명왕", "gakDaewideok", Nature.WIND);

        // 각성 특성: 자신 ELEMENT_VALUE WIND +15
        upsertAwakening(m, "awakenedMyeongwang-daewidok-awakening", "각성");

        // 하위: 끈기 (point=1) — 생명력 이전율
        MercenaryCharacteristic kkeungi = upsertChar(m, "awakenedMyeongwang-daewidok-kkeungi", "끈기", 1, null);
        seedLevels(kkeungi, "생명력 이전율", new float[]{2, 3, 7, 10, 30}, StatType.VITALITY);

        // 상위: 영속 (point=2, required=끈기) — 공격속도·이동속도·타격저항 감소 (ENEMY 음수)
        MercenaryCharacteristic yeongsok = upsertChar(m, "awakenedMyeongwang-daewidok-yeongsok", "영속", 2, "awakenedMyeongwang-daewidok-kkeungi");
        seedLevels(yeongsok, "공격속도 감소", new float[]{ -2,  -3,  -5,  -7, -10}, StatType.ATTACK_SPEED);
        seedLevels(yeongsok, "이동속도 감소", new float[]{ -2,  -3,  -5, -10, -20}, StatType.MOVE_SPEED);
        seedLevels(yeongsok, "타격저항 감소", new float[]{ -1,  -2,  -3,  -4,  -5}, StatType.HITTING_RESISTANCE);

        // 하위: 돌풍 (point=1)
        MercenaryCharacteristic dolpung = upsertChar(m, "awakenedMyeongwang-daewidok-dolpung", "돌풍", 1, null);
        seedLevels(dolpung, "흡풍멸진 데미지", new float[]{50, 70, 100, 150, 200}, null);
        seedLevelsTile(dolpung, "범위 증가",   new float[]{ 0,  1,   1,   2,   2});

        // 상위: 분천 (point=2, required=돌풍)
        // ※ 5레벨 적 속성값 -5 효과는 MVP 제외 (오픈 이슈)
        MercenaryCharacteristic buncheon = upsertChar(m, "awakenedMyeongwang-daewidok-buncheon", "분천", 2, "awakenedMyeongwang-daewidok-dolpung");
        seedLevels(buncheon, "시전속도",   new float[]{ 2,  3,  10,  30,  50}, null);
        seedLevels(buncheon, "폭발 데미지", new float[]{10, 30,  50,  70, 150}, null);
    }

    // ── 각성 금강야차명왕 (WATER) ──────────────────────────────────────────────

    private void seedGakGeumgangyacha() {
        Mercenary m = upsertMercenary("각성 금강야차명왕", "gakGeumgangyacha", Nature.WATER);

        // 각성 특성: 자신 ELEMENT_VALUE WATER +15
        upsertAwakening(m, "awakenedMyeongwang-geumgangyacha-awakening", "각성");

        // 하위: 신비 (point=1) — 지력 이전율
        MercenaryCharacteristic sinbi = upsertChar(m, "awakenedMyeongwang-geumgangyacha-sinbi", "신비", 1, null);
        seedLevels(sinbi, "지력 이전율", new float[]{2, 3, 7, 15, 30}, StatType.INTELLECT);

        // 상위: 영벽 (point=2, required=신비)
        MercenaryCharacteristic yeongbyeok = upsertChar(m, "awakenedMyeongwang-geumgangyacha-yeongbyeok", "영벽", 2, "awakenedMyeongwang-geumgangyacha-sinbi");
        seedLevels(yeongbyeok, "피해 감소",   new float[]{ 1,  2,  5,  7,  10}, null);
        seedLevels(yeongbyeok, "마법력 증가",  new float[]{10, 20, 35, 50, 100}, null);

        // 하위: 가호 (point=1)
        MercenaryCharacteristic gaho = upsertChar(m, "awakenedMyeongwang-geumgangyacha-gaho", "가호", 1, null);
        seedLevels(gaho, "데미지 증가",    new float[]{ 50, 70, 100, 150, 200}, null);
        seedLevels(gaho, "피해감소율 증가", new float[]{  2,  5,  10,  15,  30}, null);

        // 상위: 격노 (point=2, required=가호)
        MercenaryCharacteristic gyeongno = upsertChar(m, "awakenedMyeongwang-geumgangyacha-gyeongno", "격노", 2, "awakenedMyeongwang-geumgangyacha-gaho");
        seedLevelsTile(gyeongno,  "사거리 증가",    new float[]{ 1,  1,  1,  2,  2});
        seedLevels(gyeongno,      "중첩 당 데미지", new float[]{ 1,  1,  2,  2,  3}, null);
        seedLevelsCount(gyeongno, "최대 중첩 횟수", new float[]{10, 20, 30, 40, 50});
        seedLevelsCount(gyeongno, "피격 횟수",      new float[]{20, 15, 10, 10,  5});
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
                        .category(MercenaryCategory.MYEONG_KING_AWAKENING)
                        .mercenaryType(MercenaryType.AWAKENED_MYUNGWANG)
                        .nature(nature)
                        .comingSoon(false)
                        .build()));
    }

    /** 각성 특성: point=null, SELF_AUTO */
    private void upsertAwakening(Mercenary mercenary, String key, String name) {
        characteristicRepository.findByKey(key).orElseGet(() ->
                characteristicRepository.save(MercenaryCharacteristic.builder()
                        .mercenary(mercenary)
                        .key(key)
                        .name(name)
                        .point(null)
                        .requiredCharacteristicKey(null)
                        .applyType(CharacteristicApplyType.SELF_AUTO)
                        .build()));
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

    /** 단위 없는 순수 수치 저장 (중첩 횟수, 피격 횟수 등) — statType=null */
    private void seedLevelsCount(MercenaryCharacteristic characteristic, String label,
                                  float[] counts) {
        for (int i = 0; i < counts.length; i++) {
            int level = i + 1;
            float value = counts[i];
            if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                    characteristic.getId(), label, level).isPresent()) {
                continue;
            }
            int intVal = (int) value;
            String amount = intVal == value ? String.valueOf(intVal) : String.valueOf(value);
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
