package org.example.gersangtrade.catalog.dto.response;

import org.example.gersangtrade.domain.catalog.Monster;

/**
 * 몬스터 자동완성 응답 DTO — id와 이름만 반환한다.
 */
public record MonsterAutocompleteResponse(Long id, String name) {

    public static MonsterAutocompleteResponse from(Monster monster) {
        return new MonsterAutocompleteResponse(monster.getId(), monster.getName());
    }
}
