package org.example.gersangtrade.catalog.dto.response;

/**
 * 홈 사천왕 선택 UI용 응답 DTO.
 * 속성(Nature)별로 일반/각성 사천왕과 전신 스탠딩 이미지 URL을 묶어 반환한다.
 *
 * <p>스탠딩 이미지는 S3의 {@code mercenary-standing/{id}.png} 규칙으로 서버가 URL을 조합한다.
 * (증명사진용 {@code imageUrl}과는 별개의 폴더·이미지)
 *
 * @param element   속성 enum name (FIRE | WIND | THUNDER | WATER)
 * @param elementKo 속성 한글 표시 (화 | 풍 | 뇌 | 수)
 * @param normal    일반 사천왕 (없으면 null)
 * @param awakened  각성 사천왕 (없으면 null)
 */
public record HeavenlyKingResponse(
        String element,
        String elementKo,
        Variant normal,
        Variant awakened
) {
    /**
     * 사천왕 단일 항목.
     *
     * @param id                용병 ID (스탠딩 이미지 파일명)
     * @param name              용병 이름
     * @param standingImageUrl  전신 스탠딩 이미지 URL (S3 미설정 시 null)
     */
    public record Variant(Long id, String name, String standingImageUrl) {}
}
