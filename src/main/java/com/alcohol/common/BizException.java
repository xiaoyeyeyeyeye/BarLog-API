package com.alcohol.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;

    public BizException(String message) {
        this(message, 400);
    }

    public BizException(String message, int httpStatus) {
        this(message, httpStatus, null);
    }

    public BizException(String message, int httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
