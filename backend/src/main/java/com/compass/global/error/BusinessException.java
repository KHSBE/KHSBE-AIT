package com.compass.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 비즈니스 예외 처리
@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus httpStatus;

    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}