package org.example.gersangtrade.calculator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.calculator.dto.request.CalculatorRequest;
import org.example.gersangtrade.calculator.dto.request.DpsRequest;
import org.example.gersangtrade.calculator.dto.response.CalculatorResponse;
import org.example.gersangtrade.calculator.dto.response.DpsResponse;
import org.example.gersangtrade.calculator.service.CalculatorService;
import org.example.gersangtrade.calculator.service.DpsCalculatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계산기 API 컨트롤러.
 *
 * <pre>
 * POST /api/calculator     — 가성비 계산 (수동 스펙 입력)
 * POST /api/calculator/dps — DPS 계산 (덱 기반)
 * </pre>
 *
 * <p>비로그인(Guest) 허용 기능이다.
 * 계산 결과는 서버에 저장하지 않는다 (세션 내 일회성 계산).
 */
@RestController
@RequestMapping("/api/calculator")
@RequiredArgsConstructor
public class CalculatorController {

    private final CalculatorService calculatorService;
    private final DpsCalculatorService dpsCalculatorService;

    /**
     * 가성비 계산 실행.
     * 유저가 현재 스펙과 목표 몬스터를 입력하면 저항깎·속성값 가성비 리스트를 반환한다.
     */
    @PostMapping
    public ResponseEntity<CalculatorResponse> calculate(
            @RequestBody @Valid CalculatorRequest request) {
        return ResponseEntity.ok(calculatorService.calculate(request));
    }

    /**
     * DPS 계산 실행.
     * 저장된 덱과 몬스터를 선택하면 멤버별 DPS와 전체 합산 DPS를 반환한다.
     */
    @PostMapping("/dps")
    public ResponseEntity<DpsResponse> calculateDps(
            @RequestBody @Valid DpsRequest request) {
        return ResponseEntity.ok(dpsCalculatorService.calculate(request));
    }
}
