package com.alcohol.vo.diary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "日记 Tab 月历汇总")
public class DiarySummaryVO {

    private int year;
    private int month;
    private long totalCheckIns;
    private long barsVisited;
    private Double avgRating;
    private List<Integer> loggedDays;
    private int today;
}
