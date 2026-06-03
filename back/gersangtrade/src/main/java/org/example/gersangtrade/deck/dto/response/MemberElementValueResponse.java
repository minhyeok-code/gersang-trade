package org.example.gersangtrade.deck.dto.response;

/** 덱 멤버 속성값 — DPS 없이 카드에 표시하기 위한 경량 응답. */
public record MemberElementValueResponse(Long memberId, int elementValue) {}
