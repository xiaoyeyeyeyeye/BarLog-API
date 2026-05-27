package com.alcohol.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int httpStatus;

    public BizException(String message) {
        this(message, 400);
    }

    public BizException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
