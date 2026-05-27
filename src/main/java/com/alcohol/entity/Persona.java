package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("personas")
public class Persona {

    @TableId
    private String userId;

    private String mainDrinkType;
    private String secondaryDrinkType;
    private String flavorProfile;
    private String nightKeywords;
    private String socialTendency;
    private String generatedText;
    private LocalDateTime updatedAt;
}
