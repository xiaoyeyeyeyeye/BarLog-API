package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_recommend_logs")
public class AiRecommendLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;
    private String mood;
    private Integer spotifyOn;
    private String resultType;
    private String resultJson;
    private LocalDateTime createdAt;
}
