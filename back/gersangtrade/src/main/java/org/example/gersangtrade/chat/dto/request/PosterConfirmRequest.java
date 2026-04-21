package org.example.gersangtrade.chat.dto.request;

import jakarta.validation.constraints.Min;

/**
 * 게시자 거래완료 확인 요청 DTO.
 * finalPrice는 선택 입력으로, 미입력 시 게시물 원래 가격을 사용한다.
 */
public record PosterConfirmRequest(

        /**
         * 실제 거래가 (선택).
         * null이면 게시물 등록 당시 가격을 사용한다.
         */
        @Min(value = 1, message = "거래가는 1 이상이어야 합니다.")
        Long finalPrice
) {}
