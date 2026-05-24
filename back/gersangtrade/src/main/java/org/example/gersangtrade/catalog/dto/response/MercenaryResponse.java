package org.example.gersangtrade.catalog.dto.response;

import org.example.gersangtrade.domain.catalog.Mercenary;

/**
 * 공개 용병 목록 응답 DTO — DPS 계산기 용병 선택 화면에서 사용.
 *
 * @param id              용병 ID
 * @param name            용병 이름
 * @param category        카테고리 한국어 표시명 (null이면 미분류)
 * @param element         속성 (Nature enum name, 무속성은 null)
 * @param resistPierce    저항깎 수치 (MercenaryStat에서 조회, 미입력 시 null)
 * @param elementValue    속성값 (natureValue, 무속성은 null)
 * @param imageUrl        이미지 S3 URL (크롤링 전은 null)
 * @param nation          출신 국가 (Nation enum name)
 */
public record MercenaryResponse(
        Long id,
        String name,
        String category,
        String element,
        Integer resistPierce,
        Integer elementValue,
        String imageUrl,
        String nation
) {
    public static MercenaryResponse of(Mercenary m, Integer resistPierce) {
        return new MercenaryResponse(
                m.getId(),
                m.getName(),
                m.getCategory() != null ? m.getCategory().getDisplayName() : null,
                m.getNature() != null ? m.getNature().name() : null,
                resistPierce,
                m.getNatureValue(),
                m.getImageUrl(),
                m.getNation() != null ? m.getNation().name() : null
        );
    }
}
