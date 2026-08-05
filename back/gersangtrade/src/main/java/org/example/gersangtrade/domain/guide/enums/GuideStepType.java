package org.example.gersangtrade.domain.guide.enums;

/**
 * 가이드 스텝 유형.
 * 스텝이 화면에서 어떻게 렌더링되는지(아이콘 fallback)와
 * 매물 연결 버튼 노출 여부·문구를 결정한다.
 *
 * <ul>
 *   <li>GET_ITEM       — 장비·재료 획득. linkedItem 연결, [매물 보기] 노출</li>
 *   <li>GET_SET        — 세트 획득. linkedSet 연결, is_tradeable일 때만 [매물 보기]</li>
 *   <li>HIRE_MERCENARY — 용병 고용. linkedMercenary는 이미지·정보 표시용(매물 버튼 없음)</li>
 *   <li>SELL           — 판매. linkedItem/linkedSet 연결, [등록하기] 노출</li>
 *   <li>HUNT           — 사냥터. 카탈로그 연결 없음, iconUrl 사용</li>
 *   <li>TRAIT          — 특성 설정. 연결 없음</li>
 *   <li>JOB_CHANGE     — 전직</li>
 *   <li>CRAFT          — 제작</li>
 *   <li>QUEST          — 퀘스트</li>
 *   <li>MISC           — 기타</li>
 * </ul>
 */
public enum GuideStepType {
    GET_ITEM,
    GET_SET,
    HIRE_MERCENARY,
    SELL,
    HUNT,
    TRAIT,
    JOB_CHANGE,
    CRAFT,
    QUEST,
    MISC
}
