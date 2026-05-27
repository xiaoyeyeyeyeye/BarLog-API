package com.alcohol.compat.vo;

import lombok.Data;

import java.util.Map;

@Data
public class FrontendDiaryStatsVO {

    private Map<String, Integer> categoryCounts;
    private Map<String, Integer> moodCounts;
}
