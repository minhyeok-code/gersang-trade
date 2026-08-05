package org.example.gersangtrade.guide.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.deck.repository.UserDeckMemberRepository;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.domain.deck.UserDeck;
import org.example.gersangtrade.domain.guide.Guide;
import org.example.gersangtrade.guide.dto.response.GuideStepResponse;
import org.example.gersangtrade.guide.dto.response.GuideSummaryResponse;
import org.example.gersangtrade.guide.repository.GuideRepository;
import org.example.gersangtrade.guide.repository.GuideStepRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 가이드 원본 조회 서비스 (읽기 전용).
 * 공개 가이드 목록·상세 조회와, 유저 덱 기반 추천 가이드 선정을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideService {

    private final GuideRepository guideRepository;
    private final GuideStepRepository guideStepRepository;
    private final UserDeckRepository userDeckRepository;
    private final UserDeckMemberRepository userDeckMemberRepository;

    /** 공개된 전체 가이드 목록. */
    public List<GuideSummaryResponse> getPublishedGuides() {
        return guideRepository.findByPublishedTrue()
                .stream().map(GuideSummaryResponse::of).toList();
    }

    /** 공개된 단일 가이드. */
    public GuideSummaryResponse getPublishedGuide(Long guideId) {
        Guide guide = guideRepository.findByIdAndPublishedTrue(guideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "가이드를 찾을 수 없습니다: " + guideId));
        return GuideSummaryResponse.of(guide);
    }

    /** 가이드 원본 스텝 목록 (순번순) — 시작 전 미리보기. */
    public List<GuideStepResponse> getGuideSteps(Long guideId) {
        return guideStepRepository.findByGuideIdOrderByStepOrderAsc(guideId)
                .stream().map(GuideStepResponse::of).toList();
    }

    /**
     * 유저 덱 기반 추천 가이드.
     *
     * <p>매칭 방식(휴리스틱): 유저의 <b>모든 덱</b>에 편성된 용병들을 모아,
     * 그 용병을 대상(targetMercenary)으로 하는 공개 가이드를 반환한다.
     * 주력 용병을 단일 지정하는 플래그가 없어 덱 내 모든 용병을 기준으로 삼는다.
     * (활성 덱만 보면 활성이 아닌 덱의 사천왕이 누락되므로 전체 덱을 대상으로 한다.)
     * 덱이 없거나 매칭 가이드가 없으면 빈 목록을 반환한다(호출부에서 fallback 처리).
     */
    public List<GuideSummaryResponse> recommendGuidesForUser(Long userId) {
        List<UserDeck> decks = userDeckRepository.findByUserId(userId);
        if (decks.isEmpty()) {
            return List.of();
        }
        List<Long> mercenaryIds = decks.stream()
                .flatMap(deck -> userDeckMemberRepository.findByDeckIdWithMercenary(deck.getId()).stream())
                .map(member -> member.getMercenary().getId())
                .distinct()
                .toList();
        if (mercenaryIds.isEmpty()) {
            return List.of();
        }
        return guideRepository.findByPublishedTrueAndTargetMercenary_IdIn(mercenaryIds)
                .stream().map(GuideSummaryResponse::of).toList();
    }
}
