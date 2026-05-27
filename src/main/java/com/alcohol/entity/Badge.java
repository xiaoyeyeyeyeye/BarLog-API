package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("badges")
public class Badge {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private LocalDateTime createdAt;
}
