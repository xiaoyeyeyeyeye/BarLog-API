package com.alcohol.vo.bar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "酒吧 POI")
public class BarVO {

    private String id;
    private String name;
    private String typeLabel;
    private String city;
    private String area;
    private String address;
    private Double latitude;
    private Double longitude;
    private String openHours;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private String coverUrl;
    private Integer distanceM;
    private Boolean favorited;
}
