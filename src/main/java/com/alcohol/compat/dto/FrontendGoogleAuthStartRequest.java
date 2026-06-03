package com.alcohol.compat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FrontendGoogleAuthStartRequest {

    @NotBlank
    private String redirectUri;

    @NotBlank
    @Pattern(regexp = "login|register")
    private String mode;
}
