package org.example.gersangtrade.home.service;

import org.example.gersangtrade.home.dto.PriceWatchResponse;
import org.springframework.stereotype.Component;

/**
 * priceWatch 캐시 저장 여부 판단.
 * SpEL unless에서 람다를 쓸 수 없어 Java 메서드로 분리한다.
 */
@Component("priceWatchCachePolicy")
public class PriceWatchCachePolicy {

    /**
     * @return true이면 캐시하지 않음 (unless 조건)
     */
    public boolean shouldNotCache(PriceWatchResponse result) {
        if (result == null || result.targets().isEmpty()) {
            return true;
        }
        return result.targets().stream().allMatch(t ->
                t.sell().count() == 0
                        && t.buy().count() == 0
                        && t.completed().count() == 0);
    }
}
