package org.example.gersangtrade.catalog.dto.response;

import org.example.gersangtrade.domain.catalog.Monster;
import org.example.gersangtrade.domain.catalog.enums.Element;

/**
 * 몬스터 응답 DTO.
 *
 * @param id                몬스터 ID
 * @param name              몬스터 이름 (원문)
 * @param hp                생명력 (구형 몹은 null)
 * @param hittingResistance 타격저항력(%)
 * @param magicResistance   마법저항력(%)
 * @param elementValue      속성값 수치
 * @param element           속성 종류 (null이면 데이터 없음)
 */
public record MonsterResponse(
        Long id,
        String name,
        Long hp,
        Integer hittingResistance,
        Integer magicResistance,
        Integer elementValue,
        Element element
) {
    public static MonsterResponse from(Monster monster) {
        return new MonsterResponse(
                monster.getId(),
                monster.getName(),
                monster.getHp(),
                monster.getHittingResistance(),
                monster.getMagicResistance(),
                monster.getElementValue(),
                monster.getElement()
        );
    }
}
