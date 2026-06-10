package org.example.gersangtrade.watchlist.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WatchlistException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Integer current;
    private final Integer max;

    public WatchlistException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.current = null;
        this.max = null;
    }

    public WatchlistException(String errorCode, HttpStatus httpStatus, String message, int current, int max) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.current = current;
        this.max = max;
    }

    public static WatchlistException duplicate() {
        return new WatchlistException("DUPLICATE_WATCH_ITEM", HttpStatus.CONFLICT, "이미 등록된 관심 매물입니다.");
    }

    public static WatchlistException limitExceeded(int current, int max) {
        return new WatchlistException("WATCH_LIMIT_EXCEEDED", HttpStatus.UNPROCESSABLE_ENTITY,
                "관심목록은 최대 " + max + "개까지 등록할 수 있습니다.", current, max);
    }

    public static WatchlistException invalidTarget(String reason) {
        return new WatchlistException("INVALID_WATCH_TARGET", HttpStatus.BAD_REQUEST, reason);
    }

    public static WatchlistException notFound() {
        return new WatchlistException("WATCH_TARGET_NOT_FOUND", HttpStatus.NOT_FOUND, "관심목록 항목을 찾을 수 없습니다.");
    }

    public static WatchlistException forbidden() {
        return new WatchlistException("WATCH_FORBIDDEN", HttpStatus.FORBIDDEN, "본인의 관심목록만 삭제할 수 있습니다.");
    }
}
