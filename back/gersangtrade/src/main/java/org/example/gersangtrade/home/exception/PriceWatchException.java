package org.example.gersangtrade.home.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PriceWatchException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public PriceWatchException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
