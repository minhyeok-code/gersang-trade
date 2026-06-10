package org.example.gersangtrade.hunt.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.DpsRequest;
import org.example.gersangtrade.calculator.dto.request.MemberDpsInput;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.service.DeckResistanceTypeResolver;
import org.example.gersangtrade.calculator.service.DpsCalculatorService;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.deck.repository.UserDeckMemberRepository;
import org.example.gersangtrade.deck.repository.UserDeckRepository;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.deck.UserDeck;
import org.example.gersangtrade.domain.deck.UserDeckMember;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.domain.user.UserClearTime;
import org.example.gersangtrade.domain.user.UserClearTimeRepository;
import org.example.gersangtrade.domain.user.UserRepository;
import org.example.gersangtrade.domain.user.enums.UserStatus;
import org.example.gersangtrade.hunt.config.HuntHubProperties;
import org.example.gersangtrade.user.dto.request.ClearTimeRequest;
import org.example.gersangtrade.user.dto.response.ClearTimeResponse;
import org.example.gersangtrade.user.util.ExpGradeCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 클리어타임 저장·EXP 지급 서비스.
 * 사냥 허브 공개 API는 별도 HuntHubService(예정)에서 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ClearTimeService {

    /** 클리어타임 1건당 지급 EXP */
    public static final long CLEAR_TIME_EXP = 5L;

    /**
     * 클리어타임 EXP 일일 지급 최대 횟수 (거래 EXP와 별도·독립).
     * 운영 전환 시 주석 해제 — {@code docs/clear-time-hunt-hub.ko.md} §3.3.1
     */
    // private static final int DAILY_CLEAR_TIME_EXP_GRANT_LIMIT = 3;

    /** 클리어타임 허용 최소값 (초, 포함) */
    public static final int MIN_CLEAR_TIME_SECONDS = 6;

    /** 클리어타임 허용 최대값 (초, 포함) — 27초 이상은 거부 */
    public static final int MAX_CLEAR_TIME_SECONDS = 26;

    /** 운영 전환 시 §3.3.1 — 일 3회 상한과 함께 주석 해제 */
    // private static final ZoneId EXP_DAY_ZONE = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final MonsterRepository monsterRepository;
    private final UserDeckRepository deckRepository;
    private final UserDeckMemberRepository deckMemberRepository;
    private final UserClearTimeRepository clearTimeRepository;
    private final HuntHubProperties huntHubProperties;
    private final DpsCalculatorService dpsCalculatorService;
    private final DeckSnapshotBuilderService deckSnapshotBuilderService;

    /**
     * 클리어타임 저장 및 조건부 EXP 지급.
     *
     * @param userId  유저 ID
     * @param request 클리어타임 저장 요청
     * @return 저장된 클리어타임 + 지급된 EXP (미지급 시 0)
     */
    @Transactional
    public ClearTimeResponse saveClearTime(Long userId, ClearTimeRequest request) {
        validateClearTimeSeconds(request.clearTimeSeconds());

        User user = loadActiveUser(userId);
        Monster monster = monsterRepository.findById(request.monsterId())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 몬스터입니다."));
        validateDeckOwnership(userId, request.deckId());

        List<UserDeckMember> members = deckMemberRepository.findByDeckIdWithMercenary(request.deckId());
        if (members.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "덱에 용병이 없습니다.");
        }

        List<MemberDpsInput> memberInputs = members.stream()
                .map(m -> new MemberDpsInput(
                        m.getId(), m.getLevel(), m.getBonusTarget(), m.getBonusAmount()))
                .toList();

        DpsResponse dps = dpsCalculatorService.calculate(
                new DpsRequest(request.deckId(), request.monsterId(), null, memberInputs));

        var snapshotResult = deckSnapshotBuilderService.buildOrReuse(
                userId,
                request.deckId(),
                DeckResistanceTypeResolver.resolve(members),
                memberInputs,
                dps.memberResults());

        boolean duplicateContent = clearTimeRepository.existsByUserIdAndMonsterIdAndDeckSnapshot_ContentHash(
                userId, monster.getId(), snapshotResult.contentHash());
        long expEarned = resolveExpEarned(userId, duplicateContent);

        UserClearTime clearTime = UserClearTime.builder()
                .user(user)
                .monster(monster)
                .deckId(request.deckId())
                .deckSnapshot(snapshotResult.snapshot())
                .totalResistPierce(dps.totalResistPierce())
                .totalElementPierce(dps.totalElementPierce())
                .rawDps(dps.rawTotalDps())
                .adjustDps(dps.adjustTotalDps())
                .finalDps(dps.totalDps())
                .resistAfterDebuff(dps.resistAfterDebuff())
                .effectiveMonsterElement(dps.effectiveMonsterElement())
                .resistPassRate(dps.resistPassRate())
                .clearTimeSeconds(request.clearTimeSeconds())
                .isPublic(resolveIsPublic(request.isPublic()))
                .expGranted(expEarned > 0)
                .build();
        clearTimeRepository.save(clearTime);

        if (expEarned > 0) {
            ExpGradeCalculator.GradeAndStep result =
                    ExpGradeCalculator.calculate(user.getTotalExp(), expEarned);
            user.applyExp(expEarned, result.grade(), result.step());
        }

        return ClearTimeResponse.of(clearTime, expEarned);
    }

    private void validateClearTimeSeconds(int clearTimeSeconds) {
        if (clearTimeSeconds < MIN_CLEAR_TIME_SECONDS || clearTimeSeconds > MAX_CLEAR_TIME_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "클리어타임은 " + MIN_CLEAR_TIME_SECONDS + "초 이상 "
                            + (MAX_CLEAR_TIME_SECONDS + 1) + "초 미만이어야 합니다.");
        }
    }

    private void validateDeckOwnership(Long userId, Long deckId) {
        UserDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "덱을 찾을 수 없습니다."));
        if (!deck.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 덱만 사용할 수 있습니다.");
        }
    }

    /**
     * 공개 여부 결정. 토글 비활성(초기 운영) 시 항상 true.
     */
    private boolean resolveIsPublic(Boolean requested) {
        if (!huntHubProperties.isClearTimePublicToggleEnabled()) {
            return true;
        }
        return requested == null || requested;
    }

    /**
     * 클리어타임 EXP 지급 여부 판단.
     * 거래 확정 EXP와 경로·상한이 분리되어 있다.
     */
    private long resolveExpEarned(Long userId, boolean duplicateContentHash) {
        if (duplicateContentHash) {
            return 0L;
        }
        // [운영] 일일 EXP 지급 상한 3회/일(KST) — 개발 중 비활성. 활성화: docs/clear-time-hunt-hub.ko.md §3.3.1
        // LocalDateTime startOfDay = LocalDate.now(EXP_DAY_ZONE).atStartOfDay();
        // long todayExpGrants = clearTimeRepository
        //         .countByUserIdAndRecordedAtGreaterThanEqualAndExpGrantedTrue(userId, startOfDay);
        // if (todayExpGrants >= DAILY_CLEAR_TIME_EXP_GRANT_LIMIT) {
        //     return 0L;
        // }
        return CLEAR_TIME_EXP;
    }

    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("탈퇴한 사용자입니다.");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("차단된 사용자입니다.");
        }
        return user;
    }
}
