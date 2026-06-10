package org.example.gersangtrade.watchlist.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * watchlist 전용 예외 핸들러.
 * 기존 GlobalExceptionHandler 수정 없이 errorCode·422 응답을 처리한다.
 * 기존 프론트 호환을 위해 error 필드도 함께 포함한다.
 */
@RestControllerAdvice
public class WatchlistExceptionHandler {

    @ExceptionHandler(WatchlistException.class)
    public ResponseEntity<Map<String, Object>> handle(WatchlistException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", ex.getErrorCode());
        body.put("error", ex.getHttpStatus().getReasonPhrase().toLowerCase().replace(" ", "_"));
        body.put("message", ex.getMessage());
        if (ex.getCurrent() != null) {
            body.put("current", ex.getCurrent());
            body.put("max", ex.getMax());
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }
}
