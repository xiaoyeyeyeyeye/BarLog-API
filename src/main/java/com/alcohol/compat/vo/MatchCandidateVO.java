package com.alcohol.compat.vo;

import lombok.Data;

@Data
public class MatchCandidateVO {

    private String id;
    private String displayName;
    private String avatarUrl;
    private String reason;
    private Integer distanceMeters;
    private boolean hasTodayCheckIn;
}
