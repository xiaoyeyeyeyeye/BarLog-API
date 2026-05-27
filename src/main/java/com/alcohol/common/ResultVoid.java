package com.alcohol.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ResultVoid", description = "无业务数据的统一响应")
public class ResultVoid {

    @Schema(description = "状态码：1=成功，0=失败", example = "0")
    private Integer code;

    @Schema(description = "错误提示", example = "未登录或令牌已失效")
    private String msg;

    @Schema(description = "失败时为 null")
    private Object result;

    public static ResultVoid success() {
        ResultVoid r = new ResultVoid();
        r.setCode(1);
        r.setMsg("操作成功");
        return r;
    }
}
