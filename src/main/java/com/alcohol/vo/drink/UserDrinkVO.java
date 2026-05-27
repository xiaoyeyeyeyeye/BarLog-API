package com.alcohol.vo.drink;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDrinkVO {

    private String drinkId;
    private String drinkName;
    private String category;
    private String iconUrl;
    private Integer checkInCount;
    private LocalDateTime firstUnlockedAt;
    private LocalDateTime lastCheckInAt;
}
