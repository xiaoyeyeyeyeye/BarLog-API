package com.alcohol.dto.barbti;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "BarBTI 提交答案")
public class BarbtiSubmitRequest {

    @NotNull
    @Size(min = 20, max = 20, message = "需提交 20 道题答案")
    private List<Integer> answers;
}
