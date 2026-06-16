package com.alcohol.compat.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FrontendUpdateProfileRequest {

    @Size(min = 2, max = 40)
    private String displayName;

    @Size(max = 2048)
    private String avatarUrl;
}
