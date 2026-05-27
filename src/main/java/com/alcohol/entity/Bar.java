package com.alcohol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bars")
public class Bar {

    @TableId(type = IdType.INPUT)
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
    private Integer isActive;
    private LocalDateTime createdAt;

    /** Google place_id（不含 places/ 前缀） */
    private String googlePlaceId;

    /** seed | google */
    private String source;

    private LocalDateTime syncedAt;
}
