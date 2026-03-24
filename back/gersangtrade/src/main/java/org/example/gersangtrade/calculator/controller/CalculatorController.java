package org.example.gersangtrade.calculator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.CalculatorRequest;
import org.example.gersangtrade.calculator.dto.response.CalculatorResponse;
import org.example.gersangtrade.calculator.service.CalculatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가성비 계산기 API 컨트롤러.
 *
 * <pre>
 * POST /api/calculator — 현재 스펙 입력 → 데미지 현황 + 저항깎/속성값 가성비 리스트 반환
 * </pre>
 *
 * <p>비로그인(Guest) 허용 기능이다. SecurityConfig에서 POST /api/calculator permitAll 적용됨.
 *
 * <p>계산 결과는 서버에 저장하지 않는다 (세션 내 일회성 계산).
 * 가격 수정값(priceOverrides)도 DB에 반영하지 않으며 요청 범위에서만 유효하다.
 */
@RestController
@RequestMapping("/api/calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService calculatorService;

    /**
     * 가성비 계산 실행.
     *
     * <p>유저가 현재 스펙(저항깎, 속성값)과 목표 몬스터 정보를 입력하면
     * 현재 데미지 현황과 추가 시 가성비가 좋은 아이템/용병 두 목록을 반환한다.
     *
     * @param request 유저 스펙·몬스터 정보·가격 수정값
     * @return 데미지 현황 + 저항깎 리스트 + 속성값 리스트
     */
    @PostMapping
    public ResponseEntity<CalculatorResponse> calculate(
            @RequestBody @Valid CalculatorRequest request) {
        return ResponseEntity.ok(calculatorService.calculate(request));
    }
}
