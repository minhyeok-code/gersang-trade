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
 * <p>gerniverse CDN에서 이미지를 다운로드하고 S3에 업로드한 뒤 S3 URL을 반환한다.
 *
 * <p>저장 경로:
 * <pre>
 * 아이템:  s3://{bucket}/items/{imageKey}.webp
 * 용병:    s3://{bucket}/mercenaries/{imageKey}.webp
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private static final String GERNIVERSE_IMAGE_BASE =
            "https://images.gerniverse.app/tr:cm-pad_resize,w-120,h-120,f-auto,q-80/";

    private final S3Client s3Client;
    private final JsoupFetcher jsoupFetcher;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    /** true이면 S3 업로드를 건너뛰고 null 반환 — 로컬 개발 환경 전용 */
    @Value("${crawler.s3.skip:false}")
    private boolean s3Skip;

    /**
     * 아이템 이미지 다운로드 → S3 업로드 → URL 반환.
     *
     * @param imageKey gerniverse JSON-LD에서 추출한 이미지 키 (예: "item/weapon/doll/gkstk...")
     * @return S3 URL. 이미지 없거나 실패 시 null
     */
    public String uploadItemImage(String imageKey) {
        return upload(imageKey, "items/" + imageKey + ".webp");
    }

    /**
     * 용병 이미지 다운로드 → S3 업로드 → URL 반환.
     *
     * @param imageKey gerniverse JSON-LD에서 추출한 이미지 키 (예: "thumbnail/myeong-kings/...")
     * @return S3 URL. 이미지 없거나 실패 시 null
     */
    public String uploadMercenaryImage(String imageKey) {
        return upload(imageKey, "mercenaries/" + imageKey + ".webp");
    }

    private String upload(String imageKey, String s3Key) {
        if (imageKey == null || imageKey.isBlank()) return null;
        if (s3Skip) {
            log.debug("S3 업로드 skip (로컬 환경): {}", imageKey);
            return null;
        }

        String sourceUrl = GERNIVERSE_IMAGE_BASE + imageKey + ".webp";
        try {
            byte[] imageBytes = jsoupFetcher.fetchBytes(sourceUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("이미지 바이트가 비어있음: {}", sourceUrl);
                return null;
            }

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType("image/webp")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(imageBytes));

            String s3Url = s3BaseUrl + "/" + s3Key;
            log.info("S3 이미지 업로드 완료: {} → {}", sourceUrl, s3Url);
            return s3Url;

        } catch (IOException e) {
            log.error("이미지 업로드 실패 (key={}): {}", imageKey, e.getMessage());
            return null;
        }
    }
}
