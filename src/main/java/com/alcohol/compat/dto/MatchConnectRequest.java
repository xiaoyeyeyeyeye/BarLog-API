package com.alcohol.compat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MatchConnectRequest {

    @NotBlank
    private String userId;
}
