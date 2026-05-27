package com.alcohol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageVO<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总条数", example = "100")
    private long total;

    @Schema(description = "当前页码", example = "1")
    private int page;

    @Schema(description = "每页条数", example = "20")
    private int size;
}
