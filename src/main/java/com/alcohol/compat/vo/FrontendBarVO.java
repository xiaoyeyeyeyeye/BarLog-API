package com.alcohol.compat.vo;

import lombok.Data;

import java.util.List;

@Data
public class FrontendBarVO {

    private String id;
    private String name;
    private String city;
    private String area;
    private String address;
    private Double rating;
    private Integer distanceMeters;
    private Double lat;
    private Double lng;
    private List<String> tags;
    private String description;
    private String openingHours;
    private Integer checkInCount;
    /** Google 评价数量；详情接口返回 */
    private Integer reviewCount;
    /** 官网；详情接口返回（Google 来源） */
    private String websiteUrl;
    /** Google Maps 链接；详情接口返回，前端后续可用来跳转或展示归属 */
    private String googleMapsUrl;
    /** 数据来源：google | seed（便于前端调试，可选展示） */
    private String source;
}
