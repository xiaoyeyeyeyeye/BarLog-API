package com.alcohol.compat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FrontendGoogleAuthCompleteRequest {

    @NotBlank
    private String accessToken;

    private String refreshToken;
}
