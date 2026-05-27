package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("barbti_results")
public class BarbtiResult {

    @TableId
    private String userId;

    private String typeCode;
    private String subtitle;
    private String description;
    private String traitTags;
    private String scores;
    private String answers;
    private LocalDateTime completedAt;
}
