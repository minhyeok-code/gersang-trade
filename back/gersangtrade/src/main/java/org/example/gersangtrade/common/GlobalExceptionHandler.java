package org.example.gersangtrade.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러.
 * 서비스 계층에서 던진 예외를 적절한 HTTP 상태 코드와 JSON 응답으로 변환한다.
 *
 * <pre>
 * IllegalArgumentException  → 400 Bad Request  (잘못된 입력값, 리소스 없음, 정책 위반)
 * IllegalStateException     → 409 Conflict     (상태 충돌 — 이미 취소/완료된 등록글 등)
 * MethodArgumentNotValidException → 400        (@Valid 검증 실패 — 필드별 메시지 포함)
 * </pre>
 *
 * 참고: 현재 IllegalArgumentException은 "리소스 없음(404)"과 "잘못된 입력(400)"을 구분하지 않는다.
 * 추후 도메인 전용 예외(NotFoundException 등)로 분리하면 더 정확한 코드 반환이 가능하다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 리소스 미존재 — 404 Not Found.
     * userRepository.findById(...).orElseThrow(NoSuchElementException) 패턴에서 발생.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "message", e.getMessage()));
    }

    /**
     * 잘못된 입력값 또는 정책 위반 — 400 Bad Request.
     * 리소스 미존재(404)와 구분이 필요하면 NotFoundException 도입 필요.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", "bad_request", "message", e.getMessage()));
    }

    /**
     * 상태 충돌 — 409 Conflict.
     * 이미 완료/취소된 등록글 조작, 차단된 계정의 등록 시도 등.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", e.getMessage()));
    }

    /**
     * DB 제약 위반 — 409 Conflict.
     * 채팅방 상태 전환·중복 확정 등 데이터 정합성 충돌 시 반환한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause().getMessage();
        log.warn("DataIntegrityViolation: {}", cause, e);
        String message = "데이터 저장 중 충돌이 발생했습니다. 잠시 후 다시 시도해 주세요.";
        if (cause != null && cause.contains("reveal_at")) {
            message = "거래 평가 일정 저장에 실패했습니다. 백엔드를 재시작한 뒤 다시 시도해 주세요.";
        }
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", message));
    }

    /**
     * @Valid 검증 실패 — 400 Bad Request.
     * 필드별 오류 메시지를 포함해 반환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        // 같은 필드에 여러 오류가 있으면 첫 번째 메시지만 사용
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값입니다.",
                        (first, second) -> first
                ));
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", "validation_failed", "fieldErrors", fieldErrors));
    }
}
