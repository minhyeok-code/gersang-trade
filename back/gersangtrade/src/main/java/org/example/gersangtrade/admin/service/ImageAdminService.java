package org.example.gersangtrade.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.admin.dto.response.GemImageTargetResponse;
import org.example.gersangtrade.admin.dto.response.ImageSyncResult;
import org.example.gersangtrade.admin.dto.response.ImageUploadResponse;
import org.example.gersangtrade.admin.dto.response.ItemImageTargetResponse;
import org.example.gersangtrade.admin.dto.response.MercenaryImageTargetResponse;
import org.example.gersangtrade.catalog.repository.GemRepository;
import org.example.gersangtrade.catalog.repository.ItemRepository;
import org.example.gersangtrade.catalog.repository.MercenaryRepository;
import org.example.gersangtrade.catalog.repository.MonsterRepository;
import org.example.gersangtrade.domain.catalog.Gem;
import org.example.gersangtrade.domain.catalog.Item;
import org.example.gersangtrade.domain.catalog.Mercenary;
import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.enums.ItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 이미지 업로드 서비스.
 * 아이템·보석·용병 이미지를 S3에 업로드하고 엔티티의 imageUrl을 갱신한다.
 * aws.access-key 미설정 시 S3Client 빈이 없어도 기동은 정상 — 업로드 요청 시 503 반환.
 */
@Service
@RequiredArgsConstructor
public class ImageAdminService {

    /** aws.access-key 미설정 환경(로컬)에서는 null. */
    @Nullable
    @Autowired(required = false)
    private S3Client s3Client;

    private final ItemRepository itemRepository;
    private final GemRepository gemRepository;
    private final MercenaryRepository mercenaryRepository;
    private final MonsterRepository monsterRepository;

    @Value("${aws.s3-bucket:}")
    private String bucket;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    // ── 아이템 ───────────────────────────────────────────────────────────────────

    /**
     * 아이템 이름·타입 필터 검색 (페이징).
     * type 생략 시 재료·장비 모두 반환한다.
     */
    @Transactional(readOnly = true)
    public Page<ItemImageTargetResponse> searchItems(ItemType type, String name, Pageable pageable) {
        String nameTrim = (name != null && !name.isBlank()) ? name.trim() : null;
        return itemRepository.findByTypeAndNameContaining(type, nameTrim, pageable)
                .map(ItemImageTargetResponse::from);
    }

    /**
     * 거래 이력(BundleLine·TradeConfirmed)이 있으나 이미지가 없는 장비 아이템 목록.
     * 이미지 등록 우선순위 파악에 사용한다.
     */
    @Transactional(readOnly = true)
    public List<ItemImageTargetResponse> missingItemImages() {
        return itemRepository.findEquipmentWithTradeAndNoImage()
                .stream().map(ItemImageTargetResponse::from).toList();
    }

    /** 아이템 이미지를 S3에 업로드하고 imageUrl을 저장한다. */
    @Transactional
    public ImageUploadResponse uploadItemImage(Long itemId, MultipartFile file) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "아이템을 찾을 수 없습니다: " + itemId));
        String imageUrl = upload(file, "images/items/" + itemId);
        item.updateImageUrl(imageUrl);
        return new ImageUploadResponse(imageUrl);
    }

    // ── 보석 ───────────────────────────────────────────────────────────────────

    /** 보석 이름 필터 검색 (페이징). */
    @Transactional(readOnly = true)
    public Page<GemImageTargetResponse> searchGems(String name, Pageable pageable) {
        String nameTrim = (name != null && !name.isBlank()) ? name.trim() : null;
        return gemRepository.findByNameFilter(nameTrim, pageable)
                .map(GemImageTargetResponse::from);
    }

    /** 이미지가 없는 보석 전체 목록. */
    @Transactional(readOnly = true)
    public List<GemImageTargetResponse> missingGemImages() {
        return gemRepository.findByImageUrlIsNull()
                .stream().map(GemImageTargetResponse::from).toList();
    }

    /** 보석 이미지를 S3에 업로드하고 imageUrl을 저장한다. */
    @Transactional
    public ImageUploadResponse uploadGemImage(Long gemId, MultipartFile file) {
        Gem gem = gemRepository.findById(gemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "보석을 찾을 수 없습니다: " + gemId));
        String imageUrl = upload(file, "images/gems/" + gemId);
        gem.updateImageUrl(imageUrl);
        return new ImageUploadResponse(imageUrl);
    }

    // ── 용병 ───────────────────────────────────────────────────────────────────

    /** 용병 이름 필터 검색 (페이징). */
    @Transactional(readOnly = true)
    public Page<MercenaryImageTargetResponse> searchMercenaries(String name, Pageable pageable) {
        String nameTrim = (name != null && !name.isBlank()) ? name.trim() : null;
        return mercenaryRepository.findByFilters(null, null, null, nameTrim, pageable)
                .map(MercenaryImageTargetResponse::from);
    }

    /** 이미지가 없는 용병 전체 목록. */
    @Transactional(readOnly = true)
    public List<MercenaryImageTargetResponse> missingMercenaryImages() {
        return mercenaryRepository.findByImageUrlIsNull()
                .stream().map(MercenaryImageTargetResponse::from).toList();
    }

    /** 용병 이미지를 S3에 업로드하고 imageUrl을 저장한다. */
    @Transactional
    public ImageUploadResponse uploadMercenaryImage(Long mercenaryId, MultipartFile file) {
        Mercenary mercenary = mercenaryRepository.findById(mercenaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "용병을 찾을 수 없습니다: " + mercenaryId));
        String imageUrl = upload(file, "images/mercenaries/" + mercenaryId);
        mercenary.updateImageUrl(imageUrl);
        return new ImageUploadResponse(imageUrl);
    }

    // ── S3 동기화 ─────────────────────────────────────────────────────────────────

    /**
     * S3 폴더 구조를 읽어 아이템·몬스터의 imageUrl을 일괄 갱신한다.
     * items/{id}.png, monsters/{id}.png 형식의 키를 기준으로 id를 추출해 업데이트한다.
     */
    @Transactional
    public ImageSyncResult syncFromS3() {
        if (s3Client == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "S3가 설정되지 않았습니다. aws.access-key, aws.secret-key, aws.s3-bucket을 확인하세요.");
        }
        int itemsUpdated = syncItems();
        int monstersUpdated = syncMonsters();
        return new ImageSyncResult(itemsUpdated, monstersUpdated);
    }

    private int syncItems() {
        List<S3Object> objects = listAll("items/");
        int count = 0;
        for (S3Object obj : objects) {
            Long id = extractId(obj.key(), "items/");
            if (id == null) continue;
            String url = buildUrl(obj.key());
            itemRepository.findById(id).ifPresent(item -> item.updateImageUrl(url));
            count++;
        }
        return count;
    }

    private int syncMonsters() {
        List<S3Object> objects = listAll("monsters/");
        int count = 0;
        for (S3Object obj : objects) {
            Long id = extractId(obj.key(), "monsters/");
            if (id == null) continue;
            String url = buildUrl(obj.key());
            monsterRepository.findById(id).ifPresent(m -> m.updateImageUrl(url));
            count++;
        }
        return count;
    }

    /** S3에서 prefix 하위 전체 오브젝트 목록을 반환한다 (페이지 자동 처리). */
    private List<S3Object> listAll(String prefix) {
        List<S3Object> result = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(bucket).prefix(prefix);
            if (token != null) builder.continuationToken(token);
            var response = s3Client.listObjectsV2(builder.build());
            result.addAll(response.contents());
            token = response.isTruncated() ? response.nextContinuationToken() : null;
        } while (token != null);
        return result;
    }

    /**
     * S3 키에서 ID를 추출한다.
     * 예: "items/123.png" → 123 / 확장자 없거나 숫자가 아니면 null 반환
     */
    private Long extractId(String key, String prefix) {
        String filename = key.substring(prefix.length()); // "123.png"
        int dot = filename.lastIndexOf('.');
        String idStr = dot > 0 ? filename.substring(0, dot) : filename;
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildUrl(String s3Key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + s3Key;
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────────

    /**
     * 파일을 S3에 업로드하고 공개 URL을 반환한다.
     * s3KeyBase에 확장자를 붙인 키로 저장한다 (예: "images/items/1.png").
     * aws.access-key 미설정 환경에서는 503을 반환한다.
     */
    private String upload(MultipartFile file, String s3KeyBase) {
        if (s3Client == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "S3가 설정되지 않았습니다. aws.access-key, aws.secret-key, aws.s3-bucket을 확인하세요.");
        }
        String ext = resolveExtension(file);
        String s3Key = s3KeyBase + "." + ext;
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 읽기 실패: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "S3 업로드 실패: " + e.getMessage());
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + s3Key;
    }

    /** 원본 파일명 → Content-Type 순서로 확장자를 결정한다. */
    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            return original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        return switch (file.getContentType() != null ? file.getContentType() : "") {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/gif"  -> "gif";
            case "image/webp" -> "webp";
            default           -> "jpg";
        };
    }
}
