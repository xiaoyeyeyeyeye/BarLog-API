package com.alcohol.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "统一响应")
public class Result<T> implements Serializable {

    @Schema(description = "1 成功，0 失败")
    private Integer code;

    private String msg;

    private T result;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(1);
        r.setMsg("操作成功");
        r.setResult(data);
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(1);
        r.setMsg(message);
        r.setResult(data);
        return r;
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg(message);
        return r;
    }
}
