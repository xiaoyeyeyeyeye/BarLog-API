package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_codes")
public class VerificationCode {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String target;
    private String channel;
    private String purpose;
    private String codeHash;
    private String countryCode;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private Integer attemptCount;
    private LocalDateTime createdAt;
}
