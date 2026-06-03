package org.example.gersangtrade.admin.dto.response;

import org.example.gersangtrade.domain.catalog.SkillCoefficient;
import org.example.gersangtrade.domain.catalog.enums.SkillType;

import java.util.ArrayList;
import java.util.List;

/** 스킬 계수 이슈 항목 응답 */
public record SkillCoefficientIssueResponse(
        Long id,
        String rowId,
        String ownerType,
        String ownerName,
        String skillName,
        List<String> issues
) {
    public enum Issue {
        /** skillType이 null — 측정 기준 자체가 불명 */
        MISSING_SKILL_TYPE,
        /** skillType은 있으나 측정값(castsPerSecond/tickIntervalMs) 미입력 */
        UNMEASURED,
        /** 6개 계수(str/dex/vit/int/atk/lvl)가 모두 0 — 계수 미입력 의심 */
        ALL_COEFS_ZERO,
        /** hitCount가 1 미만 — 비정상값 */
        HIT_COUNT_ZERO
    }

    public static SkillCoefficientIssueResponse of(SkillCoefficient sc, List<Issue> issues) {
        String ownerType;
        String ownerName;
        String skillName;

        if (sc.isMercenarySkill()) {
            ownerType = "MERCENARY";
            ownerName = sc.getMercenarySkill().getMercenary().getName();
            skillName = sc.getMercenarySkill().getSkillName();
        } else {
            ownerType = "ITEM";
            // 스킬이 여러 아이템에 공유되므로 아이템명 대신 스킬명을 owner로 사용
            ownerName = sc.getItemSkill().getSkillName();
            skillName = sc.getItemSkill().getSkillName();
        }

        return new SkillCoefficientIssueResponse(
                sc.getId(),
                sc.getRowId(),
                ownerType,
                ownerName,
                skillName,
                issues.stream().map(Issue::name).toList()
        );
    }

    /** 이슈 목록 전체를 감지해 반환. 이슈 없으면 빈 리스트. */
    public static List<Issue> detect(SkillCoefficient sc) {
        List<Issue> found = new ArrayList<>();

        if (sc.getSkillType() == null) {
            found.add(Issue.MISSING_SKILL_TYPE);
        } else {
            boolean unmeasured = sc.getSkillType() == SkillType.INSTANT
                    ? sc.getCastsPerSecond() == null
                    : sc.getTickIntervalMs() == null;
            if (unmeasured) found.add(Issue.UNMEASURED);
        }

        if (sc.getCoefStr() == 0f && sc.getCoefDex() == 0f && sc.getCoefVit() == 0f
                && sc.getCoefInt() == 0f && sc.getCoefAtk() == 0f && sc.getCoefLvl() == 0f) {
            found.add(Issue.ALL_COEFS_ZERO);
        }

        if (sc.getHitCount() < 1) {
            found.add(Issue.HIT_COUNT_ZERO);
        }

        return found;
    }
}
