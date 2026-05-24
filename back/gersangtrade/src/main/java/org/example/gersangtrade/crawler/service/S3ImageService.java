package org.example.gersangtrade.crawler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gersangtrade.crawler.util.JsoupFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

/**
 * S3 이미지 업로드 서비스.
 *
 * <p>이미지를 다운로드하고 S3에 업로드한 뒤 S3 URL을 반환한다.
 * gerniverse CDN 키 기반 메서드와 거상짱 절대 URL 기반 메서드를 모두 제공한다.
 *
 * <p>저장 경로:
 * <pre>
 * 아이템(gerniverse):  s3://{bucket}/items/{imageKey}.webp
 * 용병(gerniverse):    s3://{bucket}/mercenaries/{imageKey}.webp
 * 아이템(거상짱):       s3://{bucket}/items/{relativePath}
 * 용병(거상짱):         s3://{bucket}/mercenaries/{relativePath}
 * </pre>
 *
 * <p>{@code crawler.s3.skip=true}이면 S3 업로드를 건너뛴다.
 * 거상짱 URL 기반 메서드는 skip 시 원본 URL을 그대로 반환해 로컬 환경에서도 이미지 확인이 가능하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private static final String GERNIVERSE_IMAGE_BASE =
            "https://images.gerniverse.app/tr:cm-pad_resize,w-120,h-120,f-auto,q-80/";

    private static final String GERSANGJJANG_ITEM_BASE =
            "https://www.gersangjjang.com/item/";
    private static final String GERSANGJJANG_YONGBING_BASE =
            "https://www.gersangjjang.com/yongbing/";

    private final S3Client s3Client;
    private final JsoupFetcher jsoupFetcher;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    /** true이면 S3 업로드를 건너뜀 — 로컬 개발 환경 전용 */
    @Value("${crawler.s3.skip:false}")
    private boolean s3Skip;

    // ── gerniverse 키 기반 (기존 메서드) ───────────────────────────────────────

    /**
     * gerniverse 아이템 이미지 키 → S3 업로드.
     *
     * @param imageKey gerniverse JSON-LD 이미지 키 (예: "item/weapon/doll/gkstk...")
     * @return S3 URL. skip이거나 실패 시 null
     */
    public String uploadItemImage(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return null;
        if (s3Skip) {
            log.debug("S3 업로드 skip (로컬 환경): {}", imageKey);
            return null;
        }
        return uploadBytes(GERNIVERSE_IMAGE_BASE + imageKey + ".webp",
                "items/" + imageKey + ".webp", "image/webp");
    }

    /**
     * gerniverse 용병 이미지 키 → S3 업로드.
     *
     * @param imageKey gerniverse JSON-LD 이미지 키 (예: "thumbnail/myeong-kings/...")
     * @return S3 URL. skip이거나 실패 시 null
     */
    public String uploadMercenaryImage(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return null;
        if (s3Skip) {
            log.debug("S3 업로드 skip (로컬 환경): {}", imageKey);
            return null;
        }
        return uploadBytes(GERNIVERSE_IMAGE_BASE + imageKey + ".webp",
                "mercenaries/" + imageKey + ".webp", "image/webp");
    }

    // ── 거상짱 절대 URL 기반 ───────────────────────────────────────────────────

    /**
     * 거상짱 아이템 이미지 절대 URL → S3 업로드.
     * s3Skip=true이면 원본 URL을 그대로 반환한다 (로컬 개발에서 이미지 확인 가능).
     *
     * @param sourceUrl 거상짱 이미지 절대 URL
     * @return S3 URL. skip이면 sourceUrl 반환. 실패 시 null
     */
    public String uploadItemImageFromUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) return null;
        if (s3Skip) {
            log.debug("S3 업로드 skip — 원본 URL 반환: {}", sourceUrl);
            return sourceUrl;
        }
        String relative = sourceUrl.startsWith(GERSANGJJANG_ITEM_BASE)
                ? sourceUrl.substring(GERSANGJJANG_ITEM_BASE.length())
                : sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1);
        return uploadBytes(sourceUrl, "items/" + relative, detectContentType(sourceUrl));
    }

    /**
     * 거상짱 용병 이미지 절대 URL → S3 업로드.
     * s3Skip=true이면 원본 URL을 그대로 반환한다 (로컬 개발에서 이미지 확인 가능).
     *
     * @param sourceUrl 거상짱 이미지 절대 URL
     * @return S3 URL. skip이면 sourceUrl 반환. 실패 시 null
     */
    public String uploadMercenaryImageFromUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) return null;
        if (s3Skip) {
            log.debug("S3 업로드 skip — 원본 URL 반환: {}", sourceUrl);
            return sourceUrl;
        }
        String relative = sourceUrl.startsWith(GERSANGJJANG_YONGBING_BASE)
                ? sourceUrl.substring(GERSANGJJANG_YONGBING_BASE.length())
                : sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1);
        return uploadBytes(sourceUrl, "mercenaries/" + relative, detectContentType(sourceUrl));
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private String uploadBytes(String sourceUrl, String s3Key, String contentType) {
        try {
            byte[] imageBytes = jsoupFetcher.fetchBytes(sourceUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("이미지 바이트가 비어있음: {}", sourceUrl);
                return null;
            }

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(imageBytes));

            String s3Url = s3BaseUrl + "/" + s3Key;
            log.info("S3 이미지 업로드 완료: {} → {}", sourceUrl, s3Url);
            return s3Url;

        } catch (IOException e) {
            log.error("이미지 업로드 실패 (url={}): {}", sourceUrl, e.getMessage());
            return null;
        }
    }

    private static String detectContentType(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/webp";
    }
}
