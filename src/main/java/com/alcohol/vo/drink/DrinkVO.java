package com.alcohol.vo.drink;

import lombok.Data;

import java.util.List;

@Data
public class DrinkVO {

    private String id;
    private String name;
    private String category;
    private List<String> flavorTags;
    private String description;
    private String iconUrl;
    private Boolean classic;
    private Long todayCheckInCount;
    private String topMood;
}
