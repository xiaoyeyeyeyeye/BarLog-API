package com.alcohol.compat;

import com.alcohol.common.BizException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 前端兼容 API 异常格式：{@code { message, code }}，不使用 {@code Result} 包装。
 */
@Order(0)
@RestControllerAdvice(basePackages = "com.alcohol.compat")
public class FrontendExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<FrontendErrorBody> handleBiz(BizException e) {
        String code = e.getHttpStatus() == 401 ? "AUTH_INVALID" : "BIZ_ERROR";
        return ResponseEntity.status(e.getHttpStatus())
                .body(new FrontendErrorBody(e.getMessage(), code));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FrontendErrorBody> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new FrontendErrorBody(msg, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FrontendErrorBody> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FrontendErrorBody(
                        e.getMessage() != null ? e.getMessage() : "Server error",
                        "INTERNAL_ERROR"));
    }
}
