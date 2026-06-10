package org.example.gersangtrade.watchlist.service;

import org.example.gersangtrade.domain.user.enums.SetComposition;
import org.example.gersangtrade.listing.service.SetTitleGenerator;

import java.util.Objects;

/**
 * SET 관심 항목과 실제 매물·구매 희망의 구성·주술 일치 여부를 판단하는 유틸리티.
 * 1차 DB 필터(setId 일치) 이후 2차 앱 레벨 필터로 사용된다.
 */
public final class SetWatchMatcher {

    private SetWatchMatcher() {}

    /**
     * 판매 번들의 WatchInfo가 관심 항목의 composition·ritualCount·ritualMark와 일치하는지 판단.
     *
     * @param watchComposition  관심 항목 구성
     * @param watchRitualCount  관심 항목 주술 수 (0=주술 무관)
     * @param watchRitualMark   관심 항목 주술 마크 (null=주술 없는 구성만 허용)
     * @param bundleInfo        번들 라인에서 추출한 WatchInfo (null이면 불일치)
     * @return 일치하면 true
     */
    public static boolean matchesSell(
            SetComposition watchComposition,
            int watchRitualCount,
            String watchRitualMark,
            SetTitleGenerator.WatchInfo bundleInfo) {

        if (bundleInfo == null) {
            return false;
        }
        if (bundleInfo.composition() != watchComposition) {
            return false;
        }
        // watchRitualCount=0, watchRitualMark=null → 주술 없는 구성만 허용
        if (watchRitualCount == 0 && watchRitualMark == null) {
            return bundleInfo.ritualCount() == 0;
        }
        // watchRitualMark 존재 → 마크와 수량 모두 일치해야 함
        return bundleInfo.ritualCount() == watchRitualCount
                && Objects.equals(bundleInfo.mark(), watchRitualMark);
    }

    /**
     * 구매 희망 등록글의 WatchInfo(displayName 구성)가 관심 SET와 일치하는지 판단.
     * 판매 매칭({@link #matchesSell})과 동일한 composition·주술 규칙을 적용한다.
     */
    public static boolean matchesBuy(
            SetComposition watchComposition,
            int watchRitualCount,
            String watchRitualMark,
            SetTitleGenerator.WatchInfo wantedInfo) {

        return matchesSell(watchComposition, watchRitualCount, watchRitualMark, wantedInfo);
    }
}
