package org.example.gersangtrade.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.example.gersangtrade.domain.guide.enums.GuidePhase;

import java.util.List;

/**
 * 주입할 가이드 한 건.
 * targetMercenaryName은 용병 이름으로 매칭, nextGuideTitle은 같은 요청 내 다른 가이드 제목과 연결한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuideImport(
        String title,
        GuidePhase phase,
        String version,
        String author,
        String targetMercenaryName,
        String nextGuideTitle,
        List<GuideStepImport> steps
) {}
