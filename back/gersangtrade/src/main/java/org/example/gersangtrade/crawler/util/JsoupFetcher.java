package org.example.gersangtrade.crawler.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Jsoup 기반 HTTP 패치 유틸리티.
 *
 * <p>crwaling 공통 설정을 적용한다:
 * <ul>
 *   <li>User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)</li>
 *   <li>요청 딜레이: 1~2초 (서버 부하 방지)</li>
 *   <li>타임아웃: 10초</li>
 *   <li>재시도: 최대 3회 (IOException 발생 시)</li>
 * </ul>
 *
 * <p>geota.co.kr 및 gerniverse.app 모두 robots.txt에서 Allow: / 이므로 정적 HTML 파싱이 허용된다.
 */
@Slf4j
@Component
public class JsoupFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_RETRY = 3;
    private static final long DELAY_MIN_MS = 1_000L;
    private static final long DELAY_MAX_MS = 2_000L;

    /**
     * HTML 문서 패치.
     * IOException 발생 시 최대 3회까지 재시도하며, 요청 사이에 1~2초 딜레이를 삽입한다.
     *
     * @param url 패치할 URL
     * @return 파싱된 Jsoup Document
     * @throws IOException 최대 재시도 횟수 초과 시
     */
    public Document fetch(String url) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                applyDelay(url, attempt);
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .get();
            } catch (IOException e) {
                lastException = e;
                log.warn("크롤링 실패 (시도 {}/{}): {} — {}", attempt, MAX_RETRY, url, e.getMessage());
            }
        }

        throw new IOException("크롤링 최대 재시도 횟수 초과: " + url, lastException);
    }

    /**
     * JSON API 응답 패치.
     * ignoreContentType 옵션으로 JSON body를 문자열로 반환한다.
     *
     * @param url     API URL
     * @param referer 요청 출처 페이지 URL (서버가 Referer로 컨텍스트를 판별하는 경우 필수)
     * @return 응답 body 문자열
     * @throws IOException 패치 실패 시
     */
    public String fetchBody(String url, String referer) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                applyDelay(url, attempt);
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .ignoreContentType(true)
                        .header("Referer", referer)
                        .header("Origin", "https://geota.co.kr")
                        .header("Accept", "application/json, */*")
                        .header("Accept-Language", "ko-KR,ko;q=0.9")
                        .execute()
                        .body();
            } catch (IOException e) {
                lastException = e;
                log.warn("API 패치 실패 (시도 {}/{}): {} — {}", attempt, MAX_RETRY, url, e.getMessage());
            }
        }

        throw new IOException("API 패치 최대 재시도 횟수 초과: " + url, lastException);
    }

    /** fetchBody(url, referer) 의 referer 생략 버전 */
    public String fetchBody(String url) throws IOException {
        return fetchBody(url, "https://geota.co.kr/");
    }

    /**
     * 바이너리 데이터(이미지) 패치.
     * ignoreContentType 옵션으로 이미지 URL에서 바이트 배열을 받아온다.
     *
     * @param url 이미지 URL
     * @return 이미지 바이트 배열
     * @throws IOException 패치 실패 시
     */
    public byte[] fetchBytes(String url) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                applyDelay(url, attempt);
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .ignoreContentType(true)
                        .execute()
                        .bodyAsBytes();
            } catch (IOException e) {
                lastException = e;
                log.warn("이미지 패치 실패 (시도 {}/{}): {} — {}", attempt, MAX_RETRY, url, e.getMessage());
            }
        }

        throw new IOException("이미지 패치 최대 재시도 횟수 초과: " + url, lastException);
    }

    /** 요청 딜레이 적용. 첫 번째 시도도 딜레이를 넣어 서버 부하를 줄인다 */
    private void applyDelay(String url, int attempt) {
        try {
            long delay = ThreadLocalRandom.current().nextLong(DELAY_MIN_MS, DELAY_MAX_MS + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("딜레이 대기 중 인터럽트 발생: {}", url);
        }
    }
}
