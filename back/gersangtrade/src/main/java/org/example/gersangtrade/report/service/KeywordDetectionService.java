package org.example.gersangtrade.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.domain.report.KeywordBlacklist;
import org.example.gersangtrade.domain.report.Report;
import org.example.gersangtrade.domain.report.enums.ReportReason;
import org.example.gersangtrade.domain.report.enums.ReportStatus;
import org.example.gersangtrade.domain.report.enums.ReportTargetType;
import org.example.gersangtrade.domain.report.enums.ReporterType;
import org.example.gersangtrade.report.repository.KeywordBlacklistRepository;
import org.example.gersangtrade.report.repository.ReportRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 현금거래 의심 키워드·패턴 자동 감지 서비스.
 *
 * <p>동작 방식 (Soft 감지):
 * <ol>
 *   <li>메시지 전송 시 {@link #detect}를 호출한다.</li>
 *   <li>활성 패턴 목록을 Spring Cache({@code keywordBlacklist})에서 읽는다.</li>
 *   <li>일치 패턴이 발견되면 SYSTEM 신고(Report)를 자동 생성한다.</li>
 *   <li>메시지 전송 자체는 허용된다 — 거부 없이 신고만 남긴다.</li>
 * </ol>
 *
 * <p>캐시는 관리자가 키워드를 수정할 때 {@link #evictCache}로 무효화한다.
 * 상세 정책: docs/report-system.ko.md 2-3절 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordDetectionService {

    private static final String CACHE_NAME = "keywordBlacklist";

    private final KeywordBlacklistRepository keywordBlacklistRepository;
    private final ReportRepository reportRepository;

    /**
     * 메시지 내용에서 블랙리스트 키워드·패턴을 검사한다.
     * 일치 패턴이 있으면 SYSTEM 신고를 자동 생성한다.
     *
     * <p>메시지 전송은 항상 허용된다 (Soft 감지).
     *
     * @param content     검사할 메시지 내용
     * @param messageId   저장된 ChatMessage ID (신고 targetId)
     * @param chatRoomId  채팅방 ID (신고에 기록)
     */
    @Transactional
    public void detect(String content, Long messageId, Long chatRoomId) {
        List<KeywordBlacklist> patterns = loadActivePatterns();

        for (KeywordBlacklist kw : patterns) {
            if (matches(kw, content)) {
                createSystemReport(messageId, chatRoomId, kw.getPattern());
                // 첫 번째 일치 패턴으로 신고 1건 생성 후 종료
                // (동일 메시지에 복수 패턴이 일치해도 신고 중복 생성 방지)
                return;
            }
        }
    }

    /**
     * 활성 키워드 목록을 캐시에서 로드한다.
     * 캐시 미스 시 DB에서 조회하여 캐시에 저장한다.
     */
    @Cacheable(CACHE_NAME)
    @Transactional(readOnly = true)
    public List<KeywordBlacklist> loadActivePatterns() {
        return keywordBlacklistRepository.findByActiveTrue();
    }

    /**
     * 키워드 캐시를 무효화한다.
     * 관리자가 키워드를 추가·수정·비활성화할 때 호출한다.
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void evictCache() {
        log.info("키워드 블랙리스트 캐시 무효화");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /** 키워드 또는 정규식 패턴 매칭 */
    private boolean matches(KeywordBlacklist kw, String content) {
        try {
            if (kw.isRegex()) {
                return Pattern.compile(kw.getPattern()).matcher(content).find();
            } else {
                return content.contains(kw.getPattern());
            }
        } catch (Exception e) {
            // 잘못된 정규식 등 예외 발생 시 해당 패턴을 건너뜀
            log.warn("키워드 패턴 검사 오류 (id={}, pattern={}): {}",
                    kw.getId(), kw.getPattern(), e.getMessage());
            return false;
        }
    }

    /** SYSTEM 자동 신고 생성 */
    private void createSystemReport(Long messageId, Long chatRoomId, String matchedPattern) {
        Report report = Report.builder()
                .reporterType(ReporterType.SYSTEM)
                .reporter(null)
                .targetType(ReportTargetType.CHAT_MESSAGE)
                .targetId(messageId)
                .reasonCategory(ReportReason.CASH_TRADE)
                .description("키워드 자동 감지: [" + matchedPattern + "]")
                .chatRoomId(chatRoomId)
                .build();
        reportRepository.save(report);
        log.info("SYSTEM 자동 신고 생성 — messageId={}, chatRoomId={}, pattern={}",
                messageId, chatRoomId, matchedPattern);
    }
}
