package org.example.gersangtrade.hunt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 사냥 허브·클리어타임 운영 설정.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gersang.hunt")
public class HuntHubProperties {

    /**
     * 클리어타임 저장 시 공개/비공개 선택 UI·API 분기 활성화 여부.
     * false(초기 운영): 요청값 무시, 항상 isPublic=true로 저장.
     */
    private boolean clearTimePublicToggleEnabled = false;

    /** 사냥 허브 해금에 필요한 서로 다른 몬스터 클리어타임 기록 수 */
    private int unlockRequiredDistinctMonsters = 3;
}
