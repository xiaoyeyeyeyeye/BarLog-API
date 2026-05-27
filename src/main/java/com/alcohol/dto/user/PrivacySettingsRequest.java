package com.alcohol.dto.user;

import lombok.Data;

@Data
public class PrivacySettingsRequest {

    private Boolean showHistoryCards = true;
    private Boolean showCityMap = true;
    private Boolean showFrequentArea = false;
    private Boolean allowStrangerDm = false;
    private Boolean sameBarOnly = false;
    private Boolean sameGenderOnly = false;
    private Boolean hideExactLocation = true;
}
