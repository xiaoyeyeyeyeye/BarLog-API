package com.alcohol.compat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FrontendCreateCheckInRequest {

    @NotBlank
    private String photoUrl;
    private String cardImageUrl;
    @NotBlank
    private String drinkName;
    @NotBlank
    private String drinkCategory;
    private String barId;
    private String barName;
    private String city;
    private String area;
    @NotNull
    private List<String> moodTags;
    private Double rating;
    private String vibeMumbling;
    @NotBlank
    private String cardStyle;
    @NotBlank
    private String visibility;
    private String socialStatus;
}
