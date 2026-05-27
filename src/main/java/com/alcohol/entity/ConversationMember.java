package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_members")
public class ConversationMember {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String conversationId;
    private String userId;
    private Integer unreadCount;
    private LocalDateTime lastReadAt;
    private LocalDateTime joinedAt;
}
