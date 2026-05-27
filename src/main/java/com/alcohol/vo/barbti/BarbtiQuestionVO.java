package com.alcohol.vo.barbti;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "BarBTI 题目")
public class BarbtiQuestionVO {

    private int index;
    private String category;
    private String text;
    private List<String> options;

    @Data
    @AllArgsConstructor
    @Schema(description = "题目列表包装")
    public static class QuestionList {
        private List<BarbtiQuestionVO> questions;
    }
}
