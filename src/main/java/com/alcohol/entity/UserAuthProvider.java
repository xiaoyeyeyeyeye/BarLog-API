package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_auth_providers")
public class UserAuthProvider {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;
    private String provider;
    private String providerUserId;
    private String providerEmail;
    private String displayName;
    private String avatarUrl;
    private String rawProfile;
    private LocalDateTime linkedAt;
    private LocalDateTime updatedAt;
}
