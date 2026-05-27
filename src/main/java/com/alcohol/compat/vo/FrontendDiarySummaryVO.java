package com.alcohol.compat.vo;

import lombok.Data;

@Data
public class FrontendDiarySummaryVO {

    private String month;
    private int checkInCount;
    private int barsVisited;
    private Double averageRating;
    private Integer currentStreak;
}
