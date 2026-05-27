package com.alcohol.community.vo;

import lombok.Data;

@Data
public class CommunityEligibilityVO {

    private boolean canViewCityFeed;
    private boolean canViewBarFeed;
    private String todayCheckInId;
    private String todayBarId;
    private String todayCity;
}
