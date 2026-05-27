package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_in_reactions")
public class CheckInReaction {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String checkInId;
    private String userId;
    private LocalDateTime createdAt;
}
