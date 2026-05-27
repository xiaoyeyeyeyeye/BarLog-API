package com.alcohol.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户统计")
public class UserStatsVO {

    private long checkInCount;
    private long barsVisited;
    private long cardCount;
    private Double avgRating;
}
