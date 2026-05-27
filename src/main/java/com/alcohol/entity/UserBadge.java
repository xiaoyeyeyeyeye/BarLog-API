package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_badges")
public class UserBadge {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;
    private String badgeId;
    private LocalDateTime unlockedAt;
}
