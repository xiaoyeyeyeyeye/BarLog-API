package com.alcohol.vo.badge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BadgeVO {

    private String id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private Boolean unlocked;
    private LocalDateTime unlockedAt;
}
