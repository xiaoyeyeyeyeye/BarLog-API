package com.alcohol.compat.vo;

import lombok.Data;

import java.util.List;

/**
 * 与 Expo mock-server / bars.helpers 对齐：附近酒吧列表包装响应。
 */
@Data
public class FrontendNearbyBarsResponseVO {

    private List<FrontendBarVO> items;
    /** google_places | mock_fallback | google_places_error */
    private String source;
    private String message;
}
