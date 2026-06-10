package org.example.gersangtrade.hunt.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.domain.hunt.ClearTimeRecordStatus;
import org.example.gersangtrade.domain.user.UserClearTimeRepository;
import org.example.gersangtrade.hunt.config.HuntHubProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사냥 허브 해금(기여 게이트) 판별 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HuntHubAccessService {

    private final UserClearTimeRepository clearTimeRepository;
    private final HuntHubProperties huntHubProperties;

    /** 유효 기록 기준 서로 다른 몬스터 수 (ACTIVE, hidden 제외) */
    public int countDistinctMonstersForUnlock(Long userId) {
        return (int) clearTimeRepository.countDistinctMonsterIdByUserIdAndStatus(
                userId, ClearTimeRecordStatus.ACTIVE);
    }

    public int requiredDistinctMonsters() {
        return huntHubProperties.getUnlockRequiredDistinctMonsters();
    }

    public boolean isUnlocked(Long userId) {
        return countDistinctMonstersForUnlock(userId) >= requiredDistinctMonsters();
    }

    /**
     * 미해금 시 403. 타인 스냅샷·랭킹 등 보호 API에서 호출한다.
     */
    public void requireUnlocked(Long userId) {
        if (!isUnlocked(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "사냥 허브는 서로 다른 몬스터 클리어타임을 "
                            + requiredDistinctMonsters() + "종 이상 기록한 후 이용할 수 있습니다.");
        }
    }
}
