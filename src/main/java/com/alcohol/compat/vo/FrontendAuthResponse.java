package com.alcohol.compat.vo;

import lombok.Data;

@Data
public class FrontendAuthResponse {

    private FrontendUserVO user;
    private String accessToken;
    private String refreshToken;
}
