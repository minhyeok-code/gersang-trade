package org.example.gersangtrade.catalog.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicLevelRepository;
import org.example.gersangtrade.catalog.repository.MercenaryCharacteristicRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MercenaryStatRepository;
import org.example.gersangtrade.catalog.repository.PlayerCharacterDetailRepository;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristic;
import org.example.gersangtrade.domain.catalog.MercenaryCharacteristicLevel;
import org.example.gersangtrade.domain.catalog.MercenaryStat;
import org.example.gersangtrade.domain.catalog.PlayerCharacterDetail;
import org.example.gersangtrade.domain.catalog.enums.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주인공 10명(5국가 × 2성별) 초기 데이터 시딩.
 * 애플리케이션 기동 시 1회 실행되며, 이미 존재하면 건너뛴다.
 * 크롤링 없음 — 게임 데이터 하드코딩.
 *
 * <p>국가-속성 매핑: 조선=FIRE, 일본=WATER, 중국=WIND, 대만=THUNDER, 인도=EARTH
 * <p>아군/적군 공격력(MIN_POWER+MAX_POWER) label은 별도 행으로 분리 저장 (unique 제약 회피)
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class PlayerCharacterSeeder implements ApplicationRunner {

    private final MercenaryRepository mercenaryRepository;
    private final MercenaryStatRepository mercenaryStatRepository;
    private final MercenaryCharacteristicRepository characteristicRepository;
    private final MercenaryCharacteristicLevelRepository levelRepository;
    private final PlayerCharacterDetailRepository detailRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (mercenaryRepository.findByName("신궁").isPresent()) {
            log.debug("주인공 시딩 skip: 이미 존재");
            return;
        }

        log.info("주인공 시딩 시작");
        seedJoseonMale();
        seedJoseonFemale();
        seedJapanMale();
        seedJapanFemale();
        seedChinaMale();
        seedChinaFemale();
        seedTaiwanFemale();
        seedTaiwanMale();
        seedIndiaMale();
        seedIndiaFemale();
        log.info("주인공 시딩 완료");
    }

    // ── 조선 남 (신궁) ────────────────────────────────────────────────────────

    private void seedJoseonMale() {
        Mercenary m = upsertMercenary("신궁", Nation.JOSEON, Nature.FIRE);
        upsertDetail(m, Nation.JOSEON, Gender.MALE);
        upsertStats(m, 150, 400, 200, 150, 70, 80, 25, 11);

        // 확산 (하위 point=1)
        MercenaryCharacteristic hwaksan = upsertChar(m, "pc-joseon-m-hwaksan", "확산", 1, null);
        seedLevels(hwaksan, "피해",     new float[]{10, 20, 30, 40, 50}, StatType.DAMAGE_PERCENT);
        seedLevelsSec(hwaksan, "지속시간", new float[]{ 1,  1,  2,  2,  3});

        // 충격 (상위 point=2, required=확산)
        MercenaryCharacteristic chungyeok = upsertChar(m, "pc-joseon-m-chungyeok", "충격", 2, "pc-joseon-m-hwaksan");
        seedLevels(chungyeok, "확률",     new float[]{20, 30, 40, 50, 60}, null);
        seedLevelsSec(chungyeok, "지속시간", new float[]{ 1,  1,  2,  2,  3});

        // 폭발 (하위 point=1)
        MercenaryCharacteristic pokbal = upsertChar(m, "pc-joseon-m-pokbal", "폭발", 1, null);
        seedLevels(pokbal, "연사 피해량", new float[]{10, 20, 30, 40, 50}, null);

        // 정밀 (상위 point=2, required=폭발)
        MercenaryCharacteristic jeongmil = upsertChar(m, "pc-joseon-m-jeongmil", "정밀", 2, "pc-joseon-m-pokbal");
        seedLevelsFlat(jeongmil, "저항력 감소", new float[]{2, 4, 6, 8, 10}, StatType.RESIST_PIERCE);
    }

    // ── 조선 여 (포수) ────────────────────────────────────────────────────────

    private void seedJoseonFemale() {
        Mercenary m = upsertMercenary("포수", Nation.JOSEON, Nature.FIRE);
        upsertDetail(m, Nation.JOSEON, Gender.FEMALE);
        upsertStats(m, 450, 50, 250, 150, 60, 90, 25, 6);

        // 집중 (하위 point=1)
        MercenaryCharacteristic jipjung = upsertChar(m, "pc-joseon-f-jipjung", "집중", 1, null);
        seedLevels(jipjung, "피해", new float[]{10, 25, 50, 75, 100}, null);

        // 산탄 (상위 point=2, required=집중)
        MercenaryCharacteristic santan = upsertChar(m, "pc-joseon-f-santan", "산탄", 2, "pc-joseon-f-jipjung");
        seedLevels(santan, "추가피해 확률", new float[]{50, 55, 60, 65, 70}, null);
        seedLevels(santan, "산탄피해량",   new float[]{10, 15, 20, 25, 30}, null);

        // 과녁 (하위 point=1)
        MercenaryCharacteristic gwanyeok = upsertChar(m, "pc-joseon-f-gwanyeok", "과녁", 1, null);
        seedLevels(gwanyeok, "피해량", new float[]{10, 20, 30, 40, 50}, null);

        // 관통 (상위 point=2, required=과녁)
        MercenaryCharacteristic gwantong = upsertChar(m, "pc-joseon-f-gwantong", "관통", 2, "pc-joseon-f-gwanyeok");
        seedLevels(gwantong, "방패 무효화", new float[]{10, 20, 30, 40, 50}, null);

        // 상재 (독립 point=1, required=null)
        MercenaryCharacteristic sangjae = upsertChar(m, "pc-joseon-f-sangjae", "상재", 1, null);
        seedLevels(sangjae, "상점판매액", new float[]{10, 15, 20, 25, 30}, null);
    }

    // ── 일본 남 (검호) ────────────────────────────────────────────────────────

    private void seedJapanMale() {
        Mercenary m = upsertMercenary("검호", Nation.JAPAN, Nature.WATER);
        upsertDetail(m, Nation.JAPAN, Gender.MALE);
        upsertStats(m, 380, 60, 260, 200, 80, 70, 25, 6);

        // 집중 (하위 point=1)
        MercenaryCharacteristic jipjung = upsertChar(m, "pc-japan-m-jipjung", "집중", 1, null);
        seedLevels(jipjung, "발도 피해량", new float[]{10, 20, 30, 40, 50}, null);

        // 치명상 (상위 point=2, required=집중)
        MercenaryCharacteristic chimyeongsang = upsertChar(m, "pc-japan-m-chimyeongsang", "치명상", 2, "pc-japan-m-jipjung");
        seedLevels(chimyeongsang, "3배 피해 확률", new float[]{10, 20, 30, 40, 50}, null);
        seedLevels(chimyeongsang, "기절 확률",     new float[]{20, 25, 30, 35, 40}, null);

        // 교활 (하위 point=1)
        MercenaryCharacteristic gyohwal = upsertChar(m, "pc-japan-m-gyohwal", "교활", 1, null);
        seedLevelsFlat(gyohwal, "저항력 감소", new float[]{2, 4, 6, 8, 10}, StatType.RESIST_PIERCE);

        // 난도 (상위 point=2, required=교활)
        MercenaryCharacteristic nando = upsertChar(m, "pc-japan-m-nando", "난도", 2, "pc-japan-m-gyohwal");
        seedLevels(nando, "피해 증가",        new float[]{10, 25, 40, 55, 70}, null);
        seedLevels(nando, "표식 필요 데미지", new float[]{10, 20, 30, 40, 50}, null);
    }

    // ── 일본 여 (이타코) ──────────────────────────────────────────────────────

    private void seedJapanFemale() {
        Mercenary m = upsertMercenary("이타코", Nation.JAPAN, Nature.WATER);
        upsertDetail(m, Nation.JAPAN, Gender.FEMALE);
        upsertStats(m, 180, 70, 300, 350, 70, 80, 25, 6);

        // 분노 (하위 point=1)
        MercenaryCharacteristic bunno = upsertChar(m, "pc-japan-f-bunno", "분노", 1, null);
        seedLevels(bunno, "빙룡출사 피해량", new float[]{10, 20, 30, 40, 50}, null);

        // 혹한 (상위 point=2, required=분노)
        MercenaryCharacteristic hokhan = upsertChar(m, "pc-japan-f-hokhan", "혹한", 2, "pc-japan-f-bunno");
        seedLevels(hokhan, "빙룡 강림 확률", new float[]{30, 35, 40, 45, 50}, null);
        seedLevels(hokhan, "빙결 확률",      new float[]{ 2,  4,  6,  8, 10}, null);

        // 마력주입 (하위 point=1)
        MercenaryCharacteristic maryeokjuip = upsertChar(m, "pc-japan-f-maryeokjuip", "마력주입", 1, null);
        seedLevels(maryeokjuip, "피해흡수율", new float[]{10, 20, 30, 50, 70}, null);

        // 마력사출 (상위 point=2, required=마력주입)
        MercenaryCharacteristic maryeoksachul = upsertChar(m, "pc-japan-f-maryeoksachul", "마력사출", 2, "pc-japan-f-maryeokjuip");
        seedLevels(maryeoksachul, "마법력", new float[]{10, 20, 30, 40, 50}, null);
    }

    // ── 중국 남 (일대종사) ────────────────────────────────────────────────────

    private void seedChinaMale() {
        Mercenary m = upsertMercenary("일대종사", Nation.CHINA, Nature.WIND);
        upsertDetail(m, Nation.CHINA, Gender.MALE);
        upsertStats(m, 320, 30, 450, 100, 100, 50, 25, 6);

        // 충격 (하위 point=1)
        MercenaryCharacteristic chungyeok = upsertChar(m, "pc-china-m-chungyeok", "충격", 1, null);
        seedLevels(chungyeok, "공진각 피해량", new float[]{10, 20, 30, 40, 50}, null);

        // 공진 (상위 point=2, required=충격)
        MercenaryCharacteristic gongjin = upsertChar(m, "pc-china-m-gongjin", "공진", 2, "pc-china-m-chungyeok");
        seedLevelsTile(gongjin, "공진각 피해 범위", new float[]{1, 1, 2, 2, 3});
        seedLevels(gongjin, "기절 확률",           new float[]{5, 5, 10, 10, 15}, null);

        // 저항 (하위 point=1)
        MercenaryCharacteristic jeohang = upsertChar(m, "pc-china-m-jeohang", "저항", 1, null);
        seedLevels(jeohang, "피해 감소량", new float[]{1, 1, 2, 2, 3}, null);

        // 기합 (상위 point=2, required=저항)
        MercenaryCharacteristic gihap = upsertChar(m, "pc-china-m-gihap", "기합", 2, "pc-china-m-jeohang");
        seedLevelsFlat(gihap, "저항력 감소", new float[]{2, 4, 6, 8, 10}, StatType.RESIST_PIERCE);
    }

    // ── 중국 여 (강신) ────────────────────────────────────────────────────────

    private void seedChinaFemale() {
        Mercenary m = upsertMercenary("강신", Nation.CHINA, Nature.WIND);
        upsertDetail(m, Nation.CHINA, Gender.FEMALE);
        upsertStats(m, 200, 60, 300, 340, 50, 100, 25, 6);

        // 축복 (하위 point=1)
        MercenaryCharacteristic chukbok = upsertChar(m, "pc-china-f-chukbok", "축복", 1, null);
        seedLevels(chukbok, "회복량",   new float[]{10, 20, 30, 40, 50}, null);
        seedLevelsSec(chukbok, "유지시간", new float[]{ 5, 10, 15, 20, 25});

        // 가호 (상위 point=2, required=축복)
        MercenaryCharacteristic gaho = upsertChar(m, "pc-china-f-gaho", "가호", 2, "pc-china-f-chukbok");
        seedLevels(gaho, "마법력 추가 회복량",
                new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f}, null);

        // 의지 (하위 point=1)
        MercenaryCharacteristic euiji = upsertChar(m, "pc-china-f-euiji", "의지", 1, null);
        seedLevels(euiji, "보호막 체력 증가", new float[]{10, 20, 30, 40, 50}, null);

        // 확산 (상위 point=2, required=의지)
        MercenaryCharacteristic hwaksan = upsertChar(m, "pc-china-f-hwaksan", "확산", 2, "pc-china-f-euiji");
        seedLevelsTile(hwaksan, "보호막 적용 범위", new float[]{1, 2, 3, 4, 5});
    }

    // ── 대만 여 (백수왕) ──────────────────────────────────────────────────────

    private void seedTaiwanFemale() {
        Mercenary m = upsertMercenary("백수왕", Nation.TAIWAN, Nature.THUNDER);
        upsertDetail(m, Nation.TAIWAN, Gender.FEMALE);
        upsertStats(m, 370, 60, 320, 150, 70, 80, 25, 6);

        // 강인함 (하위 point=1)
        MercenaryCharacteristic ganginham = upsertChar(m, "pc-taiwan-f-ganginham", "강인함", 1, null);
        seedLevels(ganginham, "야수 능력치", new float[]{20, 40, 60, 80, 100}, null);
        seedLevels(ganginham, "저항력",     new float[]{10, 20, 30, 40,  50}, null);

        // 독수리 훈련 (상위 point=2, required=강인함)
        MercenaryCharacteristic doksuri = upsertChar(m, "pc-taiwan-f-doksuri", "독수리 훈련", 2, "pc-taiwan-f-ganginham");
        seedLevels(doksuri, "공격력 추가 감소", new float[]{10, 20, 30, 40, 50}, null);
        seedLevels(doksuri, "기절 확률",        new float[]{ 2,  4,  6,  8, 10}, null);

        // 칼날비 (하위 point=1)
        MercenaryCharacteristic kalnalbi = upsertChar(m, "pc-taiwan-f-kalnalbi", "칼날비", 1, null);
        seedLevels(kalnalbi, "회전칼날 데미지", new float[]{10, 20, 30, 40, 50}, null);
        seedLevels(kalnalbi, "감전 확률",       new float[]{ 5, 10, 15, 20, 25}, null);

        // 비검 (상위 point=2, required=칼날비)
        MercenaryCharacteristic bigeom = upsertChar(m, "pc-taiwan-f-bigeom", "비검", 2, "pc-taiwan-f-kalnalbi");
        seedLevelsFlat(bigeom, "저항력", new float[]{2, 4, 6, 8, 10}, StatType.RESIST_PIERCE);
    }

    // ── 대만 남 (도사) ────────────────────────────────────────────────────────

    private void seedTaiwanMale() {
        Mercenary m = upsertMercenary("도사", Nation.TAIWAN, Nature.THUNDER);
        upsertDetail(m, Nation.TAIWAN, Gender.MALE);
        upsertStats(m, 280, 90, 280, 250, 65, 85, 25, 6);

        // 영험 (하위 point=1)
        MercenaryCharacteristic yeonghyeom = upsertChar(m, "pc-taiwan-m-yeonghyeom", "영험", 1, null);
        seedLevels(yeonghyeom, "체력회복량", new float[]{10, 20, 30, 40, 50}, null);

        // 활성화 (상위 point=2, required=영험)
        MercenaryCharacteristic hwalseonghwa = upsertChar(m, "pc-taiwan-m-hwalseonghwa", "활성화", 2, "pc-taiwan-m-yeonghyeom");
        seedLevels(hwalseonghwa, "아군 최소공격력", new float[]{5, 10, 15, 20, 25}, StatType.MIN_POWER);
        seedLevels(hwalseonghwa, "아군 최대공격력", new float[]{5, 10, 15, 20, 25}, StatType.MAX_POWER);

        // 무기력 (하위 point=1)
        MercenaryCharacteristic mugiryeok = upsertChar(m, "pc-taiwan-m-mugiryeok", "무기력", 1, null);
        seedLevels(mugiryeok, "적군 최소공격력 감소", new float[]{ -5, -15, -25, -35, -45}, StatType.MIN_POWER);
        seedLevels(mugiryeok, "적군 최대공격력 감소", new float[]{ -5, -15, -25, -35, -45}, StatType.MAX_POWER);

        // 혼란 (상위 point=2, required=무기력)
        MercenaryCharacteristic honran = upsertChar(m, "pc-taiwan-m-honran", "혼란", 2, "pc-taiwan-m-mugiryeok");
        seedLevels(honran, "적군 이동속도 감소",   new float[]{ -1,  -5, -10, -15, -20}, StatType.MOVE_SPEED);
        seedLevelsFlat(honran, "적군 사거리 감소", new float[]{  0,   0,  -1,  -1,  -2}, StatType.SKILL_RANGE);
    }

    // ── 인도 남 (투신) ────────────────────────────────────────────────────────

    private void seedIndiaMale() {
        Mercenary m = upsertMercenary("투신", Nation.INDIA, Nature.EARTH);
        upsertDetail(m, Nation.INDIA, Gender.MALE);
        upsertStats(m, 250, 50, 450, 150, 95, 85, 25, 6);

        // 무쇠주먹 (하위 point=1)
        MercenaryCharacteristic musoe = upsertChar(m, "pc-india-m-musoe", "무쇠주먹", 1, null);
        seedLevels(musoe, "팔방타 피해", new float[]{10, 20, 30, 40, 50}, null);
        seedLevels(musoe, "기절 확률",   new float[]{ 2,  4,  6,  8, 10}, null);

        // 무력화 (상위 point=2, required=무쇠주먹)
        MercenaryCharacteristic muryeokhwa = upsertChar(m, "pc-india-m-muryeokhwa", "무력화", 2, "pc-india-m-musoe");
        seedLevels(muryeokhwa, "3배 피해 확률", new float[]{5, 10, 20, 30, 40}, null);

        // 일점사 (하위 point=1)
        MercenaryCharacteristic iljeomsa = upsertChar(m, "pc-india-m-iljeomsa", "일점사", 1, null);
        seedLevels(iljeomsa, "표식 피해량 증가", new float[]{10, 15, 20, 25, 35}, StatType.DAMAGE_PERCENT);

        // 원한 (상위 point=2, required=일점사)
        MercenaryCharacteristic wonhan = upsertChar(m, "pc-india-m-wonhan", "원한", 2, "pc-india-m-iljeomsa");
        seedLevelsSec(wonhan, "표식 유지시간", new float[]{1, 2, 3, 4, 5});
    }

    // ── 인도 여 (무희) ────────────────────────────────────────────────────────

    private void seedIndiaFemale() {
        Mercenary m = upsertMercenary("무희", Nation.INDIA, Nature.EARTH);
        upsertDetail(m, Nation.INDIA, Gender.FEMALE);
        upsertStats(m, 100, 50, 300, 450, 55, 95, 25, 6);

        // 화음 (하위 point=1)
        MercenaryCharacteristic hwaeum = upsertChar(m, "pc-india-f-hwaeum", "화음", 1, null);
        seedLevels(hwaeum, "아군 최소공격력",     new float[]{10, 20, 30, 40, 50}, StatType.MIN_POWER);
        seedLevels(hwaeum, "아군 최대공격력",     new float[]{10, 20, 30, 40, 50}, StatType.MAX_POWER);
        seedLevels(hwaeum, "적군 최소공격력 감소", new float[]{ -5, -7, -10, -15, -20}, StatType.MIN_POWER);
        seedLevels(hwaeum, "적군 최대공격력 감소", new float[]{ -5, -7, -10, -15, -20}, StatType.MAX_POWER);

        // 불협화음 (상위 point=2, required=화음)
        MercenaryCharacteristic bulhyeophaeum = upsertChar(m, "pc-india-f-bulhyeophaeum", "불협화음", 2, "pc-india-f-hwaeum");
        seedLevels(bulhyeophaeum, "찬가 데미지",   new float[]{10, 20, 30, 40, 50}, null);
        seedLevelsSec(bulhyeophaeum, "찬가 유지시간", new float[]{ 1,  2,  3,  4,  5});

        // 리듬 (하위 point=1)
        MercenaryCharacteristic rideum = upsertChar(m, "pc-india-f-rideum", "리듬", 1, null);
        seedLevels(rideum, "정화 회복량", new float[]{10, 20, 30, 40, 50}, null);

        // 박자 (상위 point=2, required=리듬)
        MercenaryCharacteristic bakja = upsertChar(m, "pc-india-f-bakja", "박자", 2, "pc-india-f-rideum");
        seedLevelsSec(bakja, "상태 면역", new float[]{1, 2, 3, 4, 5});
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Mercenary upsertMercenary(String name, Nation nation, Nature nature) {
        return mercenaryRepository.findByName(name).orElseGet(() ->
                mercenaryRepository.save(Mercenary.builder()
                        .name(name)
                        .category(MercenaryCategory.PROTAGONIST)
                        .mercenaryType(MercenaryType.MAIN_CHARACTER)
                        .nation(nation)
                        .nature(nature)
                        .comingSoon(false)
                        .build()));
    }

    private void upsertDetail(Mercenary mercenary, Nation nation, Gender gender) {
        if (detailRepository.findByMercenaryId(mercenary.getId()).isPresent()) return;
        detailRepository.save(PlayerCharacterDetail.builder()
                .mercenary(mercenary)
                .nation(nation)
                .jobType(JobType.SECOND)
                .gender(gender)
                .build());
    }

    private void upsertStats(Mercenary m, int str, int dex, int vit, int intel,
                              int hitRes, int magRes, int elemVal, int sight) {
        upsertStat(m, StatType.STRENGTH,           str);
        upsertStat(m, StatType.DEXTERITY,          dex);
        upsertStat(m, StatType.VITALITY,           vit);
        upsertStat(m, StatType.INTELLECT,          intel);
        upsertStat(m, StatType.HITTING_RESISTANCE, hitRes);
        upsertStat(m, StatType.MAGIC_RESISTANCE,   magRes);
        upsertStat(m, StatType.ELEMENT_VALUE,      elemVal);
        upsertStat(m, StatType.SIGHT,              sight);
    }

    private void upsertStat(Mercenary m, StatType type, int value) {
        mercenaryStatRepository.findByMercenaryIdAndStatKey(m.getId(), type)
                .ifPresentOrElse(
                        s -> s.updateValue(value),
                        () -> mercenaryStatRepository.save(MercenaryStat.builder()
                                .mercenary(m).statKey(type).statValue(value).build())
                );
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
                    characteristic.getId(), label, level).isPresent()) continue;
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
                    characteristic.getId(), label, level).isPresent()) continue;
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
                    characteristic.getId(), label, level).isPresent()) continue;
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

    /** 단위 없는 flat 수치 저장 — RESIST_PIERCE, SKILL_RANGE 등 */
    private void seedLevelsFlat(MercenaryCharacteristic characteristic, String label,
                                 float[] values, StatType statType) {
        for (int i = 0; i < values.length; i++) {
            int level = i + 1;
            float value = values[i];
            if (levelRepository.findByCharacteristicIdAndLabelAndLevel(
                    characteristic.getId(), label, level).isPresent()) continue;
            int intVal = (int) value;
            String amount = intVal == value ? String.valueOf(intVal) : String.valueOf(value);
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
