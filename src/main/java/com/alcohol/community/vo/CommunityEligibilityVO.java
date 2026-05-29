package com.alcohol.community.vo;

import lombok.Data;

@Data
public class CommunityEligibilityVO {

    /** 今日任意打卡后即可解锁全局社区 */
    private boolean canViewCommunity;
    /** @deprecated 与 {@link #canViewCommunity} 相同，保留兼容旧客户端 */
    private boolean canViewCityFeed;
    /** @deprecated 与 {@link #canViewCommunity} 相同，保留兼容旧客户端 */
    private boolean canViewBarFeed;
    private String todayCheckInId;
    private String todayBarId;
    private String todayCity;
}
