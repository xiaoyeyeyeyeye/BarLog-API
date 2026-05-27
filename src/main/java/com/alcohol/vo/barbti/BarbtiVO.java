package com.alcohol.vo.barbti;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "BarBTI 结果")
public class BarbtiVO {

    private String typeCode;
    private String subtitle;
    private String description;
    private List<String> traitTags;
    private Map<String, Integer> scores;
    private LocalDateTime completedAt;
}
