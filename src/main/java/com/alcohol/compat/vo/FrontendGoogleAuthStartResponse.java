package com.alcohol.compat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrontendGoogleAuthStartResponse {
    private String authUrl;
}
