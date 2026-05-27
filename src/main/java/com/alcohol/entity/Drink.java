package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("drinks")
public class Drink {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;
    private String category;
    private String flavorTags;
    private String description;
    private String iconUrl;
    private Integer isClassic;
    private LocalDateTime createdAt;
}
