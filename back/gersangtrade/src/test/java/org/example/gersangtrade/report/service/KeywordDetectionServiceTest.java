package org.example.gersangtrade.report.service;

import org.example.gersangtrade.domain.report.KeywordBlacklist;
import org.example.gersangtrade.domain.report.Report;
import org.example.gersangtrade.domain.report.enums.ReportReason;
import org.example.gersangtrade.domain.report.enums.ReportTargetType;
import org.example.gersangtrade.domain.report.enums.ReporterType;
import org.example.gersangtrade.domain.user.User;
import org.example.gersangtrade.report.repository.KeywordBlacklistRepository;
import org.example.gersangtrade.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * KeywordDetectionService 단위 테스트.
 * loadActivePatterns()의 캐시 동작은 Spring 컨테이너 없이 테스트하므로
 * 직접 stubbing으로 패턴 목록을 제공한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KeywordDetectionService")
class KeywordDetectionServiceTest {

    @Mock
    private KeywordBlacklistRepository keywordBlacklistRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private KeywordDetectionService self; // 캐시 프록시 대역

    // 직접 인스턴스 생성 (캐시 어노테이션은 프록시 없이 동작하지 않으므로
    // loadActivePatterns()를 spy로 재정의한다)
    private KeywordDetectionService service;

    @BeforeEach
    void setUp() {
        service = spy(new KeywordDetectionService(keywordBlacklistRepository, reportRepository));
    }

    // ──────────────────────────────────────────────────────────────────────
    // 단순 문자열 매칭
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("일치 키워드 포함 시 SYSTEM 신고 자동 생성")
    void detect_keywordMatch_createsReport() {
        // given
        KeywordBlacklist kw = stubPlainKeyword("현금");
        doReturn(List.of(kw)).when(service).loadActivePatterns();
        given(reportRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        service.detect("현금 거래 원합니다", 10L, 5L);

        // then
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertThat(saved.getReporterType()).isEqualTo(ReporterType.SYSTEM);
        assertThat(saved.getTargetType()).isEqualTo(ReportTargetType.CHAT_MESSAGE);
        assertThat(saved.getTargetId()).isEqualTo(10L);
        assertThat(saved.getChatRoomId()).isEqualTo(5L);
        assertThat(saved.getReasonCategory()).isEqualTo(ReportReason.CASH_TRADE);
        assertThat(saved.getDescription()).contains("현금");
    }

    @Test
    @DisplayName("일치 키워드 없을 시 신고 미생성")
    void detect_noMatch_noReport() {
        // given
        KeywordBlacklist kw = stubPlainKeyword("현금");
        doReturn(List.of(kw)).when(service).loadActivePatterns();

        // when
        service.detect("일반 메시지입니다", 10L, 5L);

        // then
        verify(reportRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 정규식 매칭
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("정규식 패턴 일치 시 신고 생성")
    void detect_regexMatch_createsReport() {
        // given
        KeywordBlacklist kw = stubRegexKeyword("\\d+만원");
        doReturn(List.of(kw)).when(service).loadActivePatterns();
        given(reportRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        service.detect("5만원에 거래해요", 20L, 3L);

        // then
        verify(reportRepository).save(any());
    }

    @Test
    @DisplayName("잘못된 정규식 패턴 — 예외 발생 없이 건너뜀")
    void detect_invalidRegex_skipped() {
        // given
        KeywordBlacklist invalidKw = stubRegexKeyword("[잘못된정규식");
        doReturn(List.of(invalidKw)).when(service).loadActivePatterns();

        // when — 예외 없이 완료되어야 함
        service.detect("메시지", 1L, 1L);

        // then
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("복수 패턴 중 첫 번째 일치 시 신고 1건만 생성")
    void detect_multiplePatterns_onlyOneReport() {
        // given
        KeywordBlacklist kw1 = stubPlainKeyword("현금");
        KeywordBlacklist kw2 = stubPlainKeyword("계좌");
        doReturn(List.of(kw1, kw2)).when(service).loadActivePatterns();
        given(reportRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        service.detect("현금 계좌 이체", 30L, 7L);

        // then
        verify(reportRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("활성 패턴이 없으면 신고 미생성")
    void detect_emptyPatterns_noReport() {
        // given
        doReturn(List.of()).when(service).loadActivePatterns();

        // when
        service.detect("어떤 메시지라도", 1L, 1L);

        // then
        verify(reportRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────────────────

    /** 단순 문자열 키워드 KeywordBlacklist mock 생성 */
    private KeywordBlacklist stubPlainKeyword(String pattern) {
        KeywordBlacklist kw = mock(KeywordBlacklist.class);
        given(kw.isRegex()).willReturn(false);
        given(kw.getPattern()).willReturn(pattern);
        return kw;
    }

    /** 정규식 패턴 KeywordBlacklist mock 생성 */
    private KeywordBlacklist stubRegexKeyword(String pattern) {
        KeywordBlacklist kw = mock(KeywordBlacklist.class);
        given(kw.isRegex()).willReturn(true);
        given(kw.getPattern()).willReturn(pattern);
        return kw;
    }
}
